package cn.floriax.amber.core.ble.protocol

/**
 * Switch frame: [1]timed-power-off [2]timed-power-on [3]alarm [4]fixed 1 (echoed)
 * [5]hour format (0=24h, 1=12h) [6]colon blink [7]remote lock [8]mute,
 * [9-12]reserved 0.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class SwitchFrame(
    val offEnabled: Boolean,
    val onEnabled: Boolean,
    val alarmEnabled: Boolean,
    val byte4: Int,
    val hour12: Boolean,
    val colonBlink: Boolean,
    val lockRemote: Boolean,
    val mute: Boolean,
) {
    fun encode(): ByteArray = byteArrayOf(
        0x16,
        offEnabled.b2i().toByte(), onEnabled.b2i().toByte(), alarmEnabled.b2i().toByte(),
        byte4.toByte(), hour12.b2i().toByte(), colonBlink.b2i().toByte(),
        lockRemote.b2i().toByte(), mute.b2i().toByte(),
        0, 0, 0, 0,
        0xE9.toByte(),
    )

    /**
     * Boolean → protocol byte value.
     */
    internal fun Boolean.b2i() = if (this) 1 else 0

    companion object {
        /**
         * Documented default switch frame (byte4=1, everything else off).
         */
        val DEFAULT = SwitchFrame(
            offEnabled = false, onEnabled = false, alarmEnabled = false, byte4 = 1,
            hour12 = false, colonBlink = false, lockRemote = false, mute = false,
        )

        /**
         * Parses a switch frame; null when header/tail don't match.
         */
        fun parse(bytes: ByteArray): SwitchFrame? {
            if (FrameKind.of(bytes) != FrameKind.SWITCH) return null
            fun b(i: Int) = (bytes[i].toInt() and 0xFF) == 1
            return SwitchFrame(
                offEnabled = b(1), onEnabled = b(2), alarmEnabled = b(3),
                byte4 = bytes[4].toInt() and 0xFF,
                hour12 = b(5), colonBlink = b(6), lockRemote = b(7), mute = b(8),
            )
        }
    }
}
