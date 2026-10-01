package cn.floriax.amber.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.floriax.amber.core.database.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Preset table access. Saving goes through [BaseDao.insert] (returns the
 * new row id); deletion is by id.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@Dao
interface PresetDao : BaseDao<PresetEntity> {

    /** All presets in display order. */
    @Query("SELECT * FROM presets ORDER BY order_index")
    fun observeAll(): Flow<List<PresetEntity>>

    /** Renames one row. */
    @Query("UPDATE presets SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Number of presets (next order index on insert). */
    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int

    /** Deletes one row. */
    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)
}
