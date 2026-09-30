package cn.floriax.amber.core.ble

import no.nordicsemi.android.kotlin.ble.core.errors.GattException

/**
 * Whether a write failure is "link-level": the link must be torn down and
 * reconnected with backoff.
 *
 * The vendor mini-program actively disconnects on write failures with
 * link-level error codes (10006/10001/10008); on Android the equivalent is
 * the Nordic library's [GattException] (incl. GATT_ERROR=133, device
 * already disconnected). Non-GATT exceptions (attribute/parameter errors)
 * do not trigger reconnection — they are reported as ordinary failures.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun Throwable.isLinkFailure(): Boolean = this is GattException
