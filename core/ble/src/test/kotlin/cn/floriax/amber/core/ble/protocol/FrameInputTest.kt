package cn.floriax.amber.core.ble.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.HexFormat

/**
 * 手动发帧输入解析单测：分隔符容忍、定长与帧头尾校验。
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
class FrameInputTest {

    private fun bytes(hex: String) = HexFormat.of().parseHex(hex)

    @Test
    fun `合法查询帧带分隔符`() {
        assertArrayEquals(
            bytes("23000000000000000000000000E8"),
            parseFrameInput("23-00-00-00-00-00-00-00-00-00-00-00-00-E8"),
        )
    }

    @Test
    fun `空格分隔与无分隔符同样接受`() {
        val expected = bytes("7F00000000000000000000000055")
        assertArrayEquals(expected, parseFrameInput("7F 00 00 00 00 00 00 00 00 00 00 00 00 55"))
        assertArrayEquals(expected, parseFrameInput("7F00000000000000000000000055"))
    }

    @Test
    fun `首尾空白被容忍`() {
        assertArrayEquals(
            bytes("9900000000000000000000000066"),
            parseFrameInput(" 99-00-00-00-00-00-00-00-00-00-00-00-00-66 "),
        )
    }

    @Test
    fun `长度不是14字节返回null`() {
        assertNull(parseFrameInput("23-00"))
        assertNull(parseFrameInput("23-00-00-00-00-00-00-00-00-00-00-00-00-00-E8"))
    }

    @Test
    fun `非hex字符返回null`() {
        assertNull(parseFrameInput("ZZ-00-00-00-00-00-00-00-00-00-00-00-00-E8"))
    }

    @Test
    fun `帧头尾不合法返回null`() {
        // 长度正确但头尾不属于任何帧类型。
        assertNull(parseFrameInput("24-00-00-00-00-00-00-00-00-00-00-00-00-E8"))
        assertNull(parseFrameInput("23-00-00-00-00-00-00-00-00-00-00-00-00-99"))
    }
}
