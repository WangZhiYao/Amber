package cn.floriax.amber.core.ble.protocol

/**
 * Hex display of frame bytes: uppercase, two digits, `-`-separated
 * (e.g. `7F-FF-…-55`). Logs, the debug panel and tests share this single
 * form instead of each rolling its own.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
fun ByteArray.toHexDisplay(): String =
    joinToString("-") { (it.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0') }
