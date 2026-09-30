package cn.floriax.amber.core.ble.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.HexFormat

/**
 * 时间帧单测：十进制直写编码、now 工厂、非法值校验与往返。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class TimeFrameTest {

    private fun hex(s: String) = HexFormat.of().parseHex(s.replace("-", ""))

    @Test
    fun `编码为十进制直写`() {
        // 2026-09-27 12:03:45，关机23:30，开机08:00，闹钟07:30
        val f = TimeFrame(
            second = 45, minute = 3, hour = 12, day = 27, month = 9, year = 26,
            offMinute = 30, offHour = 23, onMinute = 0, onHour = 8,
            alarmMinute = 30, alarmHour = 7,
        )
        // 45=0x2D, 3, 12=0x0C, 27=0x1B, 9, 26=0x1A, 30=0x1E, 23=0x17, 0, 8, 30=0x1E, 7
        assertEquals(
            "99-2D-03-0C-1B-09-1A-1E-17-00-08-1E-07-66",
            f.encode().toHexDisplay(),
        )
    }

    @Test
    fun `解析§0实测黄金帧`() {
        // 设备RTC全零，定时开机08:00
        val f = TimeFrame.parse(hex("99-00-00-00-00-00-00-00-00-00-08-00-00-66"))!!
        assertEquals(0, f.second)
        assertEquals(8, f.onHour)
        assertEquals(0, f.onMinute)
        assertEquals(0, f.alarmHour)
    }

    @Test
    fun `now工厂组装完整帧`() {
        val dt = LocalDateTime.of(2026, 9, 27, 12, 3, 45)
        val f = TimeFrame.now(
            dateTime = dt,
            off = LocalTime.of(23, 30), on = LocalTime.of(8, 0), alarm = LocalTime.of(7, 30),
        )
        assertEquals("99-2D-03-0C-1B-09-1A-1E-17-00-08-1E-07-66", f.encode().toHexDisplay())
    }

    @Test
    fun `往返一致`() {
        val f = TimeFrame.now(
            LocalDateTime.of(2026, 1, 2, 3, 4, 5),
            LocalTime.of(1, 2), LocalTime.of(3, 4), LocalTime.of(5, 6),
        )
        assertEquals(f, TimeFrame.parse(f.encode()))
    }

    @Test
    fun `非法时间值返回null`() {
        assertNull(TimeFrame.parse(hex("99-2D-3C-0C-1B-09-1A-00-00-00-00-00-00-66"))) // 分=60
        assertNull(TimeFrame.parse(hex("99-2D-03-18-1B-09-1A-00-00-00-00-00-00-66"))) // 时=24
        assertNull(TimeFrame.parse(hex("99-2D-03-0C-20-09-1A-00-00-00-00-00-00-66"))) // 日=32
    }

    @Test
    fun `帧头帧尾错误返回null`() {
        assertNull(TimeFrame.parse(hex("7F-00-00-00-00-00-00-00-00-00-08-00-00-66")))
    }
}
