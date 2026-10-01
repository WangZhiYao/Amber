package cn.floriax.amber.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Saved light preset (Room row). The backlight snapshot is stored flat
 * (four H/S bytes, brightness, mode code) — no JSON blob, so every field
 * stays a plain column.
 *
 * Domain mapping lives in :data:light (core must not depend on domain).
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "hue_0") val hue0: Int,
    @ColumnInfo(name = "hue_1") val hue1: Int,
    @ColumnInfo(name = "hue_2") val hue2: Int,
    @ColumnInfo(name = "hue_3") val hue3: Int,
    @ColumnInfo(name = "sat_0") val sat0: Int,
    @ColumnInfo(name = "sat_1") val sat1: Int,
    @ColumnInfo(name = "sat_2") val sat2: Int,
    @ColumnInfo(name = "sat_3") val sat3: Int,
    val brightness: Int,
    @ColumnInfo(name = "mode_code") val modeCode: Int,
)
