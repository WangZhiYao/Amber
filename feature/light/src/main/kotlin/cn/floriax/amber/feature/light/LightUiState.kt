package cn.floriax.amber.feature.light

import cn.floriax.amber.domain.device.ConnectionState

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
)
