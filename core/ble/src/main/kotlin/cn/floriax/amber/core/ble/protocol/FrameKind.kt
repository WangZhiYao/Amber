package cn.floriax.amber.core.ble.protocol

/**
 * Header/tail identification of the four frame kinds. This layer is a pure
 * protocol layer with zero business dependencies and **holds no display
 * text**: log labels use the locale-free enum names; UI-facing labels are
 * mapped in the UI layer.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
enum class FrameKind(val header: Int, val tail: Int) {
    LED(0x7F, 0x55),
    TIME(0x99, 0x66),
    SWITCH(0x16, 0xE9),
    QUERY(0x23, 0xE8);

    companion object {
        /**
         * Returns the kind when the length is 14 and header/tail match, else null.
         */
        fun of(bytes: ByteArray): FrameKind? {
            if (bytes.size != Frames.LENGTH) return null
            val head = bytes[0].toInt() and 0xFF
            val tail = bytes[13].toInt() and 0xFF
            return entries.firstOrNull { it.header == head && it.tail == tail }
        }
    }
}
