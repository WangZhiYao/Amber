package cn.floriax.amber.core.common.di.qualifier

import javax.inject.Qualifier

/**
 * Qualifier for the application-wide IO coroutine scope
 * (SupervisorJob + IO dispatcher): long-lived background work such as BLE
 * collection jobs and backoff loops.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationIOScope

/**
 * Qualifier for the application-wide Main coroutine scope
 * (SupervisorJob + Main.immediate): single-threaded, UI-adjacent state
 * machines such as the repository's session/generation bookkeeping.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationMainScope
