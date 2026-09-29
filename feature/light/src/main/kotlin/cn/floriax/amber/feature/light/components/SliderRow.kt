package cn.floriax.amber.feature.light.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.floriax.amber.feature.light.R

/**
 * Labeled 0..255 slider with a live percentage readout on the trailing end.
 * [onValueChange] reports the value continuously while dragging (live
 * preview); [onFinish] reports it once more when the thumb is released
 * (the commit point for future device writes).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
internal fun SliderRow(
    label: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    onFinish: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var local by remember(value) { mutableIntStateOf(value) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Fixed label/readout widths keep the slider tracks of stacked rows aligned.
        Text(
            label,
            modifier = Modifier.width(52.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = local.toFloat(),
            onValueChange = { v ->
                local = v.toInt()
                onValueChange(local)
            },
            onValueChangeFinished = { onFinish(local) },
            valueRange = 0f..255f,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(
            stringResource(R.string.light_percent, local * 100 / 255),
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
