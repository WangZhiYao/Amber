package cn.floriax.amber.domain.device.model

/**
 * Device manager sheet state: saved devices plus the live scan.
 *
 * @property devices saved devices, newest connection first.
 * @property connectedMac MAC of the currently connected device (null
 * when not connected) — drives the per-row connect/disconnect action.
 * @property scanResults scan findings so far, de-duplicated by MAC with
 * the latest RSSI.
 * @property scanning whether a scan is running (auto-ends after the
 * scan window).
 * @property scanFailed the last scan could not run (e.g. Bluetooth is
 * off) — the sheet surfaces it and "rescan" clears it.
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class DeviceManagerState(
    val devices: List<ClockDevice> = emptyList(),
    val connectedMac: String? = null,
    val scanResults: List<DiscoveredDevice> = emptyList(),
    val scanning: Boolean = false,
    val scanFailed: Boolean = false,
)
