package cn.floriax.amber.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.floriax.amber.core.database.dao.ClockDeviceDao
import cn.floriax.amber.core.database.dao.PresetDao
import cn.floriax.amber.core.database.entity.ClockDeviceEntity
import cn.floriax.amber.core.database.entity.PresetEntity

/**
 * App database. One home for every persisted table (devices, presets,
 * ...): data:* modules share it via DI instead of each running its own
 * Room setup.
 *
 * Pre-release: the schema changes freely at version 1 and local
 * databases rebuild destructively (debug data only); real migrations
 * start once a schema version ships.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Database(
    entities = [ClockDeviceEntity::class, PresetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AmberDatabase : RoomDatabase() {

    /** Saved glow clock devices. */
    abstract fun clockDeviceDao(): ClockDeviceDao

    /** Saved light presets. */
    abstract fun presetDao(): PresetDao
}
