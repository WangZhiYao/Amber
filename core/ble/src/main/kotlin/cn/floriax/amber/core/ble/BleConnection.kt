package cn.floriax.amber.core.ble

import kotlinx.coroutines.flow.Flow

/**
 * An established BLE connection: notification stream, writes and the
 * disconnect signal.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface BleConnection {
    /** Emits once on disconnect (null = clean close); the connection object is dead afterwards. */
    val disconnected: Flow<Throwable?>
    val notifications: Flow<ByteArray>
    suspend fun write(bytes: ByteArray)
    fun close()
}
