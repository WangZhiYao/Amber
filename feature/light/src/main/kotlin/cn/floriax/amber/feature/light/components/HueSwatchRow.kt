package cn.floriax.amber.feature.light.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Four-group color swatches (preset snapshot): each group's lamp color as a
 * translucent tile with a thin border.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Composable
internal fun HueSwatchRow(
    hues: List<Int>,
    saturation: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        hues.forEach { hue ->
            val lamp = lampColor(hue, saturation, 255)
            Box(
                Modifier
                    .size(36.dp)
                    .background(lamp.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .border(1.dp, lamp.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            )
        }
    }
}
