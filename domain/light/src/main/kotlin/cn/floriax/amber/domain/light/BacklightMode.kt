package cn.floriax.amber.domain.light

/**
 * LED display mode.
 *
 * @param supportsCustomColor whether the mode accepts custom H/S bytes.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
enum class BacklightMode(val supportsCustomColor: Boolean) {
    SPECTRUM(false),
    BREATH(true),
    STATIC(true),
    MARQUEE(false),
    RAINBOW(false),
}
