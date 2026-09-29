package cn.floriax.amber.feature.clock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.floriax.amber.shared.designsystem.component.AmberTopBar

/**
 * Clock screen placeholder: top bar only, content left blank, to be filled
 * in later iterations.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AmberTopBar(title = stringResource(R.string.tab_clock))
        },
        // The status bar inset is consumed by the outer MainActivity Scaffold;
        // zero it here to prevent double application.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
