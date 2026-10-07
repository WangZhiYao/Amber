package cn.floriax.amber

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import cn.floriax.amber.domain.device.DeviceManagerCoordinator
import cn.floriax.amber.domain.device.usecase.AutoConnectUseCase
import cn.floriax.amber.feature.clock.clockSection
import cn.floriax.amber.feature.light.lightSection
import cn.floriax.amber.feature.settings.settingsSection
import cn.floriax.amber.navigation.Routes
import cn.floriax.amber.navigation.StateNavigator
import cn.floriax.amber.navigation.rememberNavigationState
import cn.floriax.amber.shared.designsystem.theme.AmberTheme
import cn.floriax.amber.shared.ui.permission.hasBlePermissions
import cn.floriax.amber.ui.devices.DeviceManagerSheetHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root entry point [ComponentActivity], hosting the Compose root layout and
 * the [AmberTheme].
 *
 * Each bottom bar tab owns a real back stack; tab state survives tab
 * switches, config changes and process death. The debug log page (internal
 * to the settings feature) hides the bottom bar while open.
 *
 * The activity only assembles navigation: screens own their state machines,
 * and connection actions (permissions, scanning, connecting) live in the
 * feature ViewModels, backed by the domain use cases.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Shared device manager: the sheet is mounted above navigation. */
    @Inject
    lateinit var deviceManager: DeviceManagerCoordinator

    /** Connects the default device on launch. */
    @Inject
    lateinit var autoConnect: AutoConnectUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Startup auto-connect: silent when there is no default device,
        // Bluetooth is off or permissions are missing (Android 12+) — the
        // manual pill entry takes over in those cases.
        if (hasBlePermissions()) {
            lifecycleScope.launch { autoConnect() }
        }
        setContent {
            AmberTheme {
                AmberApp(
                    deviceManager = deviceManager,
                    onBluetoothEnabled = { lifecycleScope.launch { autoConnect() } },
                )
            }
        }
    }
}

/**
 * Root layout below the theme: navigation, bottom bar and the shared device
 * manager sheet. [onBluetoothEnabled] reconnects after the user accepts the
 * system Bluetooth-enable dialog.
 *
 * @author WangZhiYao
 * @since 2026/10/7
 */
@Composable
private fun AmberApp(
    deviceManager: DeviceManagerCoordinator,
    onBluetoothEnabled: () -> Unit,
) {
    val navigationState = rememberNavigationState(
        startRoute = Routes.startRoute,
        topLevelRoutes = Routes.topLevelRoutes,
    )
    val navigator = remember(navigationState) { StateNavigator(navigationState) }
    var showDeviceManager by remember { mutableStateOf(false) }

    // Bluetooth off → the device manager is useless (no scan,
    // no connect); ask to enable instead of stacking the sheet
    // and the system dialog. Accepting reconnects directly.
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onBluetoothEnabled()
        }
    }
    val openDevices = {
        if (deviceManager.isBluetoothEnabled) {
            showDeviceManager = true
        } else {
            enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        }
    }

    val entryProvider = entryProvider {
        lightSection(onOpenDevices = openDevices)
        clockSection(onOpenDevices = openDevices)
        settingsSection(navigator, onOpenDevices = openDevices)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Hide the bottom bar on non-top-level routes.
            if (Routes.bottomTabs.any { it.key == navigationState.currentKey }) {
                AmberBottomBar(
                    selectedKey = navigationState.topLevelRoute,
                    onTabSelect = { navigator.navigate(it) },
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            entries = navigationState.toDecoratedEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.padding(innerPadding),
        )

        // Device manager sheet: opened from any tab's connection
        // pill (and the settings entry), one shared instance.
        if (showDeviceManager) {
            DeviceManagerSheetHost(
                coordinator = deviceManager,
                onDismiss = { showDeviceManager = false },
            )
        }
    }
}

/**
 * Bottom navigation bar for the top-level tabs.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
private fun AmberBottomBar(
    selectedKey: NavKey,
    onTabSelect: (NavKey) -> Unit,
) {
    NavigationBar {
        Routes.bottomTabs.forEach { tab ->
            val selected = tab.key == selectedKey
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelect(tab.key) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes),
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
