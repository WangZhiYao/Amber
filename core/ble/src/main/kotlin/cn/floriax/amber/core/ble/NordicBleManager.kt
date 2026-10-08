package cn.floriax.amber.core.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
import cn.floriax.amber.core.common.di.qualifier.ApplicationIOScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Advertised names the scan filter accepts.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
private val SCAN_NAMES = setOf("NIXIE", "BT24-T")

/** Bluetooth base UUID suffix: 16-bit aliases live on it as `0000xxxx-<suffix>`. */
private const val BASE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb"

/** Service 16-bit aliases (PROTOCOL.md §1): NIXIE uses fff0, BT24-T uses ffe0. */
private val SERVICE_ALIASES = setOf("fff0", "ffe0")

/** Characteristic 16-bit aliases: fff1 (NIXIE) / ffe1 (BT24-T). */
private val CHARACTERISTIC_ALIASES = setOf("fff1", "ffe1")

/**
 * Whether the UUID is one of the glow clock's service aliases on the
 * Bluetooth base UUID. NOTE: the alias is at the **start**
 * (`0000fff0-0000-1000-8000-00805f9b34fb`), not the string end — matching
 * with `endsWith("fff0")` never hit and every connect failed with
 * "service not found" (verified on device via logcat).
 */
internal fun isGlowClockService(uuid: UUID): Boolean =
    with(uuid.toString().lowercase()) {
        length == 36 && endsWith(BASE_UUID_SUFFIX) &&
                substring(0, 8) in SERVICE_ALIASES.map { "0000$it" }
    }

/** Whether the UUID is the glow clock's write+notify characteristic alias. */
internal fun isGlowClockCharacteristic(uuid: UUID): Boolean =
    with(uuid.toString().lowercase()) {
        length == 36 && endsWith(BASE_UUID_SUFFIX) &&
                substring(0, 8) in CHARACTERISTIC_ALIASES.map { "0000$it" }
    }

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

    // Reading the adapter state itself needs Bluetooth permissions; callers
    // hold them (the UI requests them before scanning). A missing permission
    // is reported as "off" rather than crashing the check.
    override val isBluetoothEnabled: Boolean
        @SuppressLint("MissingPermission")
        get() = runCatching {
            context.getSystemService(android.bluetooth.BluetoothManager::class.java)
                ?.adapter?.isEnabled == true
        }.getOrDefault(false)

    /** Emits the current adapter state, then every on/off transition. */
    override val bluetoothState: Flow<Boolean> = callbackFlow {
        trySend(isBluetoothEnabled)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_ON -> trySend(true)
                    BluetoothAdapter.STATE_OFF -> trySend(false)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Scans and emits only matching devices. The device name is read from
     * [BluetoothDevice.getName] with the advertised name (scan record) as
     * fallback — some phones leave getName() null when the name lives only
     * in the scan response, which used to drop every result silently.
     */
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun scan(): Flow<BleScanResult> = flow {
        Log.i(TAG, "Scan started (names=$SCAN_NAMES)")
        // Per-collection state: log each distinct non-matching name once so
        // "scan finds nothing" is diagnosable without flooding logcat.
        val loggedNames = mutableSetOf<String>()
        BleScanner(context).scan().collect { result ->
            val name = result.device.name ?: result.data?.scanRecord?.deviceName
            if (name == null) return@collect
            if (name in SCAN_NAMES) {
                val rssi = result.data?.rssi ?: 0
                Log.i(TAG, "Device found: \"$name\" @ ${result.device.address} (rssi=$rssi)")
                emit(BleScanResult(name, result.device.address, rssi))
            } else if (loggedNames.add(name)) {
                Log.d(TAG, "Ignored \"$name\" @ ${result.device.address} (no filter match)")
            }
        }
    }.onCompletion { cause ->
        when {
            // "firstOrNull() got its element" cancels the upstream with
            // AbortFlowException (a CancellationException) — a normal stop,
            // not a failure.
            cause == null || cause is CancellationException ->
                Log.i(TAG, "Scan stopped")

            else -> Log.e(TAG, "Scan failed: ${cause.message}", cause)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(mac: String): BleConnection {
        Log.i(TAG, "Connecting to $mac ...")
        val gatt = ClientBleGatt.connect(
            context = context,
            macAddress = mac,
            scope = scope,
        )
        try {
            val services = gatt.discoverServices()
            val uuids = services.services.joinToString(", ") { it.uuid.toString() }
            Log.i(TAG, "Services discovered: [$uuids]")
            val service = services.services.firstOrNull { isGlowClockService(it.uuid) }
                ?: throw BleException("Glow clock service not found (discovered: [$uuids])")
            val characteristic =
                service.characteristics.firstOrNull { isGlowClockCharacteristic(it.uuid) }
                    ?: throw BleException(
                        "Write/notify characteristic not found (service ${service.uuid}: " +
                                "[${service.characteristics.joinToString { it.uuid.toString() }}])",
                    )
            Log.i(
                TAG,
                "Characteristic ${characteristic.uuid}, properties ${characteristic.properties}"
            )
            val conn = NordicConnection(gatt, characteristic, scope)
            // Subscribe BEFORE returning: the caller writes the query frame
            // right after, and the CCCD write must precede it on the wire
            // (protocol order; also avoids concurrent native GATT ops).
            conn.subscribe()
            Log.i(TAG, "Connection ready: $mac")
            return conn
        } catch (e: Exception) {
            // This is the failure the reconnect loop used to swallow — log
            // with the cause so "reconnecting" is explainable from logcat.
            Log.e(TAG, "Connect failed: ${e.message}", e)
            // Any failure after the GATT was established must close it —
            // otherwise the connection leaks on the device side.
            gatt.close()
            throw e
        }
    }

    private companion object {
        private const val TAG = "NordicBleManager"
    }
}
