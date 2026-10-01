package cn.floriax.amber.feature.settings

import androidx.lifecycle.viewModelScope
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.repository.DeviceRepository
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings screen ViewModel: the device card projects the saved default
 * device and the live connection; the auto-sync preference is local
 * until preferences persistence lands.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    devices: DeviceRepository,
    clock: ClockRepository,
) : BaseMVIViewModel<SettingsUiState, Nothing>() {

    // Getter form: avoids the base-class construction-time initialization trap.
    override val initialState: SettingsUiState get() = SettingsUiState()

    init {
        viewModelScope.launch {
            devices.observeDevices().collect { list ->
                intent {
                    reduce { state.copy(defaultDevice = list.firstOrNull { it.isDefault }) }
                }
            }
        }
        viewModelScope.launch {
            clock.deviceState.collect { s ->
                intent {
                    reduce {
                        state.copy(connection = s.connection, connectedMac = s.deviceMac)
                    }
                }
            }
        }
    }

    /** Toggles the auto-sync preference (local only). */
    fun setAutoSync(enabled: Boolean) {
        intent {
            reduce { state.copy(autoSync = enabled) }
        }
    }
}
