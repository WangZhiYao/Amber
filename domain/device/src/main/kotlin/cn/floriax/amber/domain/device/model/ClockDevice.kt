package cn.floriax.amber.domain.device.model

/**
 * Glow clock device (domain entity).
 *
 * @param mac the device MAC address.
 * @param alias the user-defined device alias.
 * @param advertisedName the BLE advertised name, NIXIE or BT24-T.
 * @param isDefault whether this is the default device connected at startup.
 * @param lastConnectedAt epoch millis of the last successful connection.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class ClockDevice(
    val mac: String,
    val alias: String,
    val advertisedName: String,
    val isDefault: Boolean,
    val lastConnectedAt: Long,
)
