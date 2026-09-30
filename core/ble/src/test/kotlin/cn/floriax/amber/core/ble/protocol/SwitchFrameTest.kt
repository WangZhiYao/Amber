package cn.floriax.amber.core.ble.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.HexFormat

/**
 * 开关帧单测：实测黄金样例解析、默认帧与编解码往返。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class SwitchFrameTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))

    @Test
    fun `解析§0实测黄金帧`() {
        // 定时开关机开、闹钟关、byte4=1、冒号闪烁开、24小时制
        val f = SwitchFrame.parse(hex("16-01-01-00-01-00-01-00-00-00-00-00-00-E9"))!!
        assertTrue(f.offEnabled)
        assertTrue(f.onEnabled)
        assertFalse(f.alarmEnabled)
        assertEquals(1, f.byte4)
        assertFalse(f.hour12)
        assertTrue(f.colonBlink)
        assertFalse(f.lockRemote)
        assertFalse(f.mute)
    }

    @Test
    fun `默认帧等于文档默认值`() {
        assertEquals(
            "16-00-00-00-01-00-00-00-00-00-00-00-00-E9",
            SwitchFrame.DEFAULT.encode().toHexDisplay()
        )
    }

    @Test
    fun `往返一致`() {
        val f = SwitchFrame(
            offEnabled = true, onEnabled = false, alarmEnabled = true, byte4 = 1,
            hour12 = true, colonBlink = false, lockRemote = true, mute = true,
        )
        assertEquals(f, SwitchFrame.parse(f.encode()))
    }

    @Test
    fun `非开关帧返回null`() {
        assertNull(SwitchFrame.parse(hex("7F-01-01-00-01-00-01-00-00-00-00-00-00-E9")))
    }
}
