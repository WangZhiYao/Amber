package cn.floriax.amber.domain.device.repository

import cn.floriax.amber.domain.device.model.ClockDevice
import kotlinx.coroutines.flow.Flow

/**
 * Saved glow clock devices (persistence port). "At most one default" is
 * a repository rule: [setDefault] clears the flag on every other device.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface DeviceRepository {

    /** All saved devices, newest connection first. */
    fun observeDevices(): Flow<List<ClockDevice>>

    /** The default device, or null when none is marked. */
    suspend fun defaultDevice(): ClockDevice?

    /** Inserts or updates the device keyed by MAC. */
    suspend fun upsert(device: ClockDevice)

    /** Marks [mac] the default device (single-default rule, see class doc). */
    suspend fun setDefault(mac: String)

    /**
     * Marks [mac] the default device **only when no default exists yet**
     * — the first connected device becomes the default without user
     * action, so startup auto-connect works out of the box.
     */
    suspend fun setDefaultIfNone(mac: String)

    /** Renames the device keyed by [mac]. */
    suspend fun rename(mac: String, alias: String)

    /** Deletes the device keyed by [mac]. */
    suspend fun delete(mac: String)
}
