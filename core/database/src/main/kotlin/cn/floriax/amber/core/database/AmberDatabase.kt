package cn.floriax.amber.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.floriax.amber.core.database.dao.ClockDeviceDao
import cn.floriax.amber.core.database.entity.ClockDeviceEntity

/**
 * App database. One home for every persisted table (devices, presets,
 * ...): data:* modules share it via DI instead of each running its own
 * Room setup.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Database(
    entities = [ClockDeviceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AmberDatabase : RoomDatabase() {

    /** Saved glow clock devices. */
    abstract fun clockDeviceDao(): ClockDeviceDao
}