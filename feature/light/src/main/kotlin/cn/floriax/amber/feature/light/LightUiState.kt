package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.domain.light.Backlight

/**
 * Light screen state (placeholder values).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class LightUiState(
    /** Connection state for the top bar pill. */
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    /** Device name shown in the pill when connected. */
    val deviceName: String = "",
    /** LED backlight config (per-group H/S, brightness, mode). */
    val backlight: Backlight = Backlight.DEFAULT,
    /** Whether the colon of the digit preview blinks. */
    val colonBlink: Boolean = false,
    /** Same-color mode: color edits apply to all four groups at once. */
    val sameColor: Boolean = true,
    /** The digit group targeted by edits when not in same-color mode. */
    val selectedGroup: Int = 0,
)
