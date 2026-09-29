package cn.floriax.amber.feature.clock

import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Clock screen ViewModel (placeholder state, no device touched yet).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class ClockViewModel @Inject constructor() : BaseMVIViewModel<ClockUiState, Nothing>() {

    override val initialState: ClockUiState
        get() = ClockUiState(
            connection = ConnectionState.CONNECTED,
            deviceName = "客厅辉光钟",
        )

    /** Syncs the device clock to the phone time (simulated placeholder). */
    fun syncTime() {
        intent {
            reduce { state.copy(syncing = true) }
            delay(800)
            reduce {
                state.copy(
                    syncing = false,
                    lastSyncAt = System.currentTimeMillis(),
                )
            }
        }
    }
}
