package cn.floriax.amber.data.light.repository

import cn.floriax.amber.core.database.dao.PresetDao
import cn.floriax.amber.data.light.mapper.toEntity
import cn.floriax.amber.data.light.mapper.toPreset
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.Preset
import cn.floriax.amber.domain.light.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PresetRepository] on Room. Pure delegation — saved presets append at
 * the end of the display order (order index = current count).
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@Singleton
class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao,
) : PresetRepository {

    override fun observePresets(): Flow<List<Preset>> =
        dao.observeAll().map { rows -> rows.map { it.toPreset() } }

    override suspend fun save(name: String, backlight: Backlight): Long {
        val orderIndex = dao.count()
        val preset = Preset(id = 0, name = name, backlight = backlight, orderIndex = orderIndex)
        return dao.insert(preset.toEntity(orderIndex))
    }

    override suspend fun rename(id: Long, name: String) = dao.rename(id, name)

    override suspend fun delete(id: Long) = dao.delete(id)
}
