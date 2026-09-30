package cn.floriax.amber.feature.light

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.feature.light.components.DigitPreview
import cn.floriax.amber.feature.light.components.HueWheel
import cn.floriax.amber.feature.light.components.SliderRow
import cn.floriax.amber.feature.light.components.toHueDegrees
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.designsystem.component.ConnectionPill
import cn.floriax.amber.shared.ui.ext.collectState

/**
 * Light screen placeholder: digit tube preview card.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LightScreen(modifier: Modifier = Modifier, viewModel: LightViewModel = viewModel()) {
    val state by viewModel.collectState()
    val connected = state.connection == ConnectionState.CONNECTED
    val context = LocalContext.current

    // Hue sheet: non-null tapped group = open. Any tube opens the sheet when
    // the mode supports custom colors; otherwise a toast explains why not.
    var hueSheetGroup by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(
                title = stringResource(R.string.tab_light),
                actions = {
                    ConnectionPill(
                        label = connectionLabel(state.connection, state.deviceName),
                        dotColor = connectionDotColor(state.connection),
                        onClick = { /* TODO: retry / open device management */ },
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
                        onValueChange = viewModel::setBrightness,
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
                    // Contrast targets the same group(s) as the wheel above.
                    SliderRow(
                        label = stringResource(R.string.light_contrast),
                        value = state.backlight.saturations[hueIndex],
                        enabled = true,
                        onValueChange = viewModel::setSaturation,
                        onFinish = viewModel::setSaturation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
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
