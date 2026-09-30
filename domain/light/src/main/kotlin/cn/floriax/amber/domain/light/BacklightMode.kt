package cn.floriax.amber.domain.light

/**
 * LED display mode. [code] is the protocol byte (LED frame byte10):
 * 1=spectrum, 2=breath, 3=static, 4=marquee, 5=rainbow.
 *
 * @param supportsCustomColor whether the mode accepts custom H/S bytes.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
enum class BacklightMode(val code: Int, val supportsCustomColor: Boolean) {
    SPECTRUM(1, false),
    BREATH(2, true),
    STATIC(3, true),
    MARQUEE(4, false),
    RAINBOW(5, false);

    companion object {
        /** Parses a protocol code; null when unknown. */
        fun of(code: Int): BacklightMode? = entries.firstOrNull { it.code == code }
    }
}
