package cn.floriax.amber.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.floriax.amber.core.database.entity.ClockDeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Clock device table access. Pure SQL delegation.
 *
 * Saving a device goes through [BaseDao.upsert] (MAC is the stable
 * identity); deletion is by MAC without a prior read, so [BaseDao.delete]
 * (by entity) is unused here.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Dao
interface ClockDeviceDao : BaseDao<ClockDeviceEntity> {

    /** All saved devices, newest connection first. */
    @Query("SELECT * FROM clock_devices ORDER BY last_connected_at DESC")
    fun observeAll(): Flow<List<ClockDeviceEntity>>

    /** The default device row, or null when none is marked. */
    @Query("SELECT * FROM clock_devices WHERE is_default = 1 LIMIT 1")
    suspend fun findDefault(): ClockDeviceEntity?

    /** Clears the default flag on every row. */
    @Query("UPDATE clock_devices SET is_default = 0")
    suspend fun clearDefault()

    /** Sets the default flag on one row. */
    @Query("UPDATE clock_devices SET is_default = 1 WHERE mac = :mac")
    suspend fun setDefault(mac: String)

    /** Sets the default flag on one row only when no default exists yet. */
    @Query(
        "UPDATE clock_devices SET is_default = 1 WHERE mac = :mac " +
            "AND NOT EXISTS(SELECT 1 FROM clock_devices WHERE is_default = 1)"
    )
    suspend fun setDefaultIfNone(mac: String)

    /** Renames one row. */
    @Query("UPDATE clock_devices SET alias = :alias WHERE mac = :mac")
    suspend fun rename(mac: String, alias: String)

    /** Deletes one row. */
    @Query("DELETE FROM clock_devices WHERE mac = :mac")
    suspend fun delete(mac: String)
}
