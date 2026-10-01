package cn.floriax.amber.data.light.mapper

import cn.floriax.amber.core.database.entity.PresetEntity
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.BacklightMode
import cn.floriax.amber.domain.light.Preset

/**
 * Preset row ↔ domain model. The flat H/S columns reassemble into the
 * backlight snapshot lists; this mapper is the only place that knows
 * both sides.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
fun Preset.toEntity(orderIndex: Int): PresetEntity = PresetEntity(
    id = id,
    name = name,
    orderIndex = orderIndex,
    hue0 = backlight.hues[0],
    hue1 = backlight.hues[1],
    hue2 = backlight.hues[2],
    hue3 = backlight.hues[3],
    sat0 = backlight.saturations[0],
    sat1 = backlight.saturations[1],
    sat2 = backlight.saturations[2],
    sat3 = backlight.saturations[3],
    brightness = backlight.brightness,
    modeCode = backlight.mode.code,
)

/** Row → domain model (unknown mode codes fall back to SPECTRUM). */
fun PresetEntity.toPreset(): Preset = Preset(
    id = id,
    name = name,
    orderIndex = orderIndex,
    backlight = Backlight(
        hues = listOf(hue0, hue1, hue2, hue3),
        saturations = listOf(sat0, sat1, sat2, sat3),
        brightness = brightness,
        mode = BacklightMode.of(modeCode) ?: BacklightMode.SPECTRUM,
    ),
)
