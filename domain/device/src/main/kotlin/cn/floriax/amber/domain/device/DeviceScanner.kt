package cn.floriax.amber.domain.device

import cn.floriax.amber.domain.device.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow

/**
 * Port for discovering nearby devices.
 *
 * Contract:
 * - cold flow: scanning starts when collected and stops when cancelled;
 * - results are de-duplicated by MAC and each emission is the accumulated
 *   list so far;
 * - the flow completes on its own after the scan timeout (no results left
 *   to find), so collectors do not need their own timeout.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface DeviceScanner {
    /**
     * Whether the Bluetooth adapter is on. Scanning is impossible while it is
     * off, so callers check this first and guide the user to enable it.
     * Reading the adapter state itself requires Bluetooth permissions, so
     * check those before consulting this property.
     */
    val isBluetoothEnabled: Boolean

    fun scan(): Flow<List<DiscoveredDevice>>
}
