package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.ble.BleException
import cn.floriax.amber.core.ble.protocol.toHexDisplay
import cn.floriax.amber.data.device.logger.FrameLogAggregator
import cn.floriax.amber.data.device.testing.FakeBleClient
import cn.floriax.amber.data.device.testing.FakeBleConnection
import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.exception.ClockException
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import no.nordicsemi.android.kotlin.ble.core.data.BleGattOperationStatus
import no.nordicsemi.android.kotlin.ble.core.errors.GattOperationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.HexFormat

/**
 * Clock repository connection-orchestration tests: handshake flow, auto
 * sync, reconnect backoff, active disconnect, and the race regressions
 * (duplicate connects, switching devices mid-handshake, disconnect while a
 * connection attempt is in flight).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClockRepositoryConnectTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))
    private val goldenLed = hex("7F-FF-FF-FF-FF-FF-FF-FF-FF-00-01-00-00-55")
    private val goldenSwitch = hex("16-01-01-00-01-00-01-00-00-00-00-00-00-E9")
    private val goldenTime = hex("99-00-00-00-00-00-00-00-00-00-08-00-00-66")
    private val device = ClockDevice("AA:BB", "钟", "NIXIE", true, 0)

    private fun TestScope.makeRepo(
        fake: FakeBleClient,
        autoSync: Boolean = false,
    ) = ClockRepositoryImpl(
        ble = fake,
        scope = backgroundScope,
        logger = FrameLogAggregator(),
        now = { LocalDateTime.of(2026, 9, 27, 12, 3, 45) },
        autoSync = { autoSync },
    )

    /** Connects and injects the three frames to complete the handshake; returns (repo, conn). */
    private fun TestScope.connectAndHandshake(
        fake: FakeBleClient,
        conn: FakeBleConnection,
        repo: ClockRepositoryImpl,
    ) {
        launch { repo.connect(device) }
        runCurrent()
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
    }

    @Test
    fun `连接后发查询帧并收齐三帧`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        assertEquals(
            "23-00-00-00-00-00-00-00-00-00-00-00-00-E8",
            conn.written.first().toHexDisplay()
        )
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
        assertEquals(255, repo.deviceState.value.backlight.hues[0])
    }

    @Test
    fun `握手完成前保持CONNECTING且写操作被门控`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }
        runCurrent()
        repo.onFrame(goldenLed)   // only one frame: the handshake is incomplete
        runCurrent()
        assertEquals(ConnectionState.CONNECTING, repo.deviceState.value.connection)
        val result = repo.sendBacklight(Backlight.DEFAULT)
        assertTrue(result.exceptionOrNull() is ClockException.NotConnected)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `握手超时视为连接失败并转入退避重连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior =
            { attempts++; if (attempts == 1) conn else throw BleException("gone") }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }
        runCurrent()
        repo.onFrame(goldenLed)   // only one frame: the handshake never completes
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(ConnectionState.RECONNECTING, repo.deviceState.value.connection)
        // The backoff sequence totals 1+2+4=7s (advanceUntilIdle does not advance
        // virtual time for backgroundScope).
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(4, attempts)   // first connect + 3 reconnects
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `autoSync开启时连接后校时并记录上次校时`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake, autoSync = true)
        assertNull(repo.deviceState.value.lastSyncAt)
        connectAndHandshake(fake, conn, repo)
        assertTrue(
            conn.written.any {
                it.size == 14 && (it[0].toInt() and 0xFF) == 0x99 && (it[1].toInt() and 0xFF) == 0x2D
            },
        )
        // Automatic sync must also refresh "last sync" (previously only the
        // manual sync wrote it, so the UI showed a stale timestamp).
        assertNotNull("auto sync should record lastSyncAt", repo.deviceState.value.lastSyncAt)
    }

    @Test
    fun `改定时重发整帧含当前时间`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        repo.sendTimers(
            TimeConfig(LocalTime.of(8, 0), LocalTime.of(23, 30), LocalTime.of(7, 30)),
        )
        assertEquals(
            "99-2D-03-0C-1B-09-1A-1E-17-00-08-1E-07-66",
            conn.written.last().toHexDisplay()
        )
        // Changing a timer resends the whole time frame = implicit sync, so
        // "last sync" is refreshed too.
        assertNotNull(repo.deviceState.value.lastSyncAt)
    }

    /**
     * In-session reconnection never gives up: once the fast backoff
     * (1s/2s/4s) is exhausted the state falls back to disconnected (manual
     * retry available) while slow retries continue in the background until
     * the user disconnects explicitly. The sleeper below does not suspend, so
     * the loop would spin — disconnect() is called on the second slow wait,
     * which both terminates the sequence and asserts "disconnect stops it".
     */
    @Test
    fun `退避耗尽后转低频重试且断开停止`() = runTest {
        val fake = FakeBleClient()
        var attempts = 0
        fake.connectBehavior = { attempts++; throw BleException("fail $attempts") }
        val backoffs = mutableListOf<Long>()
        lateinit var repo: ClockRepositoryImpl
        repo = ClockRepositoryImpl(
            ble = fake, scope = backgroundScope, logger = FrameLogAggregator(),
            now = { LocalDateTime.of(2026, 9, 27, 12, 3, 45) },
            autoSync = { false },
            sleeper = { ms ->
                backoffs.add(ms)
                if (ms == 30_000L && backoffs.count { it == 30_000L } == 2) repo.disconnect()
            },
        )
        repo.connect(device)
        runCurrent()
        assertEquals(5, attempts)   // first connect + 3 fast backoffs + 1st slow retry
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 30_000L, 30_000L), backoffs)
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `低频重试成功后恢复连接`() = runTest {
        val fake = FakeBleClient()
        var attempts = 0
        val conn = FakeBleConnection()
        fake.connectBehavior =
            { attempts++; if (attempts == 5) conn else throw BleException("off") }
        val repo = makeRepo(fake)   // default sleeper → delay → virtual clock
        launch { repo.connect(device) }
        runCurrent()
        advanceTimeBy(5_000); runCurrent()   // first connect fails → backoff
        advanceTimeBy(7_000); runCurrent()   // t=7s fast backoff done → disconnected
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
        advanceTimeBy(25_000); runCurrent()  // t=37s: slow retry #1 starts attempt 5
        assertEquals(5, attempts)
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
    }

    /**
     * Regression: a manual reconnect (home button / pill retry) during a slow
     * wait must cancel the slow sequence — the old sequence must not issue
     * further attempts or touch the state after waking up.
     */
    @Test
    fun `低频等待中用户重连取代慢速序列`() = runTest {
        val fake = FakeBleClient()
        var attempts = 0
        fake.connectBehavior = { attempts++; throw BleException("fail $attempts") }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }
        advanceTimeBy(8_000); runCurrent()   // fast backoff exhausted, slow wait begins
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
        fake.connectBehavior = { attempts++; FakeBleConnection() }
        launch { repo.connect(device) }
        runCurrent()
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
        advanceTimeBy(60_000); runCurrent()  // an uncancelled slow sequence would add attempts here
        assertEquals(5, attempts)            // 4 (fast phase) + 1 (manual), no automatic extra
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `意外断连当刻即置RECONNECTING`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior = { attempts++; conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        conn.drop()
        runCurrent()
        assertEquals(ConnectionState.RECONNECTING, repo.deviceState.value.connection)
    }

    /**
     * Regression: auto-connect from onCreate/onStart plus a repeated "connect"
     * tap on the same device fire concurrent connects — the duplicate must be
     * ignored, otherwise the old GATT is left unowned on the device side.
     */
    @Test
    fun `同一设备重复连接请求被忽略`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior = { attempts++; conn }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }
        runCurrent()
        repo.connect(device)   // handshake still pending (CONNECTING): request again
        runCurrent()
        assertEquals(1, attempts)
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
        assertEquals(1, attempts)
    }

    @Test
    fun `改连另一台设备会关闭旧连接`() = runTest {
        val fake = FakeBleClient()
        val first = FakeBleConnection()
        val second = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior = { attempts++; if (attempts == 1) first else second }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, first, repo)
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
        launch { repo.connect(ClockDevice("CC:DD", "钟2", "NIXIE", false, 0)) }
        runCurrent()
        assertTrue("the old connection should be closed", first.closed)
    }

    @Test
    fun `用户主动断开不重连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior = { attempts++; conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        repo.disconnect()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, attempts)   // no reconnection
    }

    // ---- Write contract: gating and failure translation ----

    @Test
    fun `未连接时各写操作均NotConnected且不写帧`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)   // never connected
        val timers = TimeConfig(LocalTime.of(8, 0), LocalTime.of(23, 30), LocalTime.of(7, 30))
        for (result in listOf(
            repo.sendTimers(timers),
            repo.sendBacklight(Backlight.DEFAULT),
            repo.sendSwitches(SwitchConfig()),
            repo.syncTime(),
            repo.sendRaw(ByteArray(14)),
        )) {
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ClockException.NotConnected)
        }
        assertTrue(conn.written.isEmpty())
    }

    @Test
    fun `已连接时写操作返回Success`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        val result = repo.sendTimers(
            TimeConfig(LocalTime.of(8, 0), LocalTime.of(23, 30), LocalTime.of(7, 30)),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `非链路级写失败转译为Failed且不重连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior = { attempts++; conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        conn.writeBehavior = { throw BleException("write boom") }
        val result = repo.sendTimers(
            TimeConfig(LocalTime.of(8, 0), LocalTime.of(23, 30), LocalTime.of(7, 30)),
        )
        val e = result.exceptionOrNull()
        assertTrue("expected Failed, got $e", e is ClockException.Failed)
        assertEquals("write boom", e?.cause?.message)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(1, attempts)   // a non-link failure does not trigger reconnection
        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `链路级写失败触发断开并退避重连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        var attempts = 0
        fake.connectBehavior =
            { attempts++; if (attempts == 1) conn else throw BleException("gone") }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        conn.writeBehavior = { throw GattOperationException(BleGattOperationStatus.GATT_ERROR) }
        val result = repo.sendBacklight(Backlight.DEFAULT)
        runCurrent()
        assertTrue(result.exceptionOrNull() is ClockException.Failed)
        assertTrue("a link-level write failure should close the old connection", conn.closed)
        assertEquals(ConnectionState.RECONNECTING, repo.deviceState.value.connection)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(4, attempts)   // first connect + 3 reconnects
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
    }

    /**
     * Regression: when a reconnect attempt's handshake succeeds but the
     * immediately following auto-sync write fails at link level, the backoff
     * sequence must not end (it would otherwise sit in RECONNECTING with no
     * reconnect job alive).
     */
    @Test
    fun `重连握手成功后自动校时丢链路仍继续退避`() = runTest {
        val fake = FakeBleClient()
        var attempts = 0
        fake.connectBehavior = {
            attempts++
            FakeBleConnection().also { c ->
                // The time frame (auto sync) write fails while the query frame
                // succeeds — simulating a link drop right after connecting.
                c.writeBehavior = { bytes ->
                    if ((bytes[0].toInt() and 0xFF) == 0x99) {
                        throw GattOperationException(BleGattOperationStatus.GATT_ERROR)
                    }
                    c.written.add(bytes)
                }
            }
        }
        val repo = ClockRepositoryImpl(
            ble = fake, scope = backgroundScope, logger = FrameLogAggregator(),
            now = { LocalDateTime.of(2026, 9, 27, 12, 3, 45) },
            autoSync = { true },
        )
        launch { repo.connect(device) }
        runCurrent()
        // First connect: no reports injected → handshake timeout → backoff.
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(ConnectionState.RECONNECTING, repo.deviceState.value.connection)
        // Reconnect #1: handshake succeeds but auto sync fails → must not stay CONNECTED.
        advanceTimeBy(1_000)
        runCurrent()
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
        assertEquals(ConnectionState.RECONNECTING, repo.deviceState.value.connection)
        // The remaining backoff runs to completion: first connect + 3 reconnects.
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(4, attempts)
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
    }

    /**
     * Regression: connecting to B while A's connection attempt is suspended —
     * A must not steal back the state machine when it resumes: it closes its
     * own connection in place, writes no frame, changes no state and triggers
     * no reconnect for A.
     */
    @Test
    fun `握手等待中改连另一台设备旧尝试不夺回状态机`() = runTest {
        val fake = FakeBleClient()
        val connA = FakeBleConnection()
        val connB = FakeBleConnection()
        val releaseA = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()
        fake.connectBehavior = { mac ->
            calls.add(mac)
            if (mac == "AA:BB") {
                releaseA.await()   // suspend A's connect to open the race window
                connA
            } else {
                connB
            }
        }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }   // A: AA:BB
        runCurrent()
        launch { repo.connect(ClockDevice("CC:DD", "钟2", "NIXIE", false, 0)) }   // B supersedes A
        runCurrent()
        releaseA.complete(Unit)   // A's connect returns: it is stale by now
        runCurrent()
        assertTrue("a stale connection should be closed in place", connA.closed)
        assertTrue("a stale connection must not send the query frame", connA.written.isEmpty())
        assertEquals("CC:DD", repo.deviceState.value.deviceMac)
        // B's timeout-driven reconnects proceed as usual (1+3) with nothing for A.
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(listOf("AA:BB") + List(4) { "CC:DD" }, calls)
    }

    /**
     * Regression: disconnecting while a connection attempt is in flight must
     * not let the late attempt "revive" the connection (disconnect cannot
     * cancel a tryConnect suspended in the caller's coroutine — the
     * generation counter invalidates it instead).
     */
    @Test
    fun `连接在途时主动断开后不复活`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        val gate = CompletableDeferred<Unit>()
        fake.connectBehavior = { gate.await(); conn }
        val repo = makeRepo(fake)
        launch { repo.connect(device) }
        runCurrent()
        repo.disconnect()
        gate.complete(Unit)   // the in-flight connect attempt resumes
        runCurrent()
        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
        assertTrue("a late attempt must not send the query frame", conn.written.isEmpty())
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(
            "and must not enter reconnection",
            ConnectionState.DISCONNECTED,
            repo.deviceState.value.connection
        )
    }

    // ---- Bluetooth adapter state ----

    /**
     * Adapter off is a system-level break: reconnecting cannot succeed, so
     * the state falls back to DISCONNECTED (not RECONNECTING) and all
     * retry loops stop. The connection intent survives for the radio
     * coming back.
     */
    @Test
    fun `蓝牙关闭时停止重连并落回未连接`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)

        fake.bluetoothState.value = false
        runCurrent()

        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(
            "no reconnect attempts while the radio is off",
            ConnectionState.DISCONNECTED,
            repo.deviceState.value.connection
        )
        assertEquals(1, fake.connections.size)
    }

    @Test
    fun `蓝牙恢复后自动续连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)

        fake.bluetoothState.value = false
        runCurrent()

        val conn2 = FakeBleConnection()
        fake.connectBehavior = { conn2 }
        fake.bluetoothState.value = true
        runCurrent()
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()

        assertEquals(ConnectionState.CONNECTED, repo.deviceState.value.connection)
    }

    @Test
    fun `用户主动断开后蓝牙恢复不自动续连`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        fake.connectBehavior = { conn }
        val repo = makeRepo(fake)
        connectAndHandshake(fake, conn, repo)
        repo.disconnect()

        fake.bluetoothState.value = false
        runCurrent()
        fake.bluetoothState.value = true
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(ConnectionState.DISCONNECTED, repo.deviceState.value.connection)
        assertEquals(1, fake.connections.size)
    }
}
