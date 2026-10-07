package cn.floriax.amber.shared.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.floriax.amber.shared.designsystem.R

/**
 * One saved-device row (pure UI model — the host maps the domain device).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class SavedDeviceUi(
    val alias: String,
    val advertisedName: String,
    val mac: String,
    val isDefault: Boolean,
    val connected: Boolean,
)

/**
 * One scan finding (pure UI model).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class ScanResultUi(
    val name: String,
    val mac: String,
    val rssi: Int,
)

/**
 * Snapshot of everything the sheet renders, grouped to keep the composable
 * signature small.
 *
 * @author WangZhiYao
 * @since 2026/10/7
 */
data class DeviceManagerSheetUiState(
    val devices: List<SavedDeviceUi> = emptyList(),
    val scanResults: List<ScanResultUi> = emptyList(),
    val scanning: Boolean = false,
    val scanFailed: Boolean = false,
)

/**
 * Sheet callbacks (connect / manage / rescan), grouped the same way.
 *
 * @author WangZhiYao
 * @since 2026/10/7
 */
class DeviceManagerSheetActions(
    val onConnect: (ScanResultUi) -> Unit,
    val onConnectSaved: (SavedDeviceUi) -> Unit,
    val onDisconnect: () -> Unit,
    val onSetDefault: (String) -> Unit,
    val onRename: (String, String) -> Unit,
    val onDelete: (String) -> Unit,
    val onRescan: () -> Unit,
)

/**
 * Device manager bottom sheet: saved devices (set default / rename /
 * delete) plus the live scan (connect). Ported control-by-control from
 * the prototype's DeviceSheet; stateless — the host in :shared/ui feeds
 * it from the shared coordinator.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerSheet(
    state: DeviceManagerSheetUiState,
    onDismiss: () -> Unit,
    actions: DeviceManagerSheetActions,
) {
    var renameTarget by remember { mutableStateOf<SavedDeviceUi?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.device_my_devices),
                color = MaterialTheme.colorScheme.primary,
            )
            state.devices.forEach { device ->
                SavedDeviceRow(
                    device = device,
                    onConnect = { actions.onConnectSaved(device) },
                    onDisconnect = actions.onDisconnect,
                    onSetDefault = { actions.onSetDefault(device.mac) },
                    onRename = { renameTarget = device },
                    onDelete = { actions.onDelete(device.mac) },
                )
            }

            Text(
                stringResource(R.string.device_scan_new),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
            state.scanResults.forEach { result ->
                ScanResultRow(result = result, onConnect = { actions.onConnect(result) })
            }
            if (state.scanning) {
                Text(
                    stringResource(R.string.device_scanning),
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                if (state.scanFailed) {
                    Text(
                        stringResource(R.string.device_scan_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                TextButton(onClick = actions.onRescan) {
                    Text(stringResource(R.string.device_rescan))
                }
            }
        }
    }

    renameTarget?.let { device ->
        var name by remember(device.mac) { mutableStateOf(device.alias) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.device_rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRename(device.mac, name)
                    renameTarget = null
                }) { Text(stringResource(R.string.device_rename_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.device_cancel))
                }
            },
        )
    }
}

/** Saved device row: alias + name, then connect/disconnect, set-default, rename, delete. */
@Composable
private fun SavedDeviceRow(
    device: SavedDeviceUi,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSetDefault: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(device.alias)
            Text(
                if (device.isDefault) {
                    stringResource(R.string.device_default, device.advertisedName)
                } else {
                    device.advertisedName
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        // The connected device offers disconnect; a saved one offers a
        // direct connect (by MAC, no scan needed).
        if (device.connected) {
            TextButton(onClick = onDisconnect) {
                Text(stringResource(R.string.device_disconnect))
            }
        } else {
            TextButton(onClick = onConnect) {
                Text(stringResource(R.string.device_connect))
            }
        }
        if (!device.isDefault) {
            TextButton(onClick = onSetDefault) {
                Text(stringResource(R.string.device_set_default))
            }
        }
        TextButton(onClick = onRename) { Text(stringResource(R.string.device_rename)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.device_delete)) }
    }
}

/** Scan finding row: name (MAC) + RSSI, connect button. */
@Composable
private fun ScanResultRow(result: ScanResultUi, onConnect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${result.name} (${result.mac})")
            Text(
                stringResource(R.string.device_rssi, result.rssi),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Button(onClick = onConnect) { Text(stringResource(R.string.device_connect)) }
    }
}
