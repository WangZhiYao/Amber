package cn.floriax.amber.core.ble.protocol

/**
 * Query frame: sent after connecting to request the device's current config.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
object QueryFrame {
    val BYTES = byteArrayOf(
        0x23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xE8.toByte(),
    )
}
