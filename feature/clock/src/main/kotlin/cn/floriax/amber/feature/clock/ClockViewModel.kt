package cn.floriax.amber.feature.clock

import androidx.lifecycle.viewModelScope
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.exception.ClockException
import cn.floriax.amber.domain.device.usecase.ConnectResult
import cn.floriax.amber.domain.device.usecase.ScanAndConnectUseCase
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Clock screen ViewModel: timer/switch/hour-format edits are sent as whole
 * frames; the device state stream is the single source of truth for the UI.
 * Write operations call the repository directly — gating and exception
 * translation are part of the repository contract.
 * "Last sync" is recorded by the repository on every successful time-frame
 * write (automatic and manual alike); this ViewModel only projects it.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@HiltViewModel
class ClockViewModel @Inject constructor(
    private val clock: ClockRepository,
    private val scanAndConnect: ScanAndConnectUseCase,
) : BaseMVIViewModel<ClockUiState, ClockSideEffect>() {

    // Getter form: avoids the base-class construction-time initialization trap.
    override val initialState: ClockUiState get() = ClockUiState()

    init {
        viewModelScope.launch {
            clock.deviceState.collect { s ->
                intent {
                    reduce {
                        copy(
                            connection = s.connection,
                            deviceName = s.deviceName,
                            timers = s.timers,
                            switches = s.switches,
                            lastSyncAt = s.lastSyncAt,
                        )
                    }
                }
            }
        }
    }

    /**
     * Connection entry (pill "not connected"): scan and connect to the first
     * glow clock found. Permission is a view-layer concern — the screen asks
     * for it and calls [onPermissionRequired] when it is missing.
     * The scan is reflected in the pill, and a second tap while it runs is
     * ignored instead of starting another scan.
     */
    fun onRetryConnect() = intent {
        if (state.scanning) return@intent
        reduce { copy(scanning = true) }
        val result = scanAndConnect()
        reduce { copy(scanning = false) }
        when (result) {
            is ConnectResult.BluetoothOff -> postSideEffect(ClockSideEffect.BluetoothOff)
            is ConnectResult.NoDeviceFound -> postSideEffect(ClockSideEffect.NoDeviceFound)
            is ConnectResult.Failed -> postSideEffect(result.cause.toClockSideEffect())
            is ConnectResult.Initiated -> Unit   // device state flows in via the repository
        }
    }

    /** Bluetooth permissions are missing; the screen surfaces this to the user. */
    fun onPermissionRequired() = intent {
        postSideEffect(ClockSideEffect.PermissionRequired)
    }

    /** Syncs the device clock to the phone time. */
    fun syncTime() = intent {
        reduce { copy(syncing = true) }
        clock.syncTime().fold(
            onSuccess = {
                reduce { copy(syncing = false) }
                postSideEffect(ClockSideEffect.Synced)
            },
            onFailure = {
                reduce { copy(syncing = false) }
                postSideEffect(it.toClockSideEffect())
            },
        )
    }

    /** Sets one timer time (resends the whole time frame = implicit sync). */
    fun setTimer(kind: TimerKind, value: LocalTime) = intent {
        val timers = when (kind) {
            TimerKind.POWER_ON -> state.timers.copy(powerOn = value)
            TimerKind.POWER_OFF -> state.timers.copy(powerOff = value)
            TimerKind.ALARM -> state.timers.copy(alarm = value)
        }
        reduce { copy(timers = timers) }
        clock.sendTimers(timers).onFailure { postSideEffect(it.toClockSideEffect()) }
    }

    /** Sets the hour format (switch frame byte5). */
    fun setHourFormat(hour12: Boolean) = intent {
        val switches = state.switches.copy(hour12 = hour12)
        reduce { copy(switches = switches) }
        clock.sendSwitches(switches).onFailure { postSideEffect(it.toClockSideEffect()) }
    }

    /** Sets one function switch. */
    fun setSwitch(field: SwitchField, value: Boolean) = intent {
        val switches = when (field) {
            SwitchField.POWER_ON -> state.switches.copy(powerOnEnabled = value)
            SwitchField.POWER_OFF -> state.switches.copy(powerOffEnabled = value)
            SwitchField.ALARM -> state.switches.copy(alarmEnabled = value)
            SwitchField.COLON_BLINK -> state.switches.copy(colonBlink = value)
            SwitchField.MUTE -> state.switches.copy(mute = value)
            SwitchField.LOCK_REMOTE -> state.switches.copy(lockRemote = value)
        }
        reduce { copy(switches = switches) }
        clock.sendSwitches(switches).onFailure { postSideEffect(it.toClockSideEffect()) }
    }
}

/** Repository failure → clock screen event: "not connected" is not confused with other failures. */
private fun Throwable.toClockSideEffect(): ClockSideEffect =
    if (this is ClockException.NotConnected) {
        ClockSideEffect.NotConnected
    } else {
        ClockSideEffect.WriteFailed(message ?: toString())
    }
