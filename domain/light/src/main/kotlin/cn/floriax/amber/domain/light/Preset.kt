package cn.floriax.amber.domain.light

/**
 * Saved light configuration (a snapshot of the full backlight state).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class Preset(
    val id: Long,
    val name: String,
    val backlight: Backlight,
    val orderIndex: Int,
)
