package cn.floriax.amber.data.device.repository

import cn.floriax.amber.data.device.logger.FrameLogAggregator
import cn.floriax.amber.data.device.testing.FakeBleClient
import cn.floriax.amber.data.device.testing.FakeBleConnection
import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.device.model.FrameLog
import cn.floriax.amber.domain.light.Backlight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.HexFormat

/**
 * Clock repository state-machine tests: three-frame reports update the
 * state, unknown frames are ignored and logged, reserved bytes are echoed.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClockRepositoryImplTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))
    private val goldenLed = hex("7F-FF-FF-FF-FF-FF-FF-FF-FF-00-01-00-00-55")
    private val goldenSwitch = hex("16-01-01-00-01-00-01-00-00-00-00-00-00-E9")
    private val goldenTime = hex("99-00-00-00-00-00-00-00-00-00-08-00-00-66")
    private val device = ClockDevice("AA:BB", "钟", "NIXIE", true, 0)

    private fun TestScope.repo(fake: FakeBleClient) = ClockRepositoryImpl(
        ble = fake,
        scope = backgroundScope,
        logger = FrameLogAggregator(),
        now = { LocalDateTime.of(2026, 9, 27, 12, 3, 45) },
        autoSync = { false },
    )

    /**
     * Connects and injects the three frames to complete the handshake.
     * Multi-element delivery over the notify pipeline is unreliable under
     * coroutines-test, so frames are injected via onFrame directly.
     */
    private fun TestScope.connectAndHandshake(
        fake: FakeBleClient,
        conn: FakeBleConnection,
        repo: ClockRepositoryImpl,
    ) {
        fake.connectBehavior = { conn }
        launch { repo.connect(device) }
        runCurrent()
        repo.onFrame(goldenLed)
        repo.onFrame(goldenSwitch)
        repo.onFrame(goldenTime)
        runCurrent()
    }

    @Test
    fun `收到三帧后状态更新`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        val repo = repo(fake)
        connectAndHandshake(fake, conn, repo)
        val s = repo.deviceState.value
        assertEquals(ConnectionState.CONNECTED, s.connection)
        assertEquals(255, s.backlight.hues[0])
        assertEquals(true, s.switches.colonBlink)
        assertEquals(LocalTime.of(8, 0), s.timers.powerOn)
        assertTrue(repo.frameLogs.value.count { it.direction == FrameLog.Direction.RX } >= 3)
    }

    @Test
    fun `未知帧忽略并记SYS日志`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        val repo = repo(fake)
        connectAndHandshake(fake, conn, repo)
        val ledBefore = repo.deviceState.value.backlight
        repo.onFrame(ByteArray(14) { 0xFF.toByte() })
        repo.onFrame(hex("7F-00-00-00-00-00-00-00-00-00-00-00-00-66")) // LED头错尾
        assertEquals(ledBefore, repo.deviceState.value.backlight)
        assertTrue(repo.frameLogs.value.any { it.direction == FrameLog.Direction.SYS })
    }

    // ---- Reserved-byte echo (protocol rule: echo back what the report carried) ----

    @Test
    fun `下发LED帧原样回带回报的保留字节`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        val repo = repo(fake)
        connectAndHandshake(fake, conn, repo)
        // Inject an LED report whose reserved bytes (byte11-12) are 0A-0B.
        repo.onFrame(hex("7F-1E-3C-1E-3C-1E-3C-1E-3C-64-03-0A-0B-55"))
        repo.sendBacklight(Backlight.DEFAULT).getOrThrow()
        val sent = conn.written.last()
        assertEquals(0x0A, sent[11].toInt() and 0xFF)
        assertEquals(0x0B, sent[12].toInt() and 0xFF)
    }

    @Test
    fun `下发开关帧回带回报的byte4`() = runTest {
        val fake = FakeBleClient()
        val conn = FakeBleConnection()
        val repo = repo(fake)
        connectAndHandshake(fake, conn, repo)
        // Inject a switch report with byte4=0 (not the default 1).
        repo.onFrame(hex("16-01-00-00-00-00-01-00-00-00-00-00-00-E9"))
        repo.sendSwitches(SwitchConfig(colonBlink = true)).getOrThrow()
        assertEquals(0, conn.written.last()[4].toInt() and 0xFF)
    }
}
