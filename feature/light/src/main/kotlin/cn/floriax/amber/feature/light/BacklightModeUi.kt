package cn.floriax.amber.feature.light

import androidx.annotation.StringRes
import cn.floriax.amber.domain.light.BacklightMode

/**
 * UI projection of [BacklightMode]: display order and localized labels.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
val BACKLIGHT_MODE_UI_ORDER: List<BacklightMode> = listOf(
    BacklightMode.STATIC,
    BacklightMode.BREATH,
    BacklightMode.SPECTRUM,
    BacklightMode.MARQUEE,
    BacklightMode.RAINBOW,
)

@get:StringRes
val BacklightMode.labelRes: Int
    get() = when (this) {
        BacklightMode.STATIC -> R.string.backlight_mode_static
        BacklightMode.BREATH -> R.string.backlight_mode_breath
        BacklightMode.SPECTRUM -> R.string.backlight_mode_spectrum
        BacklightMode.MARQUEE -> R.string.backlight_mode_marquee
        BacklightMode.RAINBOW -> R.string.backlight_mode_rainbow
    }
