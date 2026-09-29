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
 * Debug log route: internal to the settings stack.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Serializable
data object DebugRoute : NavKey
