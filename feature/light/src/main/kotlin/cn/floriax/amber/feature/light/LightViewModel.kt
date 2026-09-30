package cn.floriax.amber.feature.light

import androidx.lifecycle.viewModelScope
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.exception.ClockException
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.device.usecase.ConnectResult
import cn.floriax.amber.domain.device.usecase.ScanAndConnectUseCase
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.BacklightMode
import cn.floriax.amber.domain.light.Preset
import cn.floriax.amber.feature.light.components.hueDegreesToByte
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import cn.floriax.amber.shared.ui.base.IntentContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Light screen ViewModel: same-color/per-group editing, mode gating and
 * preset management. Write operations call the repository directly — gating
 * and exception translation are part of the repository contract.
 *
 * State is split in two domains: backlight/connection/colonBlink are device
 * projections refreshed on every report, while sameColor/selectedGroup are
 * edit state derived from the device only when a connection is freshly
 * established (a later report must not silently revert the user's choice).
 *
 * Presets are still local to this ViewModel — persistence lands with the
 * preset repository.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class LightViewModel @Inject constructor(
    private val clock: ClockRepository,
    private val scanAndConnect: ScanAndConnectUseCase,
) : BaseMVIViewModel<LightUiState, LightSideEffect>() {

    /** Next preset id (placeholder: local list, no persistence). */
    private var nextPresetId = 1L

    /** Fallback name for a blank save-preset input. */
    private var saveFallbackName = ""

    // Getter form: avoids the base-class construction-time initialization trap.
    override val initialState: LightUiState get() = LightUiState()

    init {
        viewModelScope.launch {
            clock.deviceState.collect { s ->
                intent {
                    reduce {
                        val freshConnection = s.connection == ConnectionState.CONNECTED &&
                                connection != ConnectionState.CONNECTED
                        copy(
                            connection = s.connection,
                            deviceName = s.deviceName,
                            backlight = s.backlight,
                            colonBlink = s.switches.colonBlink,
                            sameColor = if (freshConnection) {
                                s.backlight.hues.distinct().size == 1
                            } else {
                                sameColor
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Connection entry (pill "not connected"): scan and connect to the first
     * glow clock found. Permission is a view-layer concern — the screen asks
     * for it and calls [onPermissionRequired] when it is missing.
     * The scan is reflected in the pill, and a second tap while it runs is
     * ignored instead of starting another scan.
     */
    fun onRetryConnect() = intent {
        if (state.scanning) return@intent
        reduce { copy(scanning = true) }
        val result = scanAndConnect()
        reduce { copy(scanning = false) }
        when (result) {
            is ConnectResult.BluetoothOff -> postSideEffect(LightSideEffect.BluetoothOff)
            is ConnectResult.NoDeviceFound -> postSideEffect(LightSideEffect.NoDeviceFound)
            is ConnectResult.Failed -> postSideEffect(result.cause.toLightSideEffect())
            is ConnectResult.Initiated -> Unit   // device state flows in via the repository
        }
    }

    /** Bluetooth permissions are missing; the screen surfaces this to the user. */
    fun onPermissionRequired() = intent {
        postSideEffect(LightSideEffect.PermissionRequired)
    }

    /** Toggles same-color mode; enabling syncs every group to the selected one. */
    fun setSameColor(enabled: Boolean) = intent {
        if (enabled) {
            // Off → on: sync the selected group's hue AND saturation to all
            // four — syncing hues alone kept a per-group contrast difference
            // alive (e.g. [FF,FF,FF,7A]) and that group rendered visibly
            // brighter than the others.
            val i = state.selectedGroup
            val newBacklight = state.backlight.copy(
                hues = List(Backlight.GROUP_COUNT) { state.backlight.hues[i] },
                saturations = List(Backlight.GROUP_COUNT) { state.backlight.saturations[i] },
            )
            reduce { copy(sameColor = true, backlight = newBacklight) }
            sendBacklight(newBacklight)
        } else {
            // On → off: edit-state switch only — the device has no "same color"
            // concept, so no frame is sent.
            reduce { copy(sameColor = false, selectedGroup = 0) }
        }
    }

    /** Selects the digit group targeted by edits (same-color mode off). */
    fun selectGroup(group: Int) = intent {
        if (!state.sameColor) {
            reduce { copy(selectedGroup = group.coerceIn(0, Backlight.GROUP_COUNT - 1)) }
        }
    }

    /** Applies a hue (degrees 0..359) from the wheel. */
    fun setHueDegrees(degrees: Int) = intent {
        if (!state.backlight.mode.supportsCustomColor) return@intent   // mode gating
        val byte = degrees.hueDegreesToByte()
        val hues = if (state.sameColor) {
            List(Backlight.GROUP_COUNT) { byte }
        } else {
            state.backlight.hues.toMutableList().also { it[state.selectedGroup] = byte }
        }
        changeBacklight { copy(hues = hues) }
    }

    /**
     * Applies a saturation byte to the edited group(s) **while dragging** —
     * preview only, no device write. The BLE frame is sent on drag release
     * ([setSaturation]); writing per drag step flooded the device and made
     * it respond sluggishly.
     */
    fun previewSaturation(value: Int) = intent {
        val v = value.coerceIn(0, 255)
        val saturations = if (state.sameColor) {
            List(Backlight.GROUP_COUNT) { v }
        } else {
            state.backlight.saturations.toMutableList().also { it[state.selectedGroup] = v }
        }
        reduce { copy(backlight = state.backlight.copy(saturations = saturations)) }
    }

    /** Applies a saturation byte (0..255) to the edited group(s) — the drag-release commit. */
    fun setSaturation(value: Int) = intent {
        val v = value.coerceIn(0, 255)
        val saturations = if (state.sameColor) {
            List(Backlight.GROUP_COUNT) { v }
        } else {
            state.backlight.saturations.toMutableList().also { it[state.selectedGroup] = v }
        }
        changeBacklight { copy(saturations = saturations) }
    }

    /**
     * Applies a brightness byte **while dragging** — preview only, no device
     * write (see [previewSaturation] for why).
     */
    fun previewBrightness(value: Int) = intent {
        val v = value.coerceIn(0, 255)
        reduce { copy(backlight = state.backlight.copy(brightness = v)) }
    }

    /** Applies a brightness byte (0..255) — the drag-release commit. */
    fun setBrightness(value: Int) =
        intent { changeBacklight { copy(brightness = value.coerceIn(0, 255)) } }

    /** Applies the display mode. */
    fun setMode(mode: BacklightMode) =
        intent { changeBacklight { copy(mode = mode) } }

    // ---- Presets (placeholder: local list, no persistence) ----

    /** Opens the apply-confirmation dialog for a preset. */
    fun onPresetClick(preset: Preset) = intent {
        reduce { copy(applyConfirmPreset = preset) }
    }

    /** Dismisses the apply-confirmation dialog. */
    fun onApplyDismiss() = intent {
        reduce { copy(applyConfirmPreset = null) }
    }

    /** Applies the confirmed preset: whole-backlight replacement, same-color re-derived. */
    fun onApplyConfirm() = intent {
        val preset = state.applyConfirmPreset ?: return@intent
        reduce {
            copy(
                applyConfirmPreset = null,
                backlight = preset.backlight,
                sameColor = preset.backlight.hues.distinct().size == 1,
            )
        }
        sendBacklight(preset.backlight)
    }

    /** Opens the save sheet with a default name (computed in the UI layer). */
    fun onSaveOpen(defaultName: String, fallbackName: String) = intent {
        saveFallbackName = fallbackName
        reduce { copy(showSaveSheet = true, saveName = defaultName) }
    }

    /** Edits the preset name in the save sheet. */
    fun onSaveNameChange(name: String) = intent {
        reduce { copy(saveName = name) }
    }

    /** Dismisses the save sheet. */
    fun onSaveDismiss() = intent {
        reduce { copy(showSaveSheet = false) }
    }

    /** Confirms saving: snapshots the current backlight into a new preset. */
    fun onSaveConfirm() = intent {
        val name = state.saveName.ifBlank { saveFallbackName }
        val preset = Preset(
            id = nextPresetId++,
            name = name,
            backlight = state.backlight,
            orderIndex = state.presets.size,
        )
        reduce {
            copy(
                showSaveSheet = false,
                presets = state.presets + preset,
            )
        }
    }

    /** Opens the long-press manage menu for a preset. */
    fun onPresetLongPress(preset: Preset) = intent {
        reduce { copy(managePreset = preset) }
    }

    /** Dismisses the manage menu. */
    fun onMenuDismiss() = intent {
        reduce { copy(managePreset = null) }
    }

    /** Renames the managed preset. */
    fun onRenamePreset(name: String) = intent {
        val target = state.managePreset ?: return@intent
        reduce {
            copy(
                managePreset = null,
                presets = state.presets.map {
                    if (it.id == target.id) it.copy(name = name) else it
                },
            )
        }
    }

    /** Deletes the managed preset. */
    fun onDeletePreset() = intent {
        val target = state.managePreset ?: return@intent
        reduce {
            copy(
                managePreset = null,
                presets = state.presets.filterNot { it.id == target.id },
            )
        }
    }

    // ---- Conflated backlight write channel ----

    /** Newest backlight waiting to be written while a frame is still in flight. */
    private var pendingBacklight: Backlight? = null

    /** The drain loop: one write in flight at a time, always the newest value next. */
    private var backlightWriteJob: Job? = null

    /**
     * Schedules [config] to be written, conflated: while a frame is still
     * in flight a newer submission replaces an older pending one. Rapid
     * successive edits used to queue every frame on the BLE write mutex —
     * the device then replayed stale values seconds after the edit
     * (dragging down briefly brightened the clock). At most "current +
     * latest" ever reach the wire.
     *
     * Failures surface as side effects (not connected vs. other failures).
     */
    private fun sendBacklight(config: Backlight) {
        pendingBacklight = config
        if (backlightWriteJob?.isActive == true) return
        backlightWriteJob = viewModelScope.launch {
            while (pendingBacklight != null) {
                val next = pendingBacklight!!
                pendingBacklight = null
                clock.sendBacklight(next).onFailure { e ->
                    intent { postSideEffect(e.toLightSideEffect()) }
                }
            }
        }
    }

    /** Local optimistic update + send (connection gating lives in the repository). */
    private suspend fun IntentContext<LightUiState, LightSideEffect>.changeBacklight(
        transform: Backlight.() -> Backlight,
    ) {
        val newBacklight = state.backlight.transform()
        reduce { copy(backlight = newBacklight) }
        sendBacklight(newBacklight)
    }
}

/** Repository failure → light screen event: "not connected" is not confused with other failures. */
private fun Throwable.toLightSideEffect(): LightSideEffect =
    if (this is ClockException.NotConnected) {
        LightSideEffect.NotConnected
    } else {
        LightSideEffect.WriteFailed(message ?: toString())
    }
