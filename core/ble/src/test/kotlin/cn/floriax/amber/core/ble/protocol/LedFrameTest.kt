package cn.floriax.amber.core.ble.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.HexFormat

/**
 * LED 帧单测：§0 实测黄金样例解析、4 组独立饱和度与编解码往返。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class LedFrameTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))

    // PROTOCOL.md §0 实测黄金样例：4组H/S=255，亮度0，模式1光谱循环
    private val golden = hex("7F-FF-FF-FF-FF-FF-FF-FF-FF-00-01-00-00-55")

    @Test
    fun `解析实测黄金帧`() {
        val f = LedFrame.parse(golden)!!
        assertArrayEquals(intArrayOf(255, 255, 255, 255), f.hues)
        assertArrayEquals(intArrayOf(255, 255, 255, 255), f.saturations)
        assertEquals(0, f.brightness)
        assertEquals(1, f.mode)
        assertArrayEquals(intArrayOf(0, 0), f.reserved)
    }

    @Test
    fun `四组饱和度独立编解码`() {
        // 四组 S 各不相同（255/40/255/40）——验证帧模型与线格式逐组 1:1
        val f = LedFrame(
            hues = intArrayOf(0, 0, 0, 0),
            saturations = intArrayOf(255, 40, 255, 40),
            brightness = 200,
            mode = 3,
        )
        assertEquals(
            "7F-00-FF-00-28-00-FF-00-28-C8-03-00-00-55",
            f.encode().toHexDisplay(),
        )
        val parsed = LedFrame.parse(f.encode())!!
        assertArrayEquals(f.saturations, parsed.saturations)
    }

    @Test
    fun `往返一致`() {
        val f = LedFrame(
            intArrayOf(0x1E, 0x1E, 0xC8, 0x96),
            intArrayOf(0x3C, 0x28, 0x64, 0x96),
            0x64, 3, intArrayOf(1, 2),
        )
        val parsed = LedFrame.parse(f.encode())!!
        assertArrayEquals(f.hues, parsed.hues)
        assertArrayEquals(f.saturations, parsed.saturations)
        assertEquals(f.brightness, parsed.brightness)
        assertEquals(f.mode, parsed.mode)
        assertArrayEquals(f.reserved, parsed.reserved)
    }

    @Test
    fun `帧头不是7F返回null`() {
        assertNull(LedFrame.parse(hex("99-FF-FF-FF-FF-FF-FF-FF-FF-00-01-00-00-55")))
    }
}
