package cn.floriax.amber.feature.light.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import android.graphics.Color as AndroidColor

/** degrees(0..359) → byte(0..255), ceil (exact inverse of [toHueDegrees]). */
internal fun Int.hueDegreesToByte(): Int = ((this * 256 + 359) / 360).coerceIn(0, 255)

/**
 * Hue wheel: tap or drag anywhere on the disc to pick a hue; the indicator
 * ring follows the finger and the hue is committed on release.
 *
 * @param hueByte the current hue byte (0..255).
 * @param onHueChangeFinished invoked with degrees (0..359) on release/tap.
 * @param modifier modifier for the wheel.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
internal fun HueWheel(
    hueByte: Int,
    onHueChangeFinished: (degrees: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var stableAngle by remember { mutableIntStateOf(hueByte.toHueDegrees()) }
    var touchAngle by remember { mutableIntStateOf(hueByte.toHueDegrees()) }
    var stableFraction by remember { mutableFloatStateOf(1f) }
    var touchFraction by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(hueByte) {
        if (!dragging) {
            stableAngle = hueByte.toHueDegrees()
            touchAngle = stableAngle
        }
    }
    val indicatorColor = Color.White
    Canvas(
        modifier = modifier
            .size(220.dp)
            .alpha(0.95f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val (angle, fraction) = radialOf(
                        offset,
                        Offset(size.width / 2f, size.height / 2f),
                        maxRadius(minOf(size.width, size.height).toFloat()),
                    )
                    touchAngle = angle
                    touchFraction = fraction
                    stableAngle = angle
                    stableFraction = fraction
                    onHueChangeFinished(angle)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        val (angle, fraction) = radialOf(
                            offset,
                            Offset(size.width / 2f, size.height / 2f),
                            maxRadius(minOf(size.width, size.height).toFloat()),
                        )
                        touchAngle = angle
                        touchFraction = fraction
                    },
                    onDrag = { change, _ ->
                        val (angle, fraction) = radialOf(
                            change.position,
                            Offset(size.width / 2f, size.height / 2f),
                            maxRadius(minOf(size.width, size.height).toFloat()),
                        )
                        touchAngle = angle
                        touchFraction = fraction
                    },
                    onDragEnd = {
                        dragging = false
                        stableAngle = touchAngle
                        stableFraction = touchFraction
                        onHueChangeFinished(touchAngle)
                    },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        val inset = 2.dp.toPx()
        val disc = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        for (deg in 0 until 360 step 3) {
            drawArc(
                color = Color(AndroidColor.HSVToColor(floatArrayOf(deg.toFloat(), 1f, 1f))),
                startAngle = deg.toFloat() - 90f,
                sweepAngle = 3.2f,
                useCenter = true,
                topLeft = topLeft,
                size = disc,
            )
        }
        val a = (if (dragging) touchAngle else stableAngle).toFloat() - 90f
        val maxR = maxRadius(this.size.minDimension)
        val fraction = if (dragging) touchFraction else stableFraction
        val r = maxR * fraction
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val rad = Math.toRadians(a.toDouble())
        val pin = Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat())
        drawCircle(indicatorColor, radius = 10.dp.toPx(), center = pin, style = Stroke(4.dp.toPx()))
    }
}

/** Max radius the indicator ring can reach. */
private fun Density.maxRadius(minDimension: Float): Float =
    minDimension / 2f - 2.dp.toPx() - 14.dp.toPx()

/** Offset → (hue angle with 0° at top, distance fraction [0, 1]). */
private fun radialOf(offset: Offset, center: Offset, maxR: Float): Pair<Int, Float> {
    val angle = ((Math.toDegrees(
        atan2(offset.y - center.y, offset.x - center.x).toDouble()
    ) + 90 + 360) % 360).toInt()
    val fraction = (hypot(offset.x - center.x, offset.y - center.y) / maxR).coerceIn(0f, 1f)
    return angle to fraction
}
