package cn.floriax.amber.core.ble

import android.annotation.SuppressLint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattCharacteristic
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wrapper around a Nordic ClientBleGatt connection.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class NordicConnection(
    private val gatt: ClientBleGatt,
    private val characteristic: ClientBleGattCharacteristic,
    private val scope: CoroutineScope,
) : BleConnection {

    // replay = 1: the repository subscribes only after installSession — a
    // disconnect fired before that subscription must still be delivered,
    // otherwise the loss is silent (masked only by handshake timeout).
    private val _disconnected = MutableSharedFlow<Throwable?>(replay = 1)
    override val disconnected: SharedFlow<Throwable?> = _disconnected

    /** The disconnect signal is emitted at most once: both the GATT state listener and [close] can trigger it — a duplicate emission would make the upper layer misread it as multiple disconnects. */
    private val disconnectSignalled = AtomicBoolean(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notifications: Flow<ByteArray> = flow {
        characteristic.getNotifications().collect { data: DataByteArray ->
            emit(data.value)
        }
    }.buffer(Channel.UNLIMITED)

    init {
        scope.launch {
            gatt.connectionState.first { it == GattConnectionState.STATE_DISCONNECTED }
            signalDisconnected()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun write(bytes: ByteArray) {
        characteristic.write(DataByteArray.from(*bytes))
    }

    @SuppressLint("MissingPermission")
    override fun close() {
        gatt.close()
        signalDisconnected()
    }

    private fun signalDisconnected() {
        if (disconnectSignalled.compareAndSet(false, true)) _disconnected.tryEmit(null)
    }
}
