package cn.floriax.amber.domain.device

import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.domain.device.model.DeviceManagerState
import cn.floriax.amber.domain.device.model.DiscoveredDevice
import cn.floriax.amber.domain.device.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Device manager sheet coordinator: one shared instance behind the sheet
 * opened from the clock/light/settings pills. Aggregates three sources
 * (device persistence, scanner, connection) the same way
 * [usecase.ScanAndConnectUseCase] does — which is why it lives in the
 * domain layer instead of a per-screen ViewModel: the sheet is mounted by
 * three screens and must show one consistent state.
 *
 * The class is a plain domain object (no DI annotations): production
 * wiring happens in :data:device's DI module, which provides it as a
 * singleton with the ApplicationIOScope — the same hand-off as
 * ClockRepositoryImpl.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class DeviceManagerCoordinator(
    private val devices: DeviceRepository,
    private val scanner: DeviceScanner,
    private val clock: ClockRepository,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(DeviceManagerState())
    val state: StateFlow<DeviceManagerState> = _state.asStateFlow()

    /** Whether the Bluetooth adapter is on (delegates to the scanner port). */
    val isBluetoothEnabled: Boolean get() = scanner.isBluetoothEnabled

    private var scanJob: Job? = null

    init {
        scope.launch {
            devices.observeDevices().collect { list ->
                _state.update { it.copy(devices = list) }
            }
        }
        scope.launch {
            clock.deviceState.collect { s ->
                _state.update { it.copy(connectedMac = s.deviceMac.takeIf { s.connection == ConnectionState.CONNECTED }) }
            }
        }
    }

    /**
     * Starts a scan. The scanner emits the accumulated findings (already
     * de-duplicated by MAC — its contract); saved devices are filtered
     * out: they already have their connect entry in "my devices", so the
     * scan section lists **new** devices only. Scanning ends itself when
     * the scan window closes; a second start while running is ignored.
     *
     * A scan that cannot run (Bluetooth off) or fails mid-flight sets
     * [DeviceManagerState.scanFailed] instead of throwing — an uncaught
     * exception in the collector coroutine would kill the process.
     */
    fun startScan() {
        if (_state.value.scanning) return
        if (!scanner.isBluetoothEnabled) {
            _state.update {
                it.copy(
                    scanFailed = true,
                    scanning = false,
                    scanResults = emptyList()
                )
            }
            return
        }
        _state.update { it.copy(scanning = true, scanResults = emptyList(), scanFailed = false) }
        scanJob = scope.launch {
            try {
                scanner.scan().collect { found ->
                    _state.update { s ->
                        s.copy(scanResults = found.filter { f -> f.mac !in s.devices.map { it.mac } })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(scanFailed = true) }
            } finally {
                _state.update { it.copy(scanning = false) }
            }
        }
    }

    /** Stops the scan and drops the results (sheet closed). */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(scanning = false, scanResults = emptyList()) }
    }

    /**
     * Connects to a scan finding, saves it and ends the scan. The device
     * becomes the default when none exists yet. The connect call has
     * "initiated" semantics (see [ClockRepository]); the handshake
     * outcome flows via deviceState.
     */
    fun connectDevice(result: DiscoveredDevice) {
        val device = ClockDevice(
            mac = result.mac,
            alias = result.name,
            advertisedName = result.name,
            isDefault = false,
            lastConnectedAt = System.currentTimeMillis(),
        )
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(scanning = false, scanResults = emptyList()) }
        scope.launch {
            devices.upsert(device)
            devices.setDefaultIfNone(device.mac)
            clock.connect(device)
        }
    }

    /** Marks [mac] the default device (connected automatically at startup). */
    fun setDefault(mac: String) {
        scope.launch { devices.setDefault(mac) }
    }

    /**
     * Connects a saved device directly by MAC (no scan). Refreshes its
     * last-connection timestamp first so the list reorders on success.
     */
    fun connectSaved(device: ClockDevice) {
        scope.launch {
            devices.upsert(device.copy(lastConnectedAt = System.currentTimeMillis()))
            clock.connect(device)
        }
    }

    /** Disconnects the current connection (no reconnection, fire-and-forget). */
    fun disconnect() {
        clock.disconnect()
    }

    /** Renames the device keyed by [mac]. */
    fun rename(mac: String, alias: String) {
        scope.launch { devices.rename(mac, alias) }
    }

    /**
     * Deletes a saved device; if it is the currently connected one, the
     * connection is dropped too (staying linked to a deleted device
     * would be confusing).
     */
    fun delete(mac: String) {
        scope.launch {
            devices.delete(mac)
            if (clock.deviceState.value.deviceMac == mac) {
                clock.disconnect()
            }
        }
    }
}
