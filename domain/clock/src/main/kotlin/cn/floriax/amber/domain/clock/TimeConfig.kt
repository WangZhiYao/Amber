package cn.floriax.amber.domain.clock

import java.time.LocalTime

/**
 * Timer schedule: power-on/power-off and alarm times.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
data class TimeConfig(
    val powerOn: LocalTime,
    val powerOff: LocalTime,
    val alarm: LocalTime,
) {
    companion object {
        val DEFAULT = TimeConfig(LocalTime.of(0, 0), LocalTime.of(0, 0), LocalTime.of(0, 0))
    }
}
