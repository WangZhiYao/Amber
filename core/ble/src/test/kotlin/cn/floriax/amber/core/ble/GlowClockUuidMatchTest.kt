package cn.floriax.amber.core.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * 辉光钟服务/特征 UUID 匹配单测。
 *
 * 设备使用 Bluetooth base UUID 上的 16 位短码（fff0/ffe0 服务、fff1/ffe1
 * 特征）：完整形式 `0000fff0-0000-1000-8000-00805f9b34fb`——短码在
 * UUID **开头**，不在结尾。真机日志证实 `endsWith("fff0")` 从未匹配过，
 * 每次连接都死在 "Glow clock service not found"。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class GlowClockUuidMatchTest {

    @Test
    fun `匹配fff0服务`() {
        assertTrue(isGlowClockService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `匹配ffe0服务`() {
        assertTrue(isGlowClockService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `大写UUID同样匹配`() {
        assertTrue(isGlowClockService(UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")))
    }

    @Test
    fun `特征短码不算服务`() {
        assertFalse(isGlowClockService(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `匹配fff1特征`() {
        assertTrue(isGlowClockCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `匹配ffe1特征`() {
        assertTrue(isGlowClockCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `服务短码不算特征`() {
        assertFalse(isGlowClockCharacteristic(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")))
    }

    @Test
    fun `非baseUUID不匹配`() {
        // 设备厂商自定义 128 位 UUID（不以 Bluetooth base UUID 结尾）
        assertFalse(isGlowClockService(UUID.fromString("fff00000-0000-1000-8000-00805f9b34fb")))
        assertFalse(isGlowClockService(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")))
    }
}
