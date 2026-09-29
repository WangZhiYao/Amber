package cn.floriax.amber.feature.light

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.domain.device.ConnectionState
import cn.floriax.amber.feature.light.components.DigitPreview
import cn.floriax.amber.feature.light.components.HueWheel
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.designsystem.component.ConnectionPill
import cn.floriax.amber.shared.ui.ext.collectState

/**
 * Light screen placeholder: digit tube preview card.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightScreen(modifier: Modifier = Modifier, viewModel: LightViewModel = viewModel()) {
    val state by viewModel.collectState()

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
                        colonBlink = state.colonBlink,
                        selectable = !state.sameColor,
                        selectedGroup = state.selectedGroup,
                        onGroupTap = viewModel::selectGroup,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            // Hue wheel card.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HueWheel(
                        hueByte = state.backlight.hues[if (state.sameColor) 0 else state.selectedGroup],
                        onHueChangeFinished = viewModel::setHueDegrees,
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
