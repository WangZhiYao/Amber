package cn.floriax.amber.core.ble

/**
 * BLE operation failure.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
class BleException(message: String, cause: Throwable? = null) : Exception(message, cause)
