package cn.floriax.amber.data.light.repository

import cn.floriax.amber.core.database.dao.PresetDao
import cn.floriax.amber.core.database.entity.PresetEntity
import cn.floriax.amber.domain.light.Backlight
import cn.floriax.amber.domain.light.BacklightMode
import cn.floriax.amber.domain.light.repository.PresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 预设仓库单测：快照扁平列映射、保存追加顺序、重命名/删除委托。
 * Room DAO 是纯 SQL 委托，用 fake 驱动。
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
class PresetRepositoryImplTest {

    private val dao = FakePresetDao()

    private val repository: PresetRepository = PresetRepositoryImpl(dao)

    @Test
    fun `保存映射快照并追加末尾`() = runTest {
        val backlight = Backlight(
            hues = listOf(1, 2, 3, 4),
            saturations = listOf(5, 6, 7, 8),
            brightness = 99,
            mode = BacklightMode.STATIC,
        )

        val id = repository.save("红色呼吸", backlight)

        val saved = dao.rows.getValue(id)
        assertEquals("红色呼吸", saved.name)
        assertEquals(0, saved.orderIndex)   // first preset → order 0
        assertEquals(listOf(1, 2, 3, 4), listOf(saved.hue0, saved.hue1, saved.hue2, saved.hue3))
        assertEquals(listOf(5, 6, 7, 8), listOf(saved.sat0, saved.sat1, saved.sat2, saved.sat3))
        assertEquals(99, saved.brightness)
        assertEquals(3, saved.modeCode)
    }

    @Test
    fun `第二个预设排在后面`() = runTest {
        repository.save("一", Backlight.DEFAULT)
        repository.save("二", Backlight.DEFAULT)

        val presets = repository.observePresets().first()
        assertEquals(listOf("一", "二"), presets.map { it.name })
    }

    @Test
    fun `观察流映射回领域模型`() = runTest {
        dao.rows[1L] = PresetEntity(
            id = 1, name = "蓝色", orderIndex = 0,
            hue0 = 10, hue1 = 10, hue2 = 10, hue3 = 10,
            sat0 = 20, sat1 = 20, sat2 = 20, sat3 = 20,
            brightness = 30, modeCode = 2,
        )
        dao.list.value = dao.rows.values.toList()

        val preset = repository.observePresets().first().single()

        assertEquals(listOf(10, 10, 10, 10), preset.backlight.hues)
        assertEquals(listOf(20, 20, 20, 20), preset.backlight.saturations)
        assertEquals(30, preset.backlight.brightness)
        assertEquals(BacklightMode.BREATH, preset.backlight.mode)
    }

    @Test
    fun `重命名与删除委托主键`() = runTest {
        repository.rename(7L, "新名字")
        repository.delete(8L)

        assertEquals(7L to "新名字", dao.renamed)
        assertEquals(8L, dao.deleted)
    }

    /** 内存 DAO：自增 id，记录写调用；写后推送观察流（模拟 Room 的响应式查询）。 */
    private class FakePresetDao : PresetDao {
        val rows = linkedMapOf<Long, PresetEntity>()
        val list = MutableStateFlow<List<PresetEntity>>(emptyList())
        var nextId = 1L
        var renamed: Pair<Long, String>? = null
        var deleted: Long? = null

        private fun publish() {
            list.value = rows.values.sortedBy { it.orderIndex }
        }

        override fun observeAll() = list

        override suspend fun insert(item: PresetEntity): Long {
            val id = nextId++
            rows[id] = item.copy(id = id)
            publish()
            return id
        }

        override suspend fun upsert(item: PresetEntity) {
            rows[item.id] = item
            publish()
        }

        override suspend fun delete(item: PresetEntity): Int {
            rows.remove(item.id)
            publish()
            return 1
        }

        override suspend fun rename(id: Long, name: String) {
            renamed = id to name
        }

        override suspend fun count(): Int = rows.size

        override suspend fun delete(id: Long) {
            deleted = id
            rows.remove(id)
            publish()
        }
    }
}
