package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.ble.BleClient
import cn.floriax.amber.core.ble.BleConnection
import cn.floriax.amber.core.ble.BleException
import cn.floriax.amber.core.ble.isLinkFailure
import cn.floriax.amber.core.ble.protocol.QueryFrame
import cn.floriax.amber.core.ble.protocol.TimeFrame
import cn.floriax.amber.core.common.suspendRunCatching
import cn.floriax.amber.data.device.logger.FrameLogAggregator
import cn.floriax.amber.data.device.mapper.toLedFrame
import cn.floriax.amber.data.device.mapper.toSwitchFrame
import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.exception.ClockException
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clock repository implementation: the device is the source of truth.
 * Contract (see ClockRepository): write gating (not connected → NotConnected),
 * execution exceptions translated to Failed via suspendRunCatching, all
 * methods main-safe, connect has "initiated" semantics.
 * The constructor takes lambdas (now/autoSync) so it cannot be @Inject —
 * built by the @Provides factory in data/di; tests instantiate it directly
 * with named arguments.
 *
 * ## Concurrency model
 *
 * Mutable state (generation/session/reconnectJob/wanted/userInitiatedClose/
 * currentMac) is touched on a single thread only: production injects a
 * Main.immediate scope (see the DI module), tests inject
 * TestScope.backgroundScope — no locks, no volatile. Note that
 * single-threaded interleaving of coroutines at suspension points still
 * exists and is resolved by connection generations:
 *
 * - all resources of one connection attempt are bundled into
 *   [ConnectionSession]; installing a new session closes the old one first;
 * - [generation] increments with every new connection intent (a non-duplicate
 *   connect call, disconnect). Each attempt compares the generation after
 *   every suspension and exits in place when superseded — this cures both
 *   "an old attempt steals back the state machine during B's handshake
 *   window" and "an in-flight attempt revives the connection after
 *   disconnect".
 *
 * All writes to [DeviceState] go through [DeviceStateProjector] (the single
 * write entry point).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class ClockRepositoryImpl(
    private val ble: BleClient,
    private val scope: CoroutineScope,
    private val logger: FrameLogAggregator,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
    private val autoSync: () -> Boolean = { true },
    /** Reconnect backoff sleep, injectable for tests. */
    private val sleeper: suspend (Long) -> Unit = { delay(it.milliseconds) },
) : ClockRepository {

    override val frameLogs: StateFlow<List<FrameLog>> = logger.logs

    override val deviceState: StateFlow<DeviceState> get() = projector.deviceState

    /** Current [DeviceState] snapshot; every read sees the latest value. */
    private val currentDeviceState: DeviceState
        get() = projector.deviceState.value

    /** Single write entry of DeviceState: report projection + orchestration state + optimistic updates + reserved bytes/handshake bits. */
    private val projector = DeviceStateProjector(logger)

    // ---- Session and generation (touched on the single injected-scope thread only) ----

    private var generation = 0L
    private var session: ConnectionSession? = null
    private var reconnectJob: Job? = null
    private var wanted = false
    private var userInitiatedClose = false
    private var currentMac: String? = null

    init {
        // Adapter off/on is a system-level event outside the GATT link:
        // off stops every retry (reconnecting without a radio would spin
        // forever and show a misleading "reconnecting" state), on resumes
        // the connection when the user had not actively disconnected.
        scope.launch {
            ble.bluetoothState.collect { enabled ->
                if (enabled) onBluetoothOn() else onBluetoothOff()
            }
        }
    }

    /** Adapter off: drop to DISCONNECTED, stop all retries, keep the intent ([wanted]). */
    private fun onBluetoothOff() {
        generation++
        reconnectJob?.cancel()
        reconnectJob = null
        abandon(session)
        projector.update { it.copy(connection = ConnectionState.DISCONNECTED) }
    }

    /**
     * Adapter back on: reconnect the remembered device when the user had
     * not actively disconnected (the intent survived [onBluetoothOff]).
     */
    private fun onBluetoothOn() {
        val mac = currentMac ?: return
        if (!wanted || userInitiatedClose) return
        if (currentDeviceState.connection != ConnectionState.DISCONNECTED) return
        val gen = ++generation
        projector.update {
            it.copy(connection = ConnectionState.CONNECTING, deviceMac = mac)
        }
        scope.launch {
            if (!tryConnect(mac, gen) && gen == generation) scheduleReconnect(mac, gen)
        }
    }

    private fun isCurrent(s: ConnectionSession) = session === s

    /** Closes the session; yields it too if it is still current. All operations idempotent. */
    private fun abandon(s: ConnectionSession?) {
        if (s == null) return
        if (session === s) session = null
        s.close()
    }

    // ---- Connection orchestration ----

    override suspend fun connect(device: ClockDevice): Result<Unit> {
        // A connection to the same device is already in flight/established: do not
        // re-initiate — concurrent connects would leak the previous GATT on the device.
        if (device.mac == currentMac &&
            currentDeviceState.connection in ONGOING_STATES
        ) {
            return Result.success(Unit)
        }
        val gen = ++generation
        wanted = true
        userInitiatedClose = false
        currentMac = device.mac
        reconnectJob?.cancel()   // a new connection intent supersedes the running backoff sequence
        projector.update {
            it.copy(
                connection = ConnectionState.CONNECTING,
                deviceName = device.alias,
                deviceMac = device.mac,
            )
        }
        // "Initiated" semantics: a handshake failure is not reported upward as a
        // failure — it falls back to background reconnection (see the contract).
        // If superseded by a new intent/disconnect during the attempt (gen changed),
        // do not reconnect for this intent; the new one owns the state machine.
        if (!tryConnect(device.mac, gen) && gen == generation) scheduleReconnect(device.mac, gen)
        return Result.success(Unit)
    }

    /**
     * One connection attempt: connect → subscribe → send the query frame →
     * wait for the LED/switch/time frames to arrive (handshake). Stays
     * CONNECTING until the handshake completes (writes are gated off); only
     * receiving all three frames sets CONNECTED.
     *
     * Returns **whether the handshake succeeded** (not the same as "connection
     * usable": the link may drop right after the handshake — usability is
     * defined by [deviceState]). [gen] is the connection generation of this
     * attempt: whenever it turns out superseded after a suspension, close the
     * connection in place, return false and leave the state machine alone.
     * Failures release the session, log and return false — nothing propagates;
     * cancellation releases the session and rethrows.
     */
    private suspend fun tryConnect(mac: String, gen: Long): Boolean {
        var s: ConnectionSession? = null
        try {
            val conn = ble.connect(mac)
            if (gen != generation) {
                // Superseded while waiting for the connection: close in place,
                // leave the state machine alone (the caller sees gen changed and
                // will not reconnect either).
                conn.close()
                return false
            }
            projector.resetHandshake()
            s = installSession(gen, conn)
            conn.write(QueryFrame.BYTES)
            logger.tx(QueryFrame.BYTES)
            val ready = withTimeoutOrNull(HANDSHAKE_TIMEOUT) {
                projector.handshakeBits.first { it == HANDSHAKE_DONE_BITS }
                true
            }
            if (ready != true) throw BleException("Query handshake timed out")
            if (gen != generation) {
                // Superseded during the handshake (the wait bits are global and may
                // have been woken by the new session's report frames).
                abandon(s)
                return false
            }
            projector.update { it.copy(connection = ConnectionState.CONNECTED) }
            if (autoSync()) syncTime()
            return true
        } catch (e: Exception) {
            logger.sys("Connect failed: ${e.message}")
            abandon(s)
            if (e is CancellationException) throw e   // cancellation is not a failed attempt
            return false
        }
    }

    /**
     * Installs the session as current: close the old session first (the old
     * GATT must not be left on the device side unowned), then attach the two
     * collection jobs (notifications / disconnect). Frames and disconnect
     * signals carry a session identity guard — late callbacks of a superseded
     * session never pollute the current state machine.
     * Note: this does **not** set CONNECTED — that is decided by the handshake
     * in [tryConnect].
     */
    private fun installSession(gen: Long, conn: BleConnection): ConnectionSession {
        val s = ConnectionSession(gen, conn)
        abandon(session)
        session = s
        s.collectJob = scope.launch {
            conn.notifications.collect { if (isCurrent(s)) onFrame(it) }
        }
        s.disconnectJob = scope.launch {
            conn.disconnected.collect { onLinkLost(s) }
        }
        return s
    }

    /**
     * Exponential-backoff reconnection (1s/2s/4s, at most 3 fast attempts)
     * plus low-frequency in-session retries. On link loss the state goes
     * RECONNECTING immediately; when the fast backoff is exhausted it does
     * **not** give up: the state falls back to DISCONNECTED (manual retry
     * entry available) and a slow 30s retry continues in the background until
     * the user explicitly disconnects or a new connection intent supersedes.
     * [gen] is the generation of this sequence: silently exit when superseded.
     */
    private fun scheduleReconnect(mac: String, gen: Long) {
        if (!wanted || userInitiatedClose) return
        // Radio off: retrying cannot succeed — fall back to DISCONNECTED
        // (the adapter-state listener resumes the connection once on).
        if (!ble.isBluetoothEnabled) {
            projector.update { it.copy(connection = ConnectionState.DISCONNECTED) }
            return
        }
        // The link is gone — leave CONNECTED/CONNECTING first; this must not be
        // skipped due to "a sequence is already running".
        projector.update { it.copy(connection = ConnectionState.RECONNECTING) }
        if (reconnectJob?.isActive == true) return   // already running: don't restart the backoff
        reconnectJob = scope.launch { runReconnectBackoff(mac, gen) }
    }

    /**
     * The reconnect backoff sequence: the fast backoff (1s/2s/4s, at most 3
     * attempts), then — never giving up — the slow 30s retry loop. Returns
     * when reconnected or superseded ([gen] no longer current / user
     * disconnected).
     */
    private suspend fun runReconnectBackoff(mac: String, gen: Long) {
        for (backoffMs in RECONNECT_BACKOFF_MS) {
            if (!backoffElapsed(gen, backoffMs)) return
            if (reconnectSucceeded(mac, gen)) return
            // This attempt failed: stay RECONNECTING, continue the backoff.
            projector.update { it.copy(connection = ConnectionState.RECONNECTING) }
        }
        if (gen != generation) return
        // Fast backoff exhausted: fall back to disconnected (manual retry
        // available), slow retries continue in the background.
        projector.update { it.copy(connection = ConnectionState.DISCONNECTED) }
        logger.sys("Reconnect exhausted, retrying every ${RECONNECT_SLOW_MS / 1_000}s")
        while (true) {
            if (!backoffElapsed(gen, RECONNECT_SLOW_MS)) return
            projector.update { it.copy(connection = ConnectionState.RECONNECTING) }
            if (reconnectSucceeded(mac, gen)) return
            projector.update { it.copy(connection = ConnectionState.DISCONNECTED) }
        }
    }

    /**
     * Waits out one reconnect backoff interval; false when the sequence was
     * superseded (generation/user intent changed) before or during the wait.
     */
    private suspend fun backoffElapsed(gen: Long, backoffMs: Long): Boolean {
        if (!stillWanted(gen)) return false
        sleeper(backoffMs)
        return stillWanted(gen)
    }

    /**
     * One reconnect attempt. Reconnection counts only when the handshake
     * succeeded AND the state really is CONNECTED — if the link drops right
     * after the handshake (e.g. the auto-sync write fails) the caller stays
     * in its backoff instead of ending the sequence.
     */
    private suspend fun reconnectSucceeded(mac: String, gen: Long): Boolean =
        tryConnect(mac, gen) && currentDeviceState.connection == ConnectionState.CONNECTED

    /** Whether the reconnect sequence is still valid: generation unchanged, still wanted, no user disconnect. */
    private fun stillWanted(gen: Long): Boolean =
        gen == generation && wanted && !userInitiatedClose

    override fun disconnect() {
        wanted = false
        userInitiatedClose = true
        // Invalidate in-flight connection attempts: disconnect cannot cancel the
        // tryConnect suspended in the caller's coroutine — it exits in place by
        // generation after resuming; otherwise it would "revive" the connection.
        generation++
        reconnectJob?.cancel()
        abandon(session)
        projector.update {
            it.copy(
                connection = ConnectionState.DISCONNECTED,
                deviceName = "",
                deviceMac = null,
            )
        }
    }

    /**
     * Link lost (unexpected disconnect / link-level write failure): close the
     * session, enter backoff reconnection when needed.
     * [lost] is the session that raised the signal; ignored if it is no longer
     * current (a late signal of an old connection). Idempotent: an ongoing
     * reconnection does not restart the backoff; after a user disconnect only
     * DISCONNECTED is set.
     */
    private fun onLinkLost(lost: ConnectionSession?) {
        if (lost == null || !isCurrent(lost)) return
        abandon(lost)
        val mac = currentMac
        if (userInitiatedClose || !wanted || mac == null) {
            projector.update { it.copy(connection = ConnectionState.DISCONNECTED) }
            return
        }
        logger.sys("Link lost, reconnecting")
        scheduleReconnect(mac, lost.generation)
    }

    // ---- Frame parsing → state ----

    /**
     * Test seam: inject report frames directly (multi-element delivery over
     * the notify pipeline is unreliable under coroutines-test). The
     * production path enters via the session's notification collection job
     * with a session identity guard.
     */
    internal fun onFrame(bytes: ByteArray) = projector.onFrame(bytes)

    // ---- Write operations (gating + optimistic update + reserved-byte echo) ----

    override suspend fun sendBacklight(config: Backlight): Result<Unit> = writeFrame(
        frame = config.toLedFrame().copy(reserved = projector.ledReserved).encode(),
        optimistic = { projector.update { it.copy(backlight = config) } },
    )

    override suspend fun sendTimers(timers: TimeConfig): Result<Unit> = writeTimeFrame(
        frame = TimeFrame.now(now(), timers.powerOff, timers.powerOn, timers.alarm),
        optimistic = { projector.update { it.copy(timers = timers) } },
    )

    override suspend fun sendSwitches(config: SwitchConfig): Result<Unit> = writeFrame(
        frame = config.toSwitchFrame(projector.switchByte4).encode(),
        optimistic = { projector.update { it.copy(switches = config) } },
    )

    override suspend fun syncTime(): Result<Unit> {
        val s = currentDeviceState
        return writeTimeFrame(
            TimeFrame.now(now(), s.timers.powerOff, s.timers.powerOn, s.timers.alarm),
        )
    }

    /**
     * Debug-panel raw frame: goes through the normal write channel and does
     * **not** refresh [DeviceState.lastSyncAt] — "last sync" records the
     * app's sync actions (automatic / manual / changing a timer); injecting a
     * raw frame does not count.
     */
    override suspend fun sendRaw(bytes: ByteArray): Result<Unit> = writeFrame(bytes)

    /**
     * Time-frame write channel. Protocol-wise the time frame is the sync
     * frame: a successful write refreshes [DeviceState.lastSyncAt] so that
     * automatic sync, manual sync and timer-change sync share one record path.
     */
    private suspend fun writeTimeFrame(
        frame: TimeFrame,
        optimistic: (() -> Unit)? = null,
    ): Result<Unit> = writeFrame(frame.encode(), optimistic).onSuccess {
        projector.update { it.copy(lastSyncAt = System.currentTimeMillis()) }
    }

    /**
     * Unified frame-write channel: NotConnected immediately when not
     * CONNECTED; execution exceptions are captured via suspendRunCatching and
     * translated to Failed (CancellationException still rethrows).
     * Link-level write failures additionally trigger disconnect + backoff
     * reconnection — handled by the **session identity at write time**: if the
     * connection was replaced by a new intent during the write, only this
     * session's failure signal is ignored; the new connection is not torn down.
     */
    private suspend fun writeFrame(
        frame: ByteArray,
        optimistic: (() -> Unit)? = null,
    ): Result<Unit> {
        if (currentDeviceState.connection != ConnectionState.CONNECTED) {
            return Result.failure(ClockException.NotConnected())
        }
        var target: ConnectionSession? = null
        return suspendRunCatching {
            val s = session ?: throw BleException("Connection lost")
            target = s
            optimistic?.invoke()
            s.conn.write(frame)
            logger.tx(frame)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                if (e.isLinkFailure()) {
                    logger.sys("Write failed, link lost: ${e.message}")
                    // onLinkLost verifies the session identity internally:
                    // ignored if already replaced.
                    onLinkLost(target)
                }
                Result.failure(ClockException.Failed(e))
            },
        )
    }

    private companion object {
        /** States counted as "connection in flight/established": duplicate connects to the same device are ignored. */
        val ONGOING_STATES = setOf(ConnectionState.CONNECTING, ConnectionState.CONNECTED)

        /** Handshake timeout: "query frame → all three frames"; a timeout fails this attempt. */
        val HANDSHAKE_TIMEOUT = 5_000.milliseconds

        /** All-three-frames bit mask: bit0=LED bit1=switch bit2=time (see DeviceStateProjector). */
        const val HANDSHAKE_DONE_BITS = 0b111

        /** Reconnect backoff sequence: 1s/2s/4s, at most 3 attempts. */
        val RECONNECT_BACKOFF_MS = longArrayOf(1_000, 2_000, 4_000)

        /** Slow retry interval after the fast backoff is exhausted. */
        const val RECONNECT_SLOW_MS = 30_000L
    }
}
