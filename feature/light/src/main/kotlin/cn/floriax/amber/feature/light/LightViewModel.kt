package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.BacklightMode
import cn.floriax.amber.domain.light.Preset
import cn.floriax.amber.feature.light.components.hueDegreesToByte
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Light screen ViewModel (placeholder state, no device touched yet).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class LightViewModel @Inject constructor() :
    BaseMVIViewModel<LightUiState, Nothing>() {

    /** Next preset id (placeholder: local list, no persistence). */
    private var nextPresetId = 1L

    /** Fallback name for a blank save-preset input. */
    private var saveFallbackName = ""

    override val initialState
        get() = LightUiState(
            connection = ConnectionState.CONNECTED,
            deviceName = "客厅辉光钟",
            backlight = Backlight(
                hues = List(Backlight.GROUP_COUNT) { 24 },
                saturations = List(Backlight.GROUP_COUNT) { 220 },
                brightness = 128,
                mode = BacklightMode.STATIC,
            ),
            colonBlink = true,
            sameColor = true,
        )

    /** Toggles same-color mode; enabling syncs every group to the first one. */
    fun setSameColor(enabled: Boolean) {
        intent {
            reduce {
                val led = if (enabled) {
                    state.backlight.copy(
                        hues = List(Backlight.GROUP_COUNT) { state.backlight.hues.first() },
                        saturations = List(Backlight.GROUP_COUNT) { state.backlight.saturations.first() },
                    )
                } else {
                    state.backlight
                }
                state.copy(sameColor = enabled, backlight = led)
            }
        }
    }

    /** Selects the digit group targeted by edits (same-color mode off). */
    fun selectGroup(group: Int) {
        intent {
            reduce { state.copy(selectedGroup = group.coerceIn(0, Backlight.GROUP_COUNT - 1)) }
        }
    }

    /** Applies a hue (degrees 0..359) from the wheel. */
    fun setHueDegrees(degrees: Int) {
        intent {
            reduce {
                val byte = degrees.hueDegreesToByte()
                val hues = if (state.sameColor) {
                    List(Backlight.GROUP_COUNT) { byte }
                } else {
                    state.backlight.hues.toMutableList().also { it[state.selectedGroup] = byte }
                }
                state.copy(backlight = state.backlight.copy(hues = hues))
            }
        }
    }

    /** Applies a saturation byte (0..255). */
    fun setSaturation(value: Int) {
        intent {
            reduce {
                val v = value.coerceIn(0, 255)
                val sats = if (state.sameColor) {
                    List(Backlight.GROUP_COUNT) { v }
                } else {
                    state.backlight.saturations.toMutableList().also { it[state.selectedGroup] = v }
                }
                state.copy(backlight = state.backlight.copy(saturations = sats))
            }
        }
    }

    /** Applies a brightness byte (0..255). */
    fun setBrightness(value: Int) {
        intent {
            reduce {
                state.copy(
                    backlight = state.backlight.copy(
                        brightness = value.coerceIn(
                            0,
                            255
                        )
                    )
                )
            }
        }
    }

    /** Applies the display mode. */
    fun setMode(mode: BacklightMode) {
        intent { reduce { state.copy(backlight = state.backlight.copy(mode = mode)) } }
    }

    // ---- Presets (placeholder: local state only, no persistence) ----

    /** Opens the apply-confirmation dialog for a preset. */
    fun onPresetClick(preset: Preset) {
        intent { reduce { state.copy(applyConfirmPreset = preset) } }
    }

    /** Dismisses the apply-confirmation dialog. */
    fun onApplyDismiss() {
        intent { reduce { state.copy(applyConfirmPreset = null) } }
    }

    /** Applies the confirmed preset: whole-backlight replacement, same-color re-derived. */
    fun onApplyConfirm() {
        intent {
            val preset = state.applyConfirmPreset ?: return@intent
            reduce {
                state.copy(
                    applyConfirmPreset = null,
                    backlight = preset.backlight,
                    sameColor = preset.backlight.hues.distinct().size == 1,
                )
            }
        }
    }

    /** Opens the save sheet with a default name (computed in the UI layer). */
    fun onSaveOpen(defaultName: String, fallbackName: String) {
        saveFallbackName = fallbackName
        intent { reduce { state.copy(showSaveSheet = true, saveName = defaultName) } }
    }

    /** Edits the preset name in the save sheet. */
    fun onSaveNameChange(name: String) {
        intent { reduce { state.copy(saveName = name) } }
    }

    /** Dismisses the save sheet. */
    fun onSaveDismiss() {
        intent { reduce { state.copy(showSaveSheet = false) } }
    }

    /** Confirms saving: snapshots the current backlight into a new preset. */
    fun onSaveConfirm() {
        intent {
            val name = state.saveName.ifBlank { saveFallbackName }
            val preset = Preset(
                id = nextPresetId++,
                name = name,
                backlight = state.backlight,
                orderIndex = state.presets.size,
            )
            reduce {
                state.copy(
                    showSaveSheet = false,
                    presets = state.presets + preset,
                )
            }
        }
    }

    /** Opens the long-press manage menu for a preset. */
    fun onPresetLongPress(preset: Preset) {
        intent { reduce { state.copy(managePreset = preset) } }
    }

    /** Dismisses the manage menu. */
    fun onMenuDismiss() {
        intent { reduce { state.copy(managePreset = null) } }
    }

    /** Renames the managed preset. */
    fun onRenamePreset(name: String) {
        intent {
            val target = state.managePreset ?: return@intent
            reduce {
                state.copy(
                    managePreset = null,
                    presets = state.presets.map {
                        if (it.id == target.id) it.copy(name = name) else it
                    },
                )
            }
        }
    }

    /** Deletes the managed preset. */
    fun onDeletePreset() {
        intent {
            val target = state.managePreset ?: return@intent
            reduce {
                state.copy(
                    managePreset = null,
                    presets = state.presets.filterNot { it.id == target.id },
                )
            }
        }
    }
}
