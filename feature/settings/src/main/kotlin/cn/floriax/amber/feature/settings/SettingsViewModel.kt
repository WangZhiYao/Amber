package cn.floriax.amber.feature.settings

import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Settings screen ViewModel (placeholder state, no device touched yet).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : BaseMVIViewModel<SettingsUiState, Nothing>() {

    override val initialState
        get() = SettingsUiState(
            defaultDevice = ClockDevice(
                mac = "AA:BB:CC:DD:EE:FF",
                alias = "客厅辉光钟",
                advertisedName = "NIXIE",
                isDefault = true,
                lastConnectedAt = 0L,
            ),
            autoSync = true,
            connection = ConnectionState.CONNECTED,
            connectedMac = "AA:BB:CC:DD:EE:FF",
        )

    /** Toggles the auto-sync preference (local only). */
    fun setAutoSync(enabled: Boolean) {
        intent {
            reduce { state.copy(autoSync = enabled) }
        }
    }
}
