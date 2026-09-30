package cn.floriax.amber.domain.device.model

/**
 * A device found by scanning: advertised name, MAC and signal strength.
 * [name] is never blank — the scanning side filters by advertised name.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class DiscoveredDevice(
    val name: String,
    val mac: String,
    val rssi: Int,
)
