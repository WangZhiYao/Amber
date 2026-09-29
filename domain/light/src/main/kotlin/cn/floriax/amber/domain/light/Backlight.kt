package cn.floriax.amber.domain.light

/**
 * LED backlight config, aligned with the LED frame (0x7F):
 * per-group H/S bytes, global brightness and mode.
 *
 * @param hues hue bytes (0..255) for the four digit groups.
 * @param saturations saturation bytes (0..255), one per group.
 * @param brightness brightness byte (0..255).
 * @param mode the display mode.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class Backlight(
    val hues: List<Int>,
    val saturations: List<Int>,
    val brightness: Int,
    val mode: BacklightMode,
) {
    init {
        require(hues.size == GROUP_COUNT && saturations.size == GROUP_COUNT) {
            "Expected $GROUP_COUNT groups, got hues=${hues.size}, saturations=${saturations.size}"
        }
    }

    companion object {
        const val GROUP_COUNT = 4

        /** Power-on defaults observed on a real device (PROTOCOL.md §0). */
        val DEFAULT = Backlight(
            hues = List(GROUP_COUNT) { 0 },
            saturations = List(GROUP_COUNT) { 255 },
            brightness = 0,
            mode = BacklightMode.SPECTRUM,
        )
    }
}
