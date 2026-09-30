package cn.floriax.amber.core.ble

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Scriptable BLE fake: connect behavior, notifications and disconnects are
 * all programmable.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class FakeBleClient : BleClient {
    override val isBluetoothEnabled = true
    var connectBehavior: suspend (String) -> BleConnection = { FakeBleConnection() }
    val connections = mutableListOf<FakeBleConnection>()

    override fun scan(): Flow<BleScanResult> =
        MutableStateFlow(BleScanResult("NIXIE", "AA:BB:CC:DD:EE:FF", -50))

    override suspend fun connect(mac: String): BleConnection =
        connectBehavior(mac).also { connections.add(it as FakeBleConnection) }
}

/**
 * Programmable BLE connection fake: records written frames, simulates
 * notifications and disconnects.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class FakeBleConnection : BleConnection {
    private val _disconnected = Channel<Throwable?>(capacity = 1)
    override val disconnected: Flow<Throwable?> = _disconnected.receiveAsFlow()

    private val _notifications = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    override val notifications: Flow<ByteArray> = _notifications.receiveAsFlow()

    var closed = false
    val written = mutableListOf<ByteArray>()

    /** Programmable write behavior: records frames by default; tests may replace it to throw and simulate write failures. */
    var writeBehavior: suspend (ByteArray) -> Unit = { written.add(it) }

    override suspend fun write(bytes: ByteArray) {
        writeBehavior(bytes)
    }

    override fun close() {
        closed = true
        _disconnected.trySend(null)
    }

    suspend fun notify(bytes: ByteArray) {
        _notifications.send(bytes)
    }

    fun drop(cause: Throwable? = null) {
        _disconnected.trySend(cause)
    }
}
