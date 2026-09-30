package cn.floriax.amber.core.ble

/**
 * Scan result: advertised name, MAC and RSSI.
 * [name] is never blank — the scanning side filters by advertised name
 * (NIXIE / BT24-T only); nameless advertisements never become results.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class BleScanResult(val name: String, val mac: String, val rssi: Int)
