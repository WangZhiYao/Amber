package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.Preset

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
    /** Saved presets. */
    val presets: List<Preset> = emptyList(),
    /** Non-null → apply-confirmation dialog for that preset. */
    val applyConfirmPreset: Preset? = null,
    /** Whether the save-preset sheet is open. */
    val showSaveSheet: Boolean = false,
    /** Name typed in the save-preset sheet. */
    val saveName: String = "",
    /** Non-null → long-press manage menu for that preset. */
    val managePreset: Preset? = null,
)
