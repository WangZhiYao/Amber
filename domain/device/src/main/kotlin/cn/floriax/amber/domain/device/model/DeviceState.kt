package cn.floriax.amber.domain.device.model

import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.light.Backlight

/**
 * Device aggregate state: connection, backlight, timers, switches and the
 * last time sync. Protocol details such as reserved bytes stay out of the
 * domain aggregate (echo rules live in the repository implementation).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class DeviceState(
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val deviceName: String = "",
    /** MAC of the current connection target (null when disconnected). */
    val deviceMac: String? = null,
    val backlight: Backlight = Backlight.DEFAULT,
    val switches: SwitchConfig = SwitchConfig(),
    val timers: TimeConfig = TimeConfig.DEFAULT,
    /** Phone-side epoch millis of the last successful time-frame write
     * (manual and automatic sync are both recorded here); null = never. */
    val lastSyncAt: Long? = null,
)
