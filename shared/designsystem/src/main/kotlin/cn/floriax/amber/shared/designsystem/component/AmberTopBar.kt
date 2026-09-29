package cn.floriax.amber.shared.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import cn.floriax.amber.shared.designsystem.icon.AmberIcons

/**
 * Amber top bar: the TopAppBar shared by every screen (top-level tabs and
 * full-screen child pages).
 *
 * Screens use this inside their own Scaffold(topBar = ...) together with
 * `contentWindowInsets = WindowInsets(0, 0, 0, 0)`: the status bar inset is
 * consumed by the outer MainActivity Scaffold, the top bar height is provided
 * by the inner Scaffold's innerPadding, so every inset is applied exactly once.
 *
 * @param title the top bar title.
 * @param modifier modifier for the top bar.
 * @param onBack back callback; null hides the back arrow (top-level tab
 *   screens), non-null shows it (full-screen child screens, e.g. the debug
 *   log page).
 * @param backIcon the back arrow icon, defaults to [AmberIcons.ArrowBack].
 * @param backContentDescription accessibility description of the back arrow.
 * @param actions the action area on the right side of the top bar.
 * @param scrollBehavior scroll behavior (pinned / enterAlways), created by the
 *   screen as needed and also attached to the inner Scaffold's nestedScroll.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmberTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = AmberIcons.ArrowBack,
    backContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = if (onBack != null) {
            {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = backIcon,
                        contentDescription = backContentDescription,
                    )
                }
            }
        } else {
            {}
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        // The status bar inset is consumed by the outer Scaffold; zero it here
        // to prevent double application.
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}
