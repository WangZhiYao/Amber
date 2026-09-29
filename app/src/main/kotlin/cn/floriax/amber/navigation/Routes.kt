package cn.floriax.amber.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import cn.floriax.amber.R
import cn.floriax.amber.feature.clock.ClockRoute
import cn.floriax.amber.feature.light.LightRoute
import cn.floriax.amber.feature.settings.SettingsRoute

/**
 * Bottom navigation tab definition: key, label and selected/unselected icons.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class Tab(
    val key: NavKey,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * App-level navigation composition config: start destination, bottom
 * navigation tabs and the top level route set.
 *
 * Screen content is registered explicitly by each feature's section extension
 * function (lightSection / clockSection / settingsSection) inside
 * MainActivity's entryProvider; feature-internal child routes (e.g. the
 * settings DebugRoute) are owned by their feature. This object only decides
 * which screens are top level, what the bottom bar looks like and which is
 * the start destination.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
object Routes {
    /** Start destination: light. */
    val startRoute: NavKey = LightRoute

    val bottomTabs = listOf(
        Tab(
            key = LightRoute,
            labelRes = R.string.tab_light,
            selectedIcon = Icons.Filled.Lightbulb,
            unselectedIcon = Icons.Outlined.Lightbulb,
        ),
        Tab(
            key = ClockRoute,
            labelRes = R.string.tab_clock,
            selectedIcon = Icons.Filled.Schedule,
            unselectedIcon = Icons.Outlined.Schedule,
        ),
        Tab(
            key = SettingsRoute,
            labelRes = R.string.tab_settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
    )

    val topLevelRoutes: Set<NavKey> = bottomTabs.map { it.key }.toSet()
}
