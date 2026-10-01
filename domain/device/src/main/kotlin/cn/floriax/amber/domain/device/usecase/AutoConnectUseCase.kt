package cn.floriax.amber.domain.device.usecase

import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.repository.DeviceRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Connects the default device at app startup.
 *
 * Connects by MAC directly (no scan, so no runtime location permission
 * is needed below Android 12). Silent by design: no default device,
 * Bluetooth off or a failed attempt simply leaves the pill in
 * "not connected" — the connection flow's background reconnection and
 * the manual pill entry take over from there.
 *
 * Callers are still expected to hold the BLE permissions on Android 12+
 * (BLUETOOTH_CONNECT); without them nothing is attempted.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class AutoConnectUseCase @Inject constructor(
    private val devices: DeviceRepository,
    private val scanner: DeviceScanner,
    private val clock: ClockRepository,
) {

    /** Attempts to connect the default device; never throws. */
    suspend operator fun invoke() {
        try {
            val device = devices.defaultDevice() ?: return
            if (!scanner.isBluetoothEnabled) return
            clock.connect(device)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Startup convenience: any failure falls back to manual entry.
        }
    }
}
