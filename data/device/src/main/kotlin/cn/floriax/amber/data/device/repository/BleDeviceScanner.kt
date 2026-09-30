package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.ble.BleClient
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * [DeviceScanner] on top of the BLE client. Name filtering is the BLE
 * client's job; this layer de-duplicates by MAC and ends the scan after
 * [SCAN_TIMEOUT].
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Singleton
class BleDeviceScanner @Inject constructor(
    private val ble: BleClient,
) : DeviceScanner {

    override val isBluetoothEnabled: Boolean get() = ble.isBluetoothEnabled

    override fun scan(): Flow<List<DiscoveredDevice>> = flow {
        val found = mutableListOf<DiscoveredDevice>()
        withTimeoutOrNull(SCAN_TIMEOUT) {
            ble.scan().collect { result ->
                if (found.none { it.mac == result.mac }) {
                    found.add(DiscoveredDevice(result.name, result.mac, result.rssi))
                    emit(found.toList())
                }
            }
        }
    }

    private companion object {
        /** Auto-scan timeout: results stop arriving after this window. */
        val SCAN_TIMEOUT = 10_000.milliseconds
    }
}
