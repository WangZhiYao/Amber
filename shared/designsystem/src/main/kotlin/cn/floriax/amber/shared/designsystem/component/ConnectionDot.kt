package cn.floriax.amber.shared.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Device connection status dot: online = tertiary, offline = outline.
 *
 * @param online whether the device is currently connected.
 * @param modifier modifier for the dot.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
fun ConnectionDot(
    online: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(
                if (online) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
    )
}
