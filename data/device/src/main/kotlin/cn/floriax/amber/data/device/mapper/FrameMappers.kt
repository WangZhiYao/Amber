package cn.floriax.amber.data.device.mapper

import cn.floriax.amber.core.ble.protocol.LedFrame
import cn.floriax.amber.core.ble.protocol.SwitchFrame
import cn.floriax.amber.core.ble.protocol.TimeFrame
import cn.floriax.amber.domain.clock.SwitchConfig
import cn.floriax.amber.domain.clock.TimeConfig
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.BacklightMode
import java.time.LocalTime

/**
 * Backlight → LED frame. Reserved bytes are echoed by the repository from the
 * latest report and do not pass through this mapper.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun Backlight.toLedFrame(): LedFrame = LedFrame(
    hues = IntArray(Backlight.GROUP_COUNT) { hues[it].coerceIn(0, 255) },
    saturations = IntArray(Backlight.GROUP_COUNT) { saturations[it].coerceIn(0, 255) },
    brightness = brightness.coerceIn(0, 255),
    mode = mode.code,
)

/**
 * LED frame → backlight. Unknown mode codes fall back to SPECTRUM.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun LedFrame.toBacklight(): Backlight = Backlight(
    hues = hues.toList(),
    saturations = saturations.toList(),
    brightness = brightness,
    mode = BacklightMode.of(mode) ?: BacklightMode.SPECTRUM,
)

/**
 * Device-reported time frame → timer config. Only the timer fields are
 * extracted (protocol red line: ignore the RTC fields).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun TimeFrame.toTimeConfig(): TimeConfig = TimeConfig(
    powerOn = LocalTime.of(onHour, onMinute),
    powerOff = LocalTime.of(offHour, offMinute),
    alarm = LocalTime.of(alarmHour, alarmMinute),
)

/**
 * Switch frame → switch config.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun SwitchFrame.toSwitchConfig(): SwitchConfig = SwitchConfig(
    powerOffEnabled = offEnabled,
    powerOnEnabled = onEnabled,
    alarmEnabled = alarmEnabled,
    hour12 = hour12,
    colonBlink = colonBlink,
    mute = mute,
    lockRemote = lockRemote,
)

/**
 * Switch config → switch frame. byte4 is a reserved byte echoed from the
 * latest report (supplied by the repository).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun SwitchConfig.toSwitchFrame(byte4: Int): SwitchFrame = SwitchFrame(
    offEnabled = powerOffEnabled,
    onEnabled = powerOnEnabled,
    alarmEnabled = alarmEnabled,
    byte4 = byte4,
    hour12 = hour12,
    colonBlink = colonBlink,
    lockRemote = lockRemote,
    mute = mute,
)
