package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.ble.BleConnection
import kotlinx.coroutines.Job

/**
 * All resources of one connection attempt: the underlying connection plus
 * the two collection jobs on it (notifications / disconnect signal).
 * The session carries the connection generation that created it — late
 * callbacks of a superseded session compare identity/generation first and
 * must not touch the current state machine (see the concurrency notes in
 * ClockRepositoryImpl).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
internal class ConnectionSession(
    val generation: Long,
    val conn: BleConnection,
) {
    var collectJob: Job? = null
    var disconnectJob: Job? = null

    /** Cancels the collection jobs and closes the underlying connection. Idempotent. */
    fun close() {
        collectJob?.cancel()
        disconnectJob?.cancel()
        conn.close()
    }
}
