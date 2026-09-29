package cn.floriax.amber.shared.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Navigation event sink owned by the app module and handed to the feature
 * section functions at composition time.
 *
 * Features depend only on this interface; the app decides how navigation
 * events mutate the back stacks.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
interface Navigator {

    /**
     * Navigate to [route]: a top level route switches the active back stack,
     * any other route is pushed onto the current stack.
     */
    fun navigate(route: NavKey)

    /**
     * Go back from the current route.
     */
    fun goBack()
}
