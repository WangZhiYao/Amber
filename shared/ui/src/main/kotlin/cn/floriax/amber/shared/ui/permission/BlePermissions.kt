package cn.floriax.amber.shared.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Version-dispatched BLE permissions: Android 12+ uses
 * BLUETOOTH_SCAN/CONNECT, older releases fall back to ACCESS_FINE_LOCATION
 * (scanning requires it there).
 *
 * The static declarations live in `:core:ble`'s manifest (the module that
 * knows what the BLE stack needs) and are merged into the app. This runtime
 * list has to be duplicated here because features depend on domain only and
 * cannot see :core:ble — **keep the two in sync** when the requirement
 * changes.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun requiredBlePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/** Whether every required BLE permission is granted. */
fun Context.hasBlePermissions(): Boolean = requiredBlePermissions().all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}
