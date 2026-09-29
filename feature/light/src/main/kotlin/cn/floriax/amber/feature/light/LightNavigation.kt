package cn.floriax.amber.feature.light

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Light screen route (top level, composed into the bottom navigation by app).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Serializable
data object LightRoute : NavKey
