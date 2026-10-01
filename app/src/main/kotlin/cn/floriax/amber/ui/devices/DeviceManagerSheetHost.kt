package cn.floriax.amber.ui.devices

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import cn.floriax.amber.domain.device.DeviceManagerCoordinator
import cn.floriax.amber.domain.device.model.DiscoveredDevice
import cn.floriax.amber.shared.designsystem.component.DeviceManagerSheet
import cn.floriax.amber.shared.designsystem.component.SavedDeviceUi
import cn.floriax.amber.shared.designsystem.component.ScanResultUi

/**
 * Host of the device manager sheet: bridges the shared domain coordinator
 * (state + actions) into the stateless designsystem sheet.
 *
 * Lives in the app assembly layer on purpose: the sheet is opened from the
 * clock/light/settings screens (via their onOpenDevices callback) but
 * must be ONE instance with ONE state — mounting it at the activity level
 * above navigation gives exactly that, without feature-to-feature
 * dependencies or leaking domain types into the shared UI layers.
 *
 * Opening starts a scan (the caller guarantees Bluetooth is on — with it
 * off the pill asks to enable Bluetooth instead of opening this sheet);
 * dismissing stops it. Connecting to a scan finding also dismisses.
 * Bluetooth switching off mid-scan surfaces the system enable dialog
 * once; declining keeps the failed hint + rescan entry.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Composable
fun DeviceManagerSheetHost(
    coordinator: DeviceManagerCoordinator,
    onDismiss: () -> Unit,
) {
    val state by coordinator.state.collectAsState()

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coordinator.startScan()
        }
    }

    fun startScanOrEnable() {
        if (coordinator.isBluetoothEnabled) {
            coordinator.startScan()
        } else {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    LaunchedEffect(Unit) { coordinator.startScan() }

    // Bluetooth switched off mid-scan (scanFailed): offer the enable
    // dialog once. Declining keeps the failed hint + rescan entry.
    LaunchedEffect(state.scanFailed) {
        if (state.scanFailed) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    fun dismiss() {
        coordinator.stopScan()
        onDismiss()
    }

    DeviceManagerSheet(
        devices = state.devices.map {
            SavedDeviceUi(
                alias = it.alias,
                advertisedName = it.advertisedName,
                mac = it.mac,
                isDefault = it.isDefault,
                connected = it.mac == state.connectedMac,
            )
        },
        scanResults = state.scanResults.map {
            ScanResultUi(it.name, it.mac, it.rssi)
        },
        scanning = state.scanning,
        scanFailed = state.scanFailed,
        onDismiss = ::dismiss,
        onConnect = { result ->
            coordinator.connectDevice(DiscoveredDevice(result.name, result.mac, result.rssi))
            dismiss()
        },
        onConnectSaved = { ui ->
            state.devices.firstOrNull { it.mac == ui.mac }?.let(coordinator::connectSaved)
        },
        onDisconnect = coordinator::disconnect,
        onSetDefault = coordinator::setDefault,
        onRename = coordinator::rename,
        onDelete = coordinator::delete,
        onRescan = ::startScanOrEnable,
    )
}
