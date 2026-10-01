package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.database.dao.ClockDeviceDao
import cn.floriax.amber.core.database.entity.ClockDeviceEntity
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 设备仓库单测：实体↔领域映射与「唯一默认」规则（清旧再置新）。
 * Room DAO 是纯 SQL 委托，用 fake 驱动；真实表走真机验证。
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class DeviceRepositoryImplTest {

    private val dao = FakeDao()

    private val repository: DeviceRepository = DeviceRepositoryImpl(dao)

    @Test
    fun `upsert映射全部字段`() = runTest {
        val device = ClockDevice("EC:B2:A2:02:99:1F", "客厅的钟", "NIXIE", true, 1_700_000_000_000)

        repository.upsert(device)

        val saved = dao.rows.values.single()
        assertEquals("EC:B2:A2:02:99:1F", saved.mac)
        assertEquals("客厅的钟", saved.alias)
        assertEquals("NIXIE", saved.advertisedName)
        assertEquals(1_700_000_000_000, saved.lastConnectedAt)
    }

    @Test
    fun `设默认先清空其他默认`() = runTest {
        repository.setDefault("EC:B2:A2:02:99:1F")

        assertEquals(true, dao.defaultCleared)
        assertEquals("EC:B2:A2:02:99:1F", dao.defaultSetTo)
    }

    @Test
    fun `无默认时首台设备成为默认`() = runTest {
        repository.setDefaultIfNone("A")
        repository.setDefaultIfNone("B")

        // 第二次调用被 DAO 的 setDefaultIfNone 忽略（无默认才生效）。
        assertEquals("A", dao.defaultIfNoneSetTo)
    }

    @Test
    fun `默认设备查询委托DAO`() = runTest {
        dao.defaultRow = entity("A", "客厅的钟")

        assertEquals("客厅的钟", repository.defaultDevice()?.alias)
    }

    @Test
    fun `观察流映射回领域模型`() = runTest {
        dao.rows["A"] = entity("A", "客厅的钟")
        dao.rows["B"] = entity("B", "卧室的钟")
        dao.list.value = dao.rows.values.toList()

        val devices = repository.observeDevices().first()

        assertEquals(listOf("客厅的钟", "卧室的钟"), devices.map { it.alias })
    }

    @Test
    fun `重命名与删除委托主键`() = runTest {
        repository.rename("A", "新名字")
        repository.delete("B")

        assertEquals("A" to "新名字", dao.renamed)
        assertEquals("B", dao.deleted)
    }

    private fun entity(mac: String, alias: String) =
        ClockDeviceEntity(mac, alias, "NIXIE", false, 0L)

    /** 内存 DAO：记录写调用。 */
    private class FakeDao : ClockDeviceDao {
        val rows = linkedMapOf<String, ClockDeviceEntity>()
        val list = MutableStateFlow<List<ClockDeviceEntity>>(emptyList())
        var defaultRow: ClockDeviceEntity? = null
        var defaultCleared = false
        var defaultSetTo: String? = null
        var defaultIfNoneSetTo: String? = null
        var renamed: Pair<String, String>? = null
        var deleted: String? = null

        override fun observeAll() = list

        override suspend fun findDefault(): ClockDeviceEntity? = defaultRow

        override suspend fun insert(item: ClockDeviceEntity): Long {
            rows[item.mac] = item
            return rows.size.toLong()
        }

        override suspend fun upsert(item: ClockDeviceEntity) {
            rows[item.mac] = item
        }

        override suspend fun clearDefault() {
            defaultCleared = true
        }

        override suspend fun setDefault(mac: String) {
            defaultSetTo = mac
        }

        override suspend fun setDefaultIfNone(mac: String) {
            if (defaultIfNoneSetTo == null) defaultIfNoneSetTo = mac
        }

        override suspend fun delete(item: ClockDeviceEntity): Int {
            rows.remove(item.mac)
            return 1
        }

        override suspend fun rename(mac: String, alias: String) {
            renamed = mac to alias
        }

        override suspend fun delete(mac: String) {
            deleted = mac
        }
    }
}
