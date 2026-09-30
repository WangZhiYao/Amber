package cn.floriax.amber.core.ble.protocol

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Time frame (bytes 1–12 are plain decimal values, not BCD):
 * [1]second [2]minute [3]hour [4]day [5]month [6]year%100
 * [7]off-minute [8]off-hour [9]on-minute [10]on-hour [11]alarm-minute [12]alarm-hour
 *
 * The device-reported bytes 1–6 (RTC) are always zero — after parsing, only
 * the timer fields may be read (protocol red line).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
data class TimeFrame(
    val second: Int, val minute: Int, val hour: Int,
    val day: Int, val month: Int, val year: Int,
    val offMinute: Int, val offHour: Int,
    val onMinute: Int, val onHour: Int,
    val alarmMinute: Int, val alarmHour: Int,
) {
    fun encode(): ByteArray = byteArrayOf(
        0x99.toByte(),
        second.toByte(), minute.toByte(), hour.toByte(),
        day.toByte(), month.toByte(), year.toByte(),
        offMinute.toByte(), offHour.toByte(),
        onMinute.toByte(), onHour.toByte(),
        alarmMinute.toByte(), alarmHour.toByte(),
        0x66.toByte(),
    )

    companion object {
        /**
         * Assembles a full time frame from the given wall-clock time.
         */
        fun now(
            dateTime: LocalDateTime,
            off: LocalTime, on: LocalTime, alarm: LocalTime,
        ) = TimeFrame(
            second = dateTime.second, minute = dateTime.minute, hour = dateTime.hour,
            day = dateTime.dayOfMonth, month = dateTime.monthValue, year = dateTime.year % 100,
            offMinute = off.minute, offHour = off.hour,
            onMinute = on.minute, onHour = on.hour,
            alarmMinute = alarm.minute, alarmHour = alarm.hour,
        )

        /**
         * Parses a time frame; null on out-of-range values
         * (second>59, minute>59, hour>23, day>31, month>12).
         */
        fun parse(bytes: ByteArray): TimeFrame? {
            if (FrameKind.of(bytes) != FrameKind.TIME) return null
            fun v(i: Int) = bytes[i].toInt() and 0xFF
            val f = TimeFrame(
                second = v(1), minute = v(2), hour = v(3),
                day = v(4), month = v(5), year = v(6),
                offMinute = v(7), offHour = v(8),
                onMinute = v(9), onHour = v(10),
                alarmMinute = v(11), alarmHour = v(12),
            )
            return f.takeIf {
                it.second <= 59 && it.minute <= 59 && it.hour <= 23 &&
                        it.offMinute <= 59 && it.offHour <= 23 &&
                        it.onMinute <= 59 && it.onHour <= 23 &&
                        it.alarmMinute <= 59 && it.alarmHour <= 23 &&
                        // RTC fields may be 0: an un-synced device reports
                        // all zeros (observed on a real device).
                        it.day in 0..31 && it.month in 0..12
            }
        }
    }
}
