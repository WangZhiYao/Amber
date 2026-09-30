package cn.floriax.amber.core.ble

import android.annotation.SuppressLint
import android.content.Context
import cn.floriax.amber.core.common.di.qualifier.ApplicationIOScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner
import javax.inject.Inject
import javax.inject.Singleton
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResult as NordicScanResult

/**
 * Advertised names the scan filter accepts.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
private val SCAN_NAMES = setOf("NIXIE", "BT24-T")

/**
 * Nordic Kotlin BLE 1.3.1 implementation.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Singleton
class NordicBleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationIOScope private val scope: CoroutineScope,
) : BleClient {

    override val isBluetoothEnabled: Boolean
        get() = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
            ?.adapter?.isEnabled == true

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scan(): Flow<BleScanResult> = BleScanner(context).scan()
        .mapNotNull { result: NordicScanResult ->
            result.device.name?.takeIf { it in SCAN_NAMES }?.let { name ->
                BleScanResult(name, result.device.address, result.data?.rssi ?: 0)
            }
        }

    @SuppressLint("MissingPermission")
    override suspend fun connect(mac: String): BleConnection {
        val gatt = ClientBleGatt.connect(
            context = context,
            macAddress = mac,
            scope = scope,
        )
        try {
            val services = gatt.discoverServices()
            val service = services.services.firstOrNull {
                val uuid = it.uuid.toString().lowercase()
                uuid.endsWith("fff0") || uuid.endsWith("ffe0")
            } ?: throw BleException("Glow clock service not found")
            val characteristic = service.characteristics.firstOrNull {
                val uuid = it.uuid.toString().lowercase()
                uuid.endsWith("fff1") || uuid.endsWith("ffe1")
            } ?: throw BleException("Write/notify characteristic not found")
            return NordicConnection(gatt, characteristic, scope)
        } catch (e: Exception) {
            // Any failure after the GATT was established must close it —
            // otherwise the connection leaks on the device side.
            gatt.close()
            throw e
        }
    }
}
