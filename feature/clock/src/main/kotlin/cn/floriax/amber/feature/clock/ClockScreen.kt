package cn.floriax.amber.feature.clock

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.designsystem.component.ConnectionPill
import cn.floriax.amber.shared.ui.ext.collectSideEffect
import cn.floriax.amber.shared.ui.ext.collectState
import cn.floriax.amber.shared.ui.permission.hasBlePermissions
import cn.floriax.amber.shared.ui.permission.requiredBlePermissions
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Clock screen placeholder: time sync card.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClockViewModel = viewModel(),
) {
    val state by viewModel.collectState()
    val connected = state.connection == ConnectionState.CONNECTED
    val context = LocalContext.current
    val resources = LocalResources.current

    // Permissions are a view-layer concern: ask here, then hand the intent to
    // the ViewModel (which owns scanning and connecting).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            viewModel.onRetryConnect()
        } else {
            viewModel.onPermissionRequired()
        }
    }

    // Bluetooth-off guidance: the ViewModel reports the state, the screen
    // shows the system enable dialog and retries once the user accepts.
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onRetryConnect()
        } else {
            // Declining the system dialog is a presentation-level outcome of a
            // dialog this screen owns, so the feedback stays here.
            Toast.makeText(
                context,
                resources.getString(R.string.bluetooth_required_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            ClockSideEffect.NotConnected -> Toast.makeText(
                context,
                resources.getString(R.string.common_not_connected),
                Toast.LENGTH_SHORT,
            ).show()

            ClockSideEffect.Synced -> Toast.makeText(
                context,
                resources.getString(R.string.clock_synced_toast),
                Toast.LENGTH_SHORT,
            ).show()

            is ClockSideEffect.WriteFailed -> Toast.makeText(
                context,
                resources.getString(R.string.common_write_failed_toast, effect.message),
                Toast.LENGTH_SHORT,
            ).show()

            ClockSideEffect.NoDeviceFound -> Toast.makeText(
                context,
                resources.getString(R.string.connect_no_device_toast),
                Toast.LENGTH_SHORT,
            ).show()

            ClockSideEffect.PermissionRequired -> Toast.makeText(
                context,
                resources.getString(R.string.permission_required_toast),
                Toast.LENGTH_SHORT,
            ).show()

            ClockSideEffect.BluetoothOff -> enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(
                title = stringResource(R.string.tab_clock),
                actions = {
                    ConnectionPill(
                        label = if (state.scanning) {
                            stringResource(R.string.connection_scanning)
                        } else {
                            connectionLabel(state.connection, state.deviceName)
                        },
                        dotColor = connectionDotColor(state.connection),
                        // Disconnected (including failed reconnection): the pill is
                        // the manual retry entry; otherwise it opens device management.
                        onClick = {
                            if (state.connection != ConnectionState.DISCONNECTED) {
                                onOpenDevices()
                            } else if (context.hasBlePermissions()) {
                                viewModel.onRetryConnect()
                            } else {
                                permissionLauncher.launch(requiredBlePermissions())
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
        // Status bar inset is consumed by the outer Scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Time sync card.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.clock_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.clock_sync_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    state.lastSyncAt?.let {
                        Text(
                            text = stringResource(
                                R.string.clock_last_sync,
                                stringResource(R.string.clock_today, it.formatSyncTime()),
                            ),
                        )
                    }
                    Button(
                        onClick = viewModel::syncTime,
                        enabled = connected && !state.syncing,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.syncing) {
                                    R.string.clock_syncing
                                } else {
                                    R.string.clock_sync_now
                                },
                            ),
                        )
                    }
                }
            }

            // Power timer card.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.clock_timer_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TimerRow(
                        label = stringResource(R.string.clock_power_on),
                        time = state.timers.powerOn,
                        enabled = state.switches.powerOnEnabled,
                        connected = connected,
                        onTimeChange = { viewModel.setTimer(TimerKind.POWER_ON, it) },
                        onEnabledChange = { viewModel.setSwitch(SwitchField.POWER_ON, it) },
                    )
                    TimerRow(
                        label = stringResource(R.string.clock_power_off),
                        time = state.timers.powerOff,
                        enabled = state.switches.powerOffEnabled,
                        connected = connected,
                        onTimeChange = { viewModel.setTimer(TimerKind.POWER_OFF, it) },
                        onEnabledChange = { viewModel.setSwitch(SwitchField.POWER_OFF, it) },
                    )
                }
            }

            // Alarm card.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.clock_alarm_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TimerRow(
                        label = stringResource(R.string.clock_alarm_card),
                        time = state.timers.alarm,
                        enabled = state.switches.alarmEnabled,
                        connected = connected,
                        onTimeChange = { viewModel.setTimer(TimerKind.ALARM, it) },
                        onEnabledChange = { viewModel.setSwitch(SwitchField.ALARM, it) },
                    )
                }
            }

            // Display card: hour format, colon blink, mute, remote lock.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.clock_display_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.clock_hour_format))
                        Spacer(modifier = Modifier.weight(1f))
                        FilterChip(
                            selected = !state.switches.hour12,
                            onClick = { viewModel.setHourFormat(false) },
                            label = { Text(stringResource(R.string.clock_24_hour)) },
                            enabled = connected,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = state.switches.hour12,
                            onClick = { viewModel.setHourFormat(true) },
                            label = { Text(stringResource(R.string.clock_12_hour)) },
                            enabled = connected,
                        )
                    }
                    SwitchRow(
                        label = stringResource(R.string.clock_colon_blink),
                        checked = state.switches.colonBlink,
                        connected = connected,
                        onChange = { viewModel.setSwitch(SwitchField.COLON_BLINK, it) },
                    )
                    SwitchRow(
                        label = stringResource(R.string.clock_mute),
                        checked = state.switches.mute,
                        connected = connected,
                        onChange = { viewModel.setSwitch(SwitchField.MUTE, it) },
                    )
                    SwitchRow(
                        label = stringResource(R.string.clock_lock_remote),
                        checked = state.switches.lockRemote,
                        connected = connected,
                        onChange = { viewModel.setSwitch(SwitchField.LOCK_REMOTE, it) },
                    )
                    Text(
                        text = stringResource(R.string.clock_lock_remote_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

/** Timer row: label, HH:MM picker button and enable switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerRow(
    label: String,
    time: LocalTime,
    enabled: Boolean,
    connected: Boolean,
    onTimeChange: (LocalTime) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { showPicker = true }, enabled = connected) {
            Text(stringResource(R.string.clock_time_hh_mm, time.hour, time.minute))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = connected,
        )
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Switch row: label, value switch, both gated by connection. */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    connected: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = connected)
    }
}

/** Formats epoch millis as HH:mm:ss in the system time zone. */
private fun Long.formatSyncTime(): String =
    DateTimeFormatter.ofPattern("HH:mm:ss")
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** Maps the connection state onto the pill label (device name when connected). */
@Composable
private fun connectionLabel(connection: ConnectionState, deviceName: String): String =
    when (connection) {
        ConnectionState.CONNECTED ->
            deviceName.ifBlank { stringResource(R.string.connection_connected) }

        ConnectionState.CONNECTING -> stringResource(R.string.connection_connecting)
        ConnectionState.RECONNECTING -> stringResource(R.string.connection_reconnecting)
        ConnectionState.DISCONNECTED -> stringResource(R.string.connection_disconnected_retry)
    }

/** Maps the connection state onto the pill dot color: connected = tertiary, else outline. */
@Composable
private fun connectionDotColor(connection: ConnectionState): Color =
    if (connection == ConnectionState.CONNECTED) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }
