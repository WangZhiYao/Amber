package cn.floriax.amber.feature.light

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.feature.light.components.DigitPreview
import cn.floriax.amber.feature.light.components.HueSwatchRow
import cn.floriax.amber.feature.light.components.HueWheel
import cn.floriax.amber.feature.light.components.SliderRow
import cn.floriax.amber.feature.light.components.toHueDegrees
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.designsystem.component.ConnectionPill
import cn.floriax.amber.shared.ui.ext.collectSideEffect
import cn.floriax.amber.shared.ui.ext.collectState
import cn.floriax.amber.shared.ui.permission.hasBlePermissions
import cn.floriax.amber.shared.ui.permission.requiredBlePermissions

/**
 * Light screen placeholder: digit tube preview card.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun LightScreen(
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightViewModel = viewModel(),
) {
    val state by viewModel.collectState()
    val connected = state.connection == ConnectionState.CONNECTED
    val context = LocalContext.current

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
                context.getString(R.string.bluetooth_required_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            LightSideEffect.NotConnected -> Toast.makeText(
                context,
                context.getString(R.string.common_not_connected),
                Toast.LENGTH_SHORT,
            ).show()

            is LightSideEffect.WriteFailed -> Toast.makeText(
                context,
                context.getString(R.string.common_write_failed_toast, effect.message),
                Toast.LENGTH_SHORT,
            ).show()

            LightSideEffect.NoDeviceFound -> Toast.makeText(
                context,
                context.getString(R.string.connect_no_device_toast),
                Toast.LENGTH_SHORT,
            ).show()

            LightSideEffect.PermissionRequired -> Toast.makeText(
                context,
                context.getString(R.string.permission_required_toast),
                Toast.LENGTH_SHORT,
            ).show()

            LightSideEffect.BluetoothOff -> enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
            )
        }
    }

    // Hue sheet: non-null tapped group = open. Any tube opens the sheet when
    // the mode supports custom colors; otherwise a toast explains why not.
    var hueSheetGroup by remember { mutableStateOf<Int?>(null) }

    // Default preset name: mode label + color name of the edited group's hue.
    val presetDefaultName = stringResource(
        R.string.preset_default_name,
        stringResource(state.backlight.mode.labelRes),
        stringResource(
            hueNameRes(
                state.backlight.hues[if (state.sameColor) 0 else state.selectedGroup].toHueDegrees(),
            ),
        ),
    )
    val presetFallbackName = stringResource(R.string.preset_fallback_name)

    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(
                title = stringResource(R.string.tab_light),
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
            // Digit tube preview card.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.light_digits_preview),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.light_sync),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Switch(
                            checked = state.sameColor,
                            onCheckedChange = viewModel::setSameColor,
                        )
                    }
                    DigitPreview(
                        hues = state.backlight.hues,
                        saturations = state.backlight.saturations,
                        brightness = state.backlight.brightness,
                        colonBlink = state.colonBlink,
                        onGroupTap = { group ->
                            if (state.backlight.mode.supportsCustomColor) {
                                if (!state.sameColor) viewModel.selectGroup(group)
                                hueSheetGroup = group
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.light_mode_no_custom),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // Parameter card: brightness slider and mode chips (global).
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SliderRow(
                        label = stringResource(R.string.light_brightness),
                        value = state.backlight.brightness,
                        enabled = connected,
                        // Drag = local preview only; the LED frame goes out on release.
                        onValueChange = viewModel::previewBrightness,
                        onFinish = viewModel::setBrightness,
                    )
                    Text(
                        text = stringResource(R.string.light_mode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BACKLIGHT_MODE_UI_ORDER.forEach { mode ->
                            FilterChip(
                                selected = state.backlight.mode == mode,
                                onClick = { viewModel.setMode(mode) },
                                label = { Text(stringResource(mode.labelRes)) },
                                enabled = connected,
                            )
                        }
                    }
                }
            }

            // Preset card: save button + horizontal preset chips.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.light_presets),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        FilledIconButton(
                            onClick = {
                                viewModel.onSaveOpen(
                                    presetDefaultName,
                                    presetFallbackName
                                )
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.light_save_preset),
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.presets.forEach { preset ->
                            PresetChip(
                                name = preset.name,
                                enabled = connected,
                                onClick = { if (connected) viewModel.onPresetClick(preset) },
                                onLongClick = { viewModel.onPresetLongPress(preset) },
                            )
                        }
                    }
                }
            }
        }

        // Color editor sheet for the tapped group (or all four at once in
        // unified mode): hue wheel + contrast slider. Hue commits on wheel
        // release; dismiss by dragging down.
        hueSheetGroup?.let { group ->
            ModalBottomSheet(onDismissRequest = { hueSheetGroup = null }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hueIndex = if (state.sameColor) 0 else group
                    HueWheel(
                        hueByte = state.backlight.hues[hueIndex],
                        enabled = true,
                        onHueChangeFinished = viewModel::setHueDegrees,
                    )
                    Text(
                        text = stringResource(
                            R.string.light_hue,
                            state.backlight.hues[hueIndex].toHueDegrees(),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Contrast targets the same group(s) as the wheel above;
                    // drag previews locally, the frame goes out on release.
                    SliderRow(
                        label = stringResource(R.string.light_contrast),
                        value = state.backlight.saturations[hueIndex],
                        enabled = true,
                        onValueChange = viewModel::previewSaturation,
                        onFinish = viewModel::setSaturation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Apply-confirmation dialog.
        state.applyConfirmPreset?.let { preset ->
            AlertDialog(
                onDismissRequest = viewModel::onApplyDismiss,
                title = { Text(stringResource(R.string.light_apply_preset)) },
                text = { Text(stringResource(R.string.light_apply_confirm, preset.name)) },
                confirmButton = {
                    TextButton(onClick = viewModel::onApplyConfirm) {
                        Text(stringResource(R.string.common_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onApplyDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }

        // Save-preset sheet: config snapshot + name input.
        if (state.showSaveSheet) {
            ModalBottomSheet(onDismissRequest = viewModel::onSaveDismiss) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.light_save_preset),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    // Config snapshot: four swatches + parameter summary.
                    HueSwatchRow(
                        hues = state.backlight.hues,
                        saturation = state.backlight.saturations[0],
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = if (state.sameColor) {
                            stringResource(
                                R.string.light_save_summary,
                                state.backlight.hues[0].toHueDegrees(),
                                state.backlight.saturations[0],
                                state.backlight.brightness,
                                stringResource(state.backlight.mode.labelRes),
                            )
                        } else {
                            stringResource(
                                R.string.light_save_summary_multi,
                                state.backlight.saturations[0],
                                state.backlight.brightness,
                                stringResource(state.backlight.mode.labelRes),
                            )
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = state.saveName,
                        onValueChange = viewModel::onSaveNameChange,
                        label = { Text(stringResource(R.string.common_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = viewModel::onSaveDismiss) {
                            Text(stringResource(R.string.common_cancel))
                        }
                        TextButton(onClick = viewModel::onSaveConfirm) {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            }
        }

        // Long-press manage menu: rename + delete.
        state.managePreset?.let { preset ->
            var renameText by remember(preset.id) { mutableStateOf(preset.name) }
            ModalBottomSheet(onDismissRequest = viewModel::onMenuDismiss) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.common_rename)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { viewModel.onRenamePreset(renameText) }) {
                            Text(stringResource(R.string.light_save_name))
                        }
                        Button(
                            onClick = viewModel::onDeletePreset,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(stringResource(R.string.light_delete_preset, preset.name))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preset chip: apply on tap (with the confirmation dialog), manage on
 * long-press. Drawn by hand instead of AssistChip — a chip's internal
 * clickable consumes the gesture, so an outer combinedClickable never
 * fires (tap AND long-press both went dead with AssistChip).
 *
 * Long-press stays active while disconnected: rename/delete are local
 * database operations; only applying needs the device.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Maps a hue angle (degrees) onto a color-name resource. */
@StringRes
private fun hueNameRes(deg: Int): Int {
    val d = ((deg % 360) + 360) % 360
    return when (d) {
        in 0..14, in 346..359 -> R.string.hue_red
        in 15..45 -> R.string.hue_orange
        in 46..70 -> R.string.hue_yellow
        in 71..160 -> R.string.hue_green
        in 161..200 -> R.string.hue_cyan
        in 201..255 -> R.string.hue_blue
        in 256..290 -> R.string.hue_purple
        else -> R.string.hue_pink
    }
}

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
