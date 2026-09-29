package cn.floriax.amber.navigation

import androidx.navigation3.runtime.NavKey
import cn.floriax.amber.shared.ui.navigation.Navigator

/**
 * App-owned [Navigator] implementation: translates navigation events into
 * mutations of the composition-held [NavigationState] (per-top-level-route
 * back stacks).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
class StateNavigator(private val state: NavigationState) : Navigator {

    override fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    override fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
