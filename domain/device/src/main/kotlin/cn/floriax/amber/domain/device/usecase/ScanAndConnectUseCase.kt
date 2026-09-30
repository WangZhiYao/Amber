package cn.floriax.amber.domain.device.usecase

import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.model.ClockDevice
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Outcome of a scan-and-connect attempt.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
sealed interface ConnectResult {
    /** The connection flow was initiated for [device] (see the repository's "initiated" semantics). */
    data class Initiated(val device: ClockDevice) : ConnectResult

    /** Bluetooth is off; the user must enable it before scanning can work. */
    data object BluetoothOff : ConnectResult

    /** The scan finished without finding a device. */
    data object NoDeviceFound : ConnectResult

    /** Scanning or connecting failed; [cause] carries the original failure. */
    data class Failed(val cause: Throwable) : ConnectResult
}

/**
 * Scans for a glow clock and initiates a connection to the first one found.
 *
 * Combines two sources ([DeviceScanner] + [ClockRepository]), which is why it
 * is a use case rather than a plain repository call. It is the entry point
 * for "connect" when no device is remembered yet; once device persistence
 * lands, connecting to a remembered device goes through the repository and
 * this use case remains the first-run path.
 *
 * Failures are reported as [ConnectResult], never thrown (cancellation is
 * the exception and propagates as usual).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class ScanAndConnectUseCase @Inject constructor(
    private val scanner: DeviceScanner,
    private val clock: ClockRepository,
) {
    suspend operator fun invoke(): ConnectResult = try {
        // Bluetooth off is reported separately so the UI can guide the user to
        // the system enable dialog instead of showing "no device found".
        if (!scanner.isBluetoothEnabled) return ConnectResult.BluetoothOff
        val found = scanner.scan().firstOrNull()?.firstOrNull()
        when {
            found == null -> ConnectResult.NoDeviceFound
            else -> {
                val device = ClockDevice(
                    mac = found.mac,
                    alias = found.name,
                    advertisedName = found.name,
                    isDefault = true,
                    lastConnectedAt = System.currentTimeMillis(),
                )
                // connect never reports business failures; a failure here is a
                // real failure of the connection flow itself.
                clock.connect(device).getOrThrow()
                ConnectResult.Initiated(device)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        ConnectResult.Failed(e)
    }
}
