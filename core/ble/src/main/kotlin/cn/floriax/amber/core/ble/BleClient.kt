package cn.floriax.amber.core.ble

import kotlinx.coroutines.flow.Flow

/**
 * BLE client abstraction (scan/connect); production uses the Nordic
 * implementation, tests swap in a fake.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface BleClient {
    val isBluetoothEnabled: Boolean

    /**
     * Adapter on/off stream (emits the current state on collection). The
     * connection state machine uses it to stop reconnecting while the
     * adapter is off and resume automatically once it is back.
     */
    val bluetoothState: Flow<Boolean>

    /** Cold flow: scans while being collected; the client filters by name. */
    fun scan(): Flow<BleScanResult>

    /** Connect + discover services + locate the characteristic. Throws [BleException] on failure. */
    suspend fun connect(mac: String): BleConnection
}
