package cn.floriax.amber.feature.light

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Registers the light feature's navigation entries. Connection actions
 * (permissions, scanning) are handled by the screen and its ViewModel; the
 * app only supplies navigation.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
fun EntryProviderScope<NavKey>.lightSection(
    onOpenDevices: () -> Unit,
) {
    entry<LightRoute> {
        LightScreen(onOpenDevices = onOpenDevices)
    }
}
