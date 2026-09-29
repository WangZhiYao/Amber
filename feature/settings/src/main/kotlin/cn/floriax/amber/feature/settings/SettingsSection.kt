package cn.floriax.amber.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import cn.floriax.amber.feature.settings.debug.DebugScreen
import cn.floriax.amber.shared.ui.navigation.Navigator

/**
 * Registers the settings feature's navigation entries, including the
 * feature-internal debug log page.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
fun EntryProviderScope<NavKey>.settingsSection(navigator: Navigator) {
    entry<SettingsRoute> {
        SettingsScreen(onOpenDebug = { navigator.navigate(DebugRoute) })
    }
    entry<DebugRoute> {
        DebugScreen(onBack = { navigator.goBack() })
    }
}
