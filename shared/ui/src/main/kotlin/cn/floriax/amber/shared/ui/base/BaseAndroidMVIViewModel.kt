package cn.floriax.amber.shared.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [AndroidViewModel] base class implementing the [MVIContainer] contract.
 *
 * Extends [AndroidViewModel] to support [Application]-level dependencies
 * (e.g., via Hilt `@AndroidEntryPoint`). Holds the screen state in a single
 * state flow seeded with [initialState], and runs every intent in
 * [viewModelScope] through one shared [IntentContext], so all state changes
 * funnel through [IntentContext.reduce].
 *
 * @param STATE the immutable UI state of the screen.
 * @param SIDE_EFFECT the one-shot event consumed by the UI.
 *
 * @author WangZhiYao
 * @since 2025/9/12
 */
abstract class BaseAndroidMVIViewModel<STATE : Any, SIDE_EFFECT : Any>(
    application: Application
) : AndroidViewModel(application), MVIContainer<STATE, SIDE_EFFECT> {

    /**
     * The state the screen starts from.
     */
    protected abstract val initialState: STATE

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    override val uiState: StateFlow<STATE> = _state.asStateFlow()

    private val _sideEffect: MutableSharedFlow<SIDE_EFFECT> = MutableSharedFlow()
    override val sideEffect: SharedFlow<SIDE_EFFECT> = _sideEffect.asSharedFlow()

    private val intentContext = IntentContextImpl()

    /**
     * Launches [action] in [viewModelScope] on this ViewModel's single shared
     * [IntentContext].
     *
     * @param action the intent logic to execute.
     * @return the [Job] the intent runs in; cancel it to abort the intent.
     */
    override fun intent(
        action: suspend IntentContext<STATE, SIDE_EFFECT>.() -> Unit
    ): Job = viewModelScope.launch {
        intentContext.action()
    }

    /**
     * [IntentContext] implementation backed by this ViewModel's state and
     * side effect flows.
     */
    private inner class IntentContextImpl : IntentContext<STATE, SIDE_EFFECT> {
        override val state: STATE
            get() = _state.value

        override fun reduce(reducer: (STATE) -> STATE) {
            _state.update(reducer)
        }

        override suspend fun postSideEffect(sideEffect: SIDE_EFFECT) {
            _sideEffect.emit(sideEffect)
        }
    }
}
