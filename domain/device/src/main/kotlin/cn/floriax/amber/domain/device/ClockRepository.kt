package cn.floriax.amber.domain.device

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.DeviceState
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.flow.StateFlow

/**
 * Clock repository: device state stream, connection orchestration and all
 * device write operations.
 *
 * Contract (every implementation must obey):
 * - **Never throws business exceptions**: failures are reported as
 *   `Result.failure(ClockException)` — not connected is [cn.floriax.amber.domain.device.exception.ClockException.NotConnected],
 *   mid-operation failure is [cn.floriax.amber.domain.device.exception.ClockException.Failed] (carrying the original cause).
 * - **main-safe**: all suspend methods may be called on the main thread;
 *   no blocking work inside.
 * - **connect is "initiated" semantics**: a successful return only means the
 *   connection flow has started (including falling back to background
 *   reconnection); handshake results and later states are always reported
 *   via [deviceState].
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface ClockRepository {
    val deviceState: StateFlow<DeviceState>
    val frameLogs: StateFlow<List<FrameLog>>

    /** Initiates a connection; result semantics: see the interface contract. */
    suspend fun connect(device: ClockDevice): Result<Unit>

    /** Disconnects actively (fire-and-forget, no reconnection). */
    fun disconnect()

    /** Sends the backlight config; returns NotConnected without touching the device. */
    suspend fun sendBacklight(config: Backlight): Result<Unit>

    /** Resends the whole time frame (includes the current time = implicit sync). */
    suspend fun sendTimers(timers: TimeConfig): Result<Unit>

    /** Sends the switches config. */
    suspend fun sendSwitches(config: SwitchConfig): Result<Unit>

    /** Syncs the device clock to the phone time (resends the whole time frame). */
    suspend fun syncTime(): Result<Unit>

    /** Sends a raw frame from the debug panel. */
    suspend fun sendRaw(bytes: ByteArray): Result<Unit>
}
