package cn.floriax.amber.feature.settings

import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState

/**
 * Settings screen state.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class SettingsUiState(
    /** The default device (connected automatically at startup). */
    val defaultDevice: ClockDevice? = null,
    val autoSync: Boolean = true,
    /** Connection state; the device row status dot is derived from it. */
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedMac: String? = null,
) {
    /** Whether the given device is currently online. */
    fun isOnline(mac: String): Boolean =
        connection == ConnectionState.CONNECTED && mac == connectedMac
}
