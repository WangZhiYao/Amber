package cn.floriax.amber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import cn.floriax.amber.feature.clock.clockSection
import cn.floriax.amber.feature.light.lightSection
import cn.floriax.amber.feature.settings.SettingsRoute
import cn.floriax.amber.feature.settings.settingsSection
import cn.floriax.amber.navigation.Routes
import cn.floriax.amber.navigation.StateNavigator
import cn.floriax.amber.navigation.rememberNavigationState
import cn.floriax.amber.shared.designsystem.theme.AmberTheme
import dagger.hilt.android.AndroidEntryPoint

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmberTheme {
                val navigationState = rememberNavigationState(
                    startRoute = Routes.startRoute,
                    topLevelRoutes = Routes.topLevelRoutes,
                )
                val navigator = remember(navigationState) { StateNavigator(navigationState) }

                val entryProvider = entryProvider {
                    lightSection(onOpenDevices = { navigator.navigate(SettingsRoute) })
                    clockSection(onOpenDevices = { navigator.navigate(SettingsRoute) })
                    settingsSection(navigator)
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
                }
            }
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
