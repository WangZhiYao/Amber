package cn.floriax.amber.feature.clock

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Clock screen route (top level, composed into the bottom navigation by app).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Serializable
data object ClockRoute : NavKey
