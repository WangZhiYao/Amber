package cn.floriax.amber.core.ble.protocol

/**
 * LED frame layout: [0]=0x7F, [1..8]=4 groups of (H,S), [9]=brightness,
 * [10]=mode (1..5), [11..12]=reserved (echoed), [13]=0x55.
 *
 * The four S bytes each occupy their own slot on the wire (bytes 2/4/6/8);
 * the frame model maps the wire format 1:1 per group. Note: the vendor
 * mini-program UI offers a single contrast slider and fills all four slots
 * with the same value — that is a UI limitation, not a verified firmware
 * constraint (whether the firmware parses S per group has not been tested
 * on a real device). The model therefore does not presuppose the
 * limitation.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class LedFrame(
    val hues: IntArray,           // hue bytes 0..255, one per group
    val saturations: IntArray,    // saturation (contrast) bytes 0..255, one per group
    val brightness: Int,          // 0..255
    val mode: Int,                // 1..5
    val reserved: IntArray = intArrayOf(0, 0),    // bytes 11-12, echoed on send
) {
    init {
        require(hues.size == 4 && saturations.size == 4 && reserved.size == 2)
    }

    fun encode(): ByteArray = byteArrayOf(
        0x7F,
        hues[0].toByte(), saturations[0].toByte(),
        hues[1].toByte(), saturations[1].toByte(),
        hues[2].toByte(), saturations[2].toByte(),
        hues[3].toByte(), saturations[3].toByte(),
        brightness.toByte(), mode.toByte(),
        reserved[0].toByte(), reserved[1].toByte(),
        0x55,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LedFrame) return false

        if (brightness != other.brightness) return false
        if (mode != other.mode) return false
        if (!hues.contentEquals(other.hues)) return false
        if (!saturations.contentEquals(other.saturations)) return false
        if (!reserved.contentEquals(other.reserved)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = brightness
        result = 31 * result + mode
        result = 31 * result + hues.contentHashCode()
        result = 31 * result + saturations.contentHashCode()
        result = 31 * result + reserved.contentHashCode()
        return result
    }

    companion object {
        /**
         * Parses an LED frame; null when header/tail don't match.
         */
        fun parse(bytes: ByteArray): LedFrame? {
            if (FrameKind.of(bytes) != FrameKind.LED) return null
            return LedFrame(
                hues = intArrayOf(
                    bytes[1].toInt() and 0xFF,
                    bytes[3].toInt() and 0xFF,
                    bytes[5].toInt() and 0xFF,
                    bytes[7].toInt() and 0xFF,
                ),
                saturations = intArrayOf(
                    bytes[2].toInt() and 0xFF,
                    bytes[4].toInt() and 0xFF,
                    bytes[6].toInt() and 0xFF,
                    bytes[8].toInt() and 0xFF,
                ),
                brightness = bytes[9].toInt() and 0xFF,
                mode = bytes[10].toInt() and 0xFF,
                reserved = intArrayOf(bytes[11].toInt() and 0xFF, bytes[12].toInt() and 0xFF),
            )
        }
    }
}
