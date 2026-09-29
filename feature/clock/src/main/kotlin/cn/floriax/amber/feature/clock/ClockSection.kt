package cn.floriax.amber.feature.clock

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers the clock feature's navigation entries.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
fun EntryProviderScope<NavKey>.clockSection() {
    entry<ClockRoute> { ClockScreen() }
}
