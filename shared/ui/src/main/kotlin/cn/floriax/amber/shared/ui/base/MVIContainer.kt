package cn.floriax.amber.shared.ui.base

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVI (Model-View-Intent) contract between a screen's ViewModel and its UI.
 *
 * The UI renders the latest [uiState], consumes one-shot [sideEffect] events,
 * and dispatches user actions back through [intent].
 *
 * @param STATE the immutable UI state of the screen.
 * @param SIDE_EFFECT the one-shot event consumed by the UI, e.g. navigation or showing a toast.
 *
 * @author WangZhiYao
 * @since 2026/9/27
 */
interface MVIContainer<STATE : Any, SIDE_EFFECT : Any> {

    /**
     * The UI state to render. A hot stream that always replays the latest
     * state to new collectors.
     */
    val uiState: StateFlow<STATE>

    /**
     * One-shot events consumed by the UI, such as navigation or showing a
     * toast. Unlike [uiState], values are never replayed to late collectors.
     */
    val sideEffect: SharedFlow<SIDE_EFFECT>

    /**
     * Dispatch an intent for execution.
     *
     * [action] runs in an [IntentContext] where it can read the current state,
     * reduce it to a new state and post side effects.
     *
     * @param action the intent logic to execute.
     * @return the [Job] the intent runs in; cancel it to abort the intent.
     */
    fun intent(action: suspend IntentContext<STATE, SIDE_EFFECT>.() -> Unit): Job
}

/**
 * Provides a context for intent execution.
 * This context allows mutation of state and posting of side effects.
 *
 * @param STATE the immutable UI state of the screen.
 * @param SIDE_EFFECT the one-shot event consumed by the UI.
 */
interface IntentContext<STATE : Any, SIDE_EFFECT : Any> {

    /**
     * The current state at the time of intent execution.
     */
    val state: STATE

    /**
     * Reduce the current state to a new state.
     *
     * @param reducer a lambda that receives the current state and returns a new state.
     */
    fun reduce(reducer: STATE.() -> STATE)

    /**
     * Post a side effect to be handled by the UI.
     *
     * @param sideEffect the side effect to be posted.
     */
    suspend fun postSideEffect(sideEffect: SIDE_EFFECT)
}

