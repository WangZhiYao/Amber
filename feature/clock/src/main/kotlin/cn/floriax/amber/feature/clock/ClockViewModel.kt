package cn.floriax.amber.feature.clock

import cn.floriax.amber.domain.device.model.ConnectionState
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clock screen ViewModel (placeholder state, no device touched yet).
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class ClockViewModel @Inject constructor() : BaseMVIViewModel<ClockUiState, Nothing>() {

    override val initialState: ClockUiState
        get() = ClockUiState(
            connection = ConnectionState.CONNECTED,
            deviceName = "客厅辉光钟",
        )

    /** Syncs the device clock to the phone time (simulated placeholder). */
    fun syncTime() {
        intent {
            reduce { state.copy(syncing = true) }
            delay(800.milliseconds)
            reduce {
                state.copy(
                    syncing = false,
                    lastSyncAt = System.currentTimeMillis(),
                )
            }
        }
    }

    /** Sets one timer time (placeholder: local state only). */
    fun setTimer(kind: TimerKind, value: LocalTime) {
        intent {
            val timers = when (kind) {
                TimerKind.POWER_ON -> state.timers.copy(powerOn = value)
                TimerKind.POWER_OFF -> state.timers.copy(powerOff = value)
                TimerKind.ALARM -> state.timers.copy(alarm = value)
            }
            reduce { state.copy(timers = timers) }
        }
    }

    /** Sets the hour format (placeholder: local state only). */
    fun setHourFormat(hour12: Boolean) {
        intent {
            reduce { state.copy(switches = state.switches.copy(hour12 = hour12)) }
        }
    }

    /** Sets one function switch (placeholder: local state only). */
    fun setSwitch(field: SwitchField, value: Boolean) {
        intent {
            val switches = when (field) {
                SwitchField.POWER_ON -> state.switches.copy(powerOnEnabled = value)
                SwitchField.POWER_OFF -> state.switches.copy(powerOffEnabled = value)
                SwitchField.ALARM -> state.switches.copy(alarmEnabled = value)
                SwitchField.COLON_BLINK -> state.switches.copy(colonBlink = value)
                SwitchField.MUTE -> state.switches.copy(mute = value)
                SwitchField.LOCK_REMOTE -> state.switches.copy(lockRemote = value)
            }
            reduce { state.copy(switches = switches) }
        }
    }
}
