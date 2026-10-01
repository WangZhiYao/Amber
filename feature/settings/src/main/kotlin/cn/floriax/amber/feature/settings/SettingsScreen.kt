package cn.floriax.amber.feature.settings

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.floriax.amber.shared.designsystem.component.AmberTopBar
import cn.floriax.amber.shared.designsystem.component.ConnectionDot
import cn.floriax.amber.shared.ui.ext.collectState

/**
 * Settings screen: device, preference and developer cards.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDebug: () -> Unit,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.collectState()

    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(title = stringResource(R.string.tab_settings))
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
            // Device card: default device row with online status dot.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_device_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    state.defaultDevice?.let { device ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ConnectionDot(online = state.isOnline(device.mac))
                            Text(
                                text = device.alias,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(
                                    R.string.settings_device_default,
                                    device.advertisedName,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    } ?: Text(
                        text = stringResource(R.string.settings_no_device),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    OutlinedButton(
                        onClick = onOpenDevices,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.settings_manage_devices))
                    }
                }
            }

            // Preference card: auto sync switch.
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_pref_card),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.settings_auto_sync),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.autoSync,
                        onCheckedChange = viewModel::setAutoSync,
                    )
                }
            }

            // Developer card: debug panel entry (a full-screen page that does
            // not occupy a bottom navigation slot).
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_dev_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenDebug) {
                        Text(stringResource(R.string.settings_debug_panel))
                    }
                }
            }

            // About card: version info.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_about_card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_version),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
