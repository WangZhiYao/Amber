package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.database.dao.ClockDeviceDao
import cn.floriax.amber.data.device.mapper.toClockDevice
import cn.floriax.amber.data.device.mapper.toEntity
import cn.floriax.amber.domain.device.model.ClockDevice
import cn.floriax.amber.domain.device.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeviceRepository] on Room. The DAO is pure SQL delegation; the
 * single-default rule is applied here (clear every default, then set
 * the new one) so it is unit-testable without a database.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val dao: ClockDeviceDao,
) : DeviceRepository {

    override fun observeDevices(): Flow<List<ClockDevice>> =
        dao.observeAll().map { rows -> rows.map { it.toClockDevice() } }

    override suspend fun upsert(device: ClockDevice) = dao.upsert(device.toEntity())

    override suspend fun defaultDevice(): ClockDevice? = dao.findDefault()?.toClockDevice()

    override suspend fun setDefault(mac: String) {
        dao.clearDefault()
        dao.setDefault(mac)
    }

    override suspend fun setDefaultIfNone(mac: String) = dao.setDefaultIfNone(mac)

    override suspend fun rename(mac: String, alias: String) = dao.rename(mac, alias)

    override suspend fun delete(mac: String) = dao.delete(mac)
}
