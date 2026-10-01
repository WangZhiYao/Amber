package cn.floriax.amber.feature.settings.debug

import androidx.lifecycle.viewModelScope
import cn.floriax.amber.core.ble.protocol.parseFrameInput
import cn.floriax.amber.core.ble.protocol.toHexDisplay
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug panel state.
 *
 * @property logs the frame log (newest last; capped by the aggregator).
 * @property input the manual frame hex input.
 * @property inputError null = not validated yet (blank), true = invalid,
 * false = valid.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
data class DebugUiState(
    val logs: List<cn.floriax.amber.domain.device.model.FrameLog> = emptyList(),
    val input: String = "",
    val inputError: Boolean? = null,
)

/**
 * Debug panel one-shot events.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
sealed interface DebugSideEffect {
    /** The logs were formatted into [text] and should go to the clipboard. */
    data class Copied(val text: String) : DebugSideEffect

    /** The manual frame was written. */
    data object Sent : DebugSideEffect

    /** The manual frame could not be written (invalid input, not connected, link failure). */
    data object SendFailed : DebugSideEffect
}

/**
 * Debug panel ViewModel: frame log projection and manual frame sending.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val clock: ClockRepository,
) : BaseMVIViewModel<DebugUiState, DebugSideEffect>() {

    // Getter form: avoids the base-class construction-time initialization trap.
    override val initialState: DebugUiState get() = DebugUiState()

    init {
        viewModelScope.launch {
            clock.frameLogs.collect { logs ->
                intent { reduce { copy(logs = logs) } }
            }
        }
    }

    /** Live input validation: blank = untouched, otherwise parsed. */
    fun onInputChange(value: String) = intent {
        val error = if (value.isBlank()) null else parseFrameInput(value) == null
        reduce { copy(input = value, inputError = error) }
    }

    /** Sends the parsed frame; failures surface as a toast side effect. */
    fun onSend() = intent {
        val bytes = parseFrameInput(state.input)
        if (bytes == null) {
            reduce { copy(inputError = true) }
            postSideEffect(DebugSideEffect.SendFailed)
            return@intent
        }
        clock.sendRaw(bytes).fold(
            onSuccess = { postSideEffect(DebugSideEffect.Sent) },
            onFailure = { postSideEffect(DebugSideEffect.SendFailed) },
        )
    }

    /** Formats the whole log for the clipboard. */
    fun onCopyLogs() = intent {
        val text = state.logs.joinToString("\n") { log ->
            "${log.bytes?.toHexDisplay() ?: ""} ${log.text}"
        }
        postSideEffect(DebugSideEffect.Copied(text))
    }
}
