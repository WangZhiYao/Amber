package cn.floriax.amber.data.device.logger

import android.util.Log
import cn.floriax.amber.core.ble.protocol.FrameKind
import cn.floriax.amber.domain.device.model.FrameLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Frame log aggregator (debug-panel data source), keeping the most recent
 * 500 entries. Consumes raw frame bytes from the BLE layer and produces the
 * domain [FrameLog] — the single log type in the codebase.
 *
 * SYS entries are mirrored to logcat — connection orchestration messages
 * ("Connect failed", "Link lost", backoff transitions) flow through here
 * and are the other half of the BLE story the core/ble wrapper logs.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Singleton
class FrameLogAggregator @Inject constructor() {
    private val _logs = MutableStateFlow<List<FrameLog>>(emptyList())
    val logs: StateFlow<List<FrameLog>> = _logs

    fun log(direction: FrameLog.Direction, bytes: ByteArray?, text: String) {
        // Only SYS entries are mirrored to logcat: frame traffic is already
        // logged by the core/ble connection wrapper — duplicating TX/RX here
        // would double every frame line.
        if (direction == FrameLog.Direction.SYS) {
            Log.d(TAG, text)
        }
        _logs.update { current ->
            (current + FrameLog(System.currentTimeMillis(), direction, bytes, text)).takeLast(500)
        }
    }

    fun tx(bytes: ByteArray) = log(FrameLog.Direction.TX, bytes, label(bytes))
    fun rx(bytes: ByteArray) = log(FrameLog.Direction.RX, bytes, label(bytes))
    fun sys(text: String) = log(FrameLog.Direction.SYS, null, text)

    /**
     * Frame-kind label: the locale-free enum name (e.g. "LED"/"TIME") for
     * exported text; UI-facing labels are localized in the UI layer —
     * this aggregator holds no display text.
     */
    private fun label(bytes: ByteArray): String = FrameKind.of(bytes)?.name ?: "?"

    private companion object {
        private const val TAG = "FrameLogAggregator"
    }
}
