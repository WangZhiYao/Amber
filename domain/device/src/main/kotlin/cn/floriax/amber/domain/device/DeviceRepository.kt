package cn.floriax.amber.domain.device

import cn.floriax.amber.domain.device.model.ClockDevice
import kotlinx.coroutines.flow.Flow

/**
 * Device repository: the remembered device list (aliases, default device).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface DeviceRepository {
    val devices: Flow<List<ClockDevice>>
    suspend fun upsert(device: ClockDevice)
    suspend fun setDefault(mac: String)
    suspend fun rename(mac: String, alias: String)
    suspend fun delete(mac: String)
    suspend fun defaultDevice(): ClockDevice?
}
