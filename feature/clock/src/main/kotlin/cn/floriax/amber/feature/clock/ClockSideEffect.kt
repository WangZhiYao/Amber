package cn.floriax.amber.feature.clock

/**
 * One-shot clock screen events: "not connected" and write failures stay
 * distinct so the UI can explain them differently, and the connection entry
 * reports its own outcomes.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
sealed interface ClockSideEffect {
    data object NotConnected : ClockSideEffect
    data object Synced : ClockSideEffect
    data class WriteFailed(val message: String) : ClockSideEffect

    /** A scan finished without finding a device. */
    data object NoDeviceFound : ClockSideEffect

    /** Bluetooth permissions are missing (denied, or revoked in settings). */
    data object PermissionRequired : ClockSideEffect

    /** Bluetooth is off: the screen asks the user to enable it. */
    data object BluetoothOff : ClockSideEffect
}
