package cn.floriax.amber.feature.light

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers the light feature's navigation entries.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
fun EntryProviderScope<NavKey>.lightSection() {
    entry<LightRoute> { LightScreen() }
}
