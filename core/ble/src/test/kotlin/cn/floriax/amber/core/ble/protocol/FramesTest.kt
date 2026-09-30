package cn.floriax.amber.core.ble.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.HexFormat

/**
 * 协议帧类型识别单测：帧头帧尾、定长 14 字节校验。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class FramesTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))

    @Test
    fun `查询帧为固定14字节`() {
        assertEquals(14, QueryFrame.BYTES.size)
        assertEquals("23-00-00-00-00-00-00-00-00-00-00-00-00-E8", QueryFrame.BYTES.toHexDisplay())
    }

    @Test
    fun `按帧头帧尾识别类型`() {
        assertEquals(
            FrameKind.QUERY,
            FrameKind.of(hex("23-00-00-00-00-00-00-00-00-00-00-00-00-E8"))
        )
        assertEquals(FrameKind.LED, FrameKind.of(hex("7F-00-00-00-00-00-00-00-00-00-00-00-00-55")))
        assertEquals(FrameKind.TIME, FrameKind.of(hex("99-00-00-00-00-00-00-00-00-00-00-00-00-66")))
        assertEquals(
            FrameKind.SWITCH,
            FrameKind.of(hex("16-00-00-00-00-00-00-00-00-00-00-00-00-E9"))
        )
    }

    @Test
    fun `长度不是14返回null`() {
        assertNull(FrameKind.of(byteArrayOf(0x23)))
        assertNull(FrameKind.of(ByteArray(15) { 0 }))
    }

    @Test
    fun `帧头或帧尾不匹配返回null`() {
        assertNull(FrameKind.of(hex("7F-00-00-00-00-00-00-00-00-00-00-00-00-66"))) // LED头+TIME尾
        assertNull(FrameKind.of(hex("FF-00-00-00-00-00-00-00-00-00-00-00-00-55")))
    }
}
