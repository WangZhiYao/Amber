package cn.floriax.amber.feature.light

/**
 * One-shot light screen events: "not connected" and write failures stay
 * distinct so the UI can explain them differently, and the connection entry
 * reports its own outcomes.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
sealed interface LightSideEffect {
    data object NotConnected : LightSideEffect
    data class WriteFailed(val message: String) : LightSideEffect

    /** A scan finished without finding a device. */
    data object NoDeviceFound : LightSideEffect

    /** Bluetooth permissions are missing (denied, or revoked in settings). */
    data object PermissionRequired : LightSideEffect

    /** Bluetooth is off: the screen asks the user to enable it. */
    data object BluetoothOff : LightSideEffect
}
