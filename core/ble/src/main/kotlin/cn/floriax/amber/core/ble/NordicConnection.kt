package cn.floriax.amber.core.ble

import android.annotation.SuppressLint
import android.util.Log
import cn.floriax.amber.core.ble.protocol.toHexDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattCharacteristic
import no.nordicsemi.android.kotlin.ble.core.data.BleGattProperty
import no.nordicsemi.android.kotlin.ble.core.data.BleWriteType
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Write type picked from the characteristic's actual properties.
 *
 * The Nordic library's default ([BleWriteType.DEFAULT], acknowledged write)
 * hard-requires [BleGattProperty.PROPERTY_WRITE] and throws
 * `MissingPropertyException` otherwise — and cheap fff1/ffe1 serial modules
 * frequently expose only WRITE_WITHOUT_RESPONSE + NOTIFY. A fixed DEFAULT
 * made every handshake write fail on such devices; choosing per properties
 * works on both.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
internal fun chooseWriteType(properties: List<BleGattProperty>): BleWriteType =
    when {
        BleGattProperty.PROPERTY_WRITE in properties -> BleWriteType.DEFAULT
        BleGattProperty.PROPERTY_WRITE_NO_RESPONSE in properties -> BleWriteType.NO_RESPONSE
        else -> throw BleException(
            "Characteristic has no write property (properties: $properties)",
        )
    }

/**
 * Wrapper around a Nordic ClientBleGatt connection.
 *
 * Notification subscription is **explicit**: [subscribe] is called by the
 * factory (NordicBleManager.connect) and returns only after the CCCD
 * descriptor has been written, i.e. notifications are genuinely enabled on
 * the device. The upper layer may then write the query frame — the protocol
 * order "subscribe notify → send query" (PROTOCOL.md §1) holds without a
 * race between the CCCD descriptor write and the characteristic write (the
 * library serialises them per feature, not globally).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class NordicConnection(
    private val gatt: ClientBleGatt,
    private val characteristic: ClientBleGattCharacteristic,
    private val scope: CoroutineScope,
) : BleConnection {

    private val writeType = chooseWriteType(characteristic.properties)

    // replay = 1: the repository subscribes only after installSession — a
    // disconnect fired before that subscription must still be delivered,
    // otherwise the loss is silent (masked only by handshake timeout).
    private val _disconnected = MutableSharedFlow<Throwable?>(replay = 1)
    override val disconnected: SharedFlow<Throwable?> = _disconnected

    // Hot stream fed by the single background collector started in [subscribe].
    // The repository may (re)collect it at any time; reports pushed before its
    // collection starts are held by the buffer rather than lost.
    private val _notifications = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = NOTIFICATION_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val notifications: SharedFlow<ByteArray> = _notifications

    private var subscriptionJob: Job? = null

    /** The disconnect signal is emitted at most once: both the GATT state listener and [close] can trigger it — a duplicate emission would make the upper layer misread it as multiple disconnects. */
    private val disconnectSignalled = AtomicBoolean(false)

    init {
        scope.launch {
            gatt.connectionState.first { it == GattConnectionState.STATE_DISCONNECTED }
            Log.w(TAG, "GATT link dropped (${characteristic.uuid})")
            signalDisconnected()
        }
    }

    /**
     * Enables notifications and returns once the device has acknowledged the
     * CCCD write ([ClientBleGattCharacteristic.getNotifications] suspends
     * through it). Throws on link loss / missing CCCD — the factory then
     * closes the GATT and the failure surfaces as a normal connect failure.
     */
    @SuppressLint("MissingPermission")
    suspend fun subscribe() {
        val flow = characteristic.getNotifications()
        Log.i(TAG, "Notify enabled: ${characteristic.uuid} (writeType=$writeType)")
        subscriptionJob = scope.launch {
            try {
                flow.collect { data: DataByteArray ->
                    Log.d(TAG, "RX ${data.value.toHexDisplay()}")
                    _notifications.tryEmit(data.value)
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e   // close()/session teardown — not an anomaly
            } catch (e: Exception) {
                Log.e(TAG, "Notification stream ended: ${e.message}", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun write(bytes: ByteArray) {
        Log.d(TAG, "TX ${bytes.toHexDisplay()}")
        characteristic.write(DataByteArray.from(*bytes), writeType)
    }

    @SuppressLint("MissingPermission")
    override fun close() {
        Log.i(TAG, "Closing connection")
        subscriptionJob?.cancel()
        gatt.close()
        signalDisconnected()
    }

    private fun signalDisconnected() {
        if (disconnectSignalled.compareAndSet(false, true)) _disconnected.tryEmit(null)
    }

    private companion object {
        private const val TAG = "NordicConnection"

        /** Frames are 14 bytes and arrive in a burst of three — 64 slots hold any burst with margin. */
        const val NOTIFICATION_BUFFER = 64
    }
}
