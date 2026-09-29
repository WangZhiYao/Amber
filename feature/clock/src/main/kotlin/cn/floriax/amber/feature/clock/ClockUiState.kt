package cn.floriax.amber.feature.clock

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.device.ConnectionState

/**
 * Clock screen state (placeholder values).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class ClockUiState(
    /** Connection state for the top bar pill. */
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    /** Device name shown in the pill when connected. */
    val deviceName: String = "",
    /** Timer schedule (power-on/power-off/alarm). */
    val timers: TimeConfig = TimeConfig.DEFAULT,
    /** Function switches. */
    val switches: SwitchConfig = SwitchConfig(),
    /** Epoch millis of the last successful time sync; null = never synced. */
    val lastSyncAt: Long? = null,
    /** Whether a time sync is in flight. */
    val syncing: Boolean = false,
)
