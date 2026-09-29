package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.device.ConnectionState
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

    override val initialState
        get() = LightUiState(connection = ConnectionState.CONNECTED, deviceName = "客厅辉光钟")
}
