package cn.floriax.amber.data.device.repository

import cn.floriax.amber.core.ble.protocol.FrameKind
import cn.floriax.amber.core.ble.protocol.LedFrame
import cn.floriax.amber.core.ble.protocol.SwitchFrame
import cn.floriax.amber.core.ble.protocol.TimeFrame
import cn.floriax.amber.core.ble.protocol.toHexDisplay
import cn.floriax.amber.data.device.logger.FrameLogAggregator
import cn.floriax.amber.data.device.mapper.toBacklight
import cn.floriax.amber.data.device.mapper.toSwitchConfig
import cn.floriax.amber.data.device.mapper.toTimeConfig
import cn.floriax.amber.domain.device.model.DeviceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Device state projector: the single write entry point of [DeviceState]
 * (internal to the data layer, not a test seam). Three kinds of writes share
 * it: device report projection ([onFrame]), connection orchestration state
 * and optimistic updates of write operations ([update]).
 * It also holds report-only companion state: handshake progress bits and
 * reserved bytes (protocol rule: whatever the report carries is echoed back
 * on the next send).
 *
 * Starts no coroutines and holds no connection — concurrency and lifecycle
 * belong to the session orchestration in ClockRepositoryImpl, so this class
 * has no concurrency concerns and may be used from any thread.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
internal class DeviceStateProjector(private val logger: FrameLogAggregator) {

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState

    /** Handshake progress bits: set when LED/switch/time frames have all arrived. Reset per attempt. */
    private val _handshakeBits = MutableStateFlow(0)
    val handshakeBits: StateFlow<Int> = _handshakeBits

    /** Reserved bytes of the latest reported LED frame (byte11-12), echoed on send. */
    var ledReserved: IntArray = intArrayOf(0, 0)
        private set

    /** Reserved byte of the latest reported switch frame (byte4), echoed on send. */
    var switchByte4: Int = 1
        private set

    /** Unified state write entry: connection state, optimistic updates, sync records. */
    fun update(transform: (DeviceState) -> DeviceState) {
        _deviceState.update(transform)
    }

    /** New connection attempt: clear the handshake progress. */
    fun resetHandshake() {
        _handshakeBits.value = 0
    }

    /** Device report → state projection. The device is the source of truth: a valid frame updates its fields (also tolerates proactive pushes). */
    fun onFrame(bytes: ByteArray) {
        when (FrameKind.of(bytes)) {
            FrameKind.LED -> LedFrame.parse(bytes)?.let { f ->
                logger.rx(bytes)
                _handshakeBits.update { it or LED_BIT }
                ledReserved = f.reserved
                update { it.copy(backlight = f.toBacklight()) }
            } ?: logger.sys("Ignored malformed LED frame ${bytes.toHexDisplay()}")

            FrameKind.SWITCH -> SwitchFrame.parse(bytes)?.let { f ->
                logger.rx(bytes)
                _handshakeBits.update { it or SWITCH_BIT }
                switchByte4 = f.byte4
                update { it.copy(switches = f.toSwitchConfig()) }
            } ?: logger.sys("Ignored malformed switch frame ${bytes.toHexDisplay()}")

            FrameKind.TIME -> TimeFrame.parse(bytes)?.let { f ->
                logger.rx(bytes)
                _handshakeBits.update { it or TIME_BIT }
                update { it.copy(timers = f.toTimeConfig()) }
            } ?: logger.sys("Ignored malformed time frame ${bytes.toHexDisplay()}")

            else -> logger.sys("Ignored unknown frame ${bytes.toHexDisplay()}")
        }
    }

    private companion object {
        /** Handshake progress bits: bit0=LED bit1=switch bit2=time (matches HANDSHAKE_DONE_BITS). */
        const val LED_BIT = 1
        const val SWITCH_BIT = 2
        const val TIME_BIT = 4
    }
}
