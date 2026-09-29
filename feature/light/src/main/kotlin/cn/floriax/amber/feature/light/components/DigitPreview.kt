package cn.floriax.amber.feature.light.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import android.graphics.Color as AndroidColor

/** Lamp preview color: H(byte) + S(byte) + V(brightness byte). */
internal fun lampColor(hueByte: Int, saturation: Int, brightness: Int): Color = Color(
    AndroidColor.HSVToColor(
        floatArrayOf(
            hueByte.toHueDegrees().toFloat(),
            saturation / 255f,
            brightness.coerceIn(0, 255) / 255f,
        ),
    ),
)

/** byte(0..255) → degrees(0..358). */
internal fun Int.toHueDegrees(): Int = this * 360 / 256

/** IN-12 tube aspect ratio (height ≈ 1.8 × width). */
private val TubeAspectRatio = 21f / 31f

/** Fixed digit glow color (gas discharge orange). */
private val NixieGlow = Color(0xFFFFA652)

/** Backlight veil opacity over the tube base. */
private const val BacklightVeilAlpha = 0.16f

/**
 * Digit tube preview: HH:MM, four tubes, blinking colon. Tubes are
 * selectable when [selectable]; the selected tube gets a primary border.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@Composable
internal fun DigitPreview(
    hues: List<Int>,
    saturations: List<Int>,
    brightness: Int,
    colonBlink: Boolean,
    selectable: Boolean,
    selectedGroup: Int,
    onGroupTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var time by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            delay(1_000.milliseconds)
        }
    }
    val blink by rememberInfiniteTransition(label = "colon").animateFloat(
        initialValue = 1f,
        targetValue = if (colonBlink) 0.15f else 1f,
        animationSpec = infiniteRepeatable(tween(1000)),
        label = "blink",
    )
    val digits = listOf(
        time.hour / 10, time.hour % 10, time.minute / 10, time.minute % 10,
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        digits.forEachIndexed { i, d ->
            val lamp = lampColor(hues[i], saturations[i], brightness)
            val selected = selectable && i == selectedGroup
            val tubeShape = RoundedCornerShape(48)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(TubeAspectRatio)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tubeShape)
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                tubeShape
                            )
                        },
                    )
                    .clip(tubeShape)
                    .background(lamp.copy(alpha = BacklightVeilAlpha))
                    .clickable(enabled = selectable) { onGroupTap(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$d",
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NixieGlow,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(color = NixieGlow, blurRadius = 16f),
                    ),
                )
            }
            if (i == 1) {
                // Two-dot colon, matching the digit glow color.
                val dotColor = NixieGlow.copy(alpha = blink)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        }
    }
}
