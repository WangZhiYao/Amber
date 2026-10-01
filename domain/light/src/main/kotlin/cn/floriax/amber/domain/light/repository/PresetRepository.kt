package cn.floriax.amber.domain.light.repository

import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.Preset
import kotlinx.coroutines.flow.Flow

/**
 * Saved light presets (persistence port).
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
interface PresetRepository {

    /** All presets in display order. */
    fun observePresets(): Flow<List<Preset>>

    /**
     * Saves a new preset (appended at the end of the display order).
     *
     * @return the new preset's id.
     */
    suspend fun save(name: String, backlight: Backlight): Long

    /** Renames the preset keyed by [id]. */
    suspend fun rename(id: Long, name: String)

    /** Deletes the preset keyed by [id]. */
    suspend fun delete(id: Long)
}
