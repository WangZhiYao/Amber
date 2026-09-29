package cn.floriax.amber.feature.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Settings screen route (top level, composed into the bottom navigation by app).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Serializable
data object SettingsRoute : NavKey

/**
 * Debug log route: an internal child route of the settings stack; other
 * modules must not be aware of it or navigate to it.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Serializable
data object DebugRoute : NavKey
