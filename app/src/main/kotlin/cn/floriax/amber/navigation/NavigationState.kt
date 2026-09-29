package cn.floriax.amber.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Create a navigation state that persists config changes and process death.
 *
 * @param startRoute - The top level route to start on. This should also be in [topLevelRoutes].
 * @param topLevelRoutes - The top level routes in the app.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    // Create a back stack for each top level route.
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation state. This class does not modify its own state. It is designed
 * to be modified using the [Navigator] class.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the state object that backs the top level route.
 * @param backStacks - the back stacks for each top level route.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** The current top level route. */
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * The key at the top of the current top level route's back stack.
     */
    val currentKey: NavKey?
        get() = backStacks[topLevelRoute]?.lastOrNull()

    /**
     * Convert the navigation state into [NavEntry]s that have been decorated with a
     * `SaveableStateHolder`.
     *
     * @param entryProvider - the entry provider used to convert the keys in the
     * back stacks to [NavEntry]s.
     */
    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> {
        // For each back stack, create a `SaveableStateHolder` decorator and use it to decorate
        // the entries from that stack. When backStacks changes, `rememberDecoratedNavEntries` will
        // be recomposed and a new list of decorated entries is returned.
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        // Only return the entries for the stacks that are currently in use.
        return topLevelRoutesInUse()
            .flatMap { decoratedEntries[it] ?: emptyList() }
    }

    /**
     * Get the top level routes that are currently in use. The start route is always the first
     * route in the list. This means the user will always exit the app through the starting
     * route ("exit through home" pattern). The list will contain a maximum of one other route.
     * Note that even if a top level route is not in use its state is still retained.
     */
    private fun topLevelRoutesInUse(): List<NavKey> =
        if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}
