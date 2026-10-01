package cn.floriax.amber.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Saved glow clock device (Room row). MAC is the stable identity — one
 * physical device is one row regardless of renames.
 *
 * Domain mapping lives in :data:device (core must not depend on domain);
 * this module knows rows only.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Entity(tableName = "clock_devices")
data class ClockDeviceEntity(
    @PrimaryKey val mac: String,
    val alias: String,
    @ColumnInfo(name = "advertised_name") val advertisedName: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "last_connected_at") val lastConnectedAt: Long,
)