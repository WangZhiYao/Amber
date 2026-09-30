package cn.floriax.amber.core.common.di.qualifier

import javax.inject.Qualifier

/**
 * Qualifier for the IO [kotlinx.coroutines.CoroutineDispatcher].
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IODispatcher

/**
 * Qualifier for the Main [kotlinx.coroutines.CoroutineDispatcher]
 * (Dispatchers.Main.immediate).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainDispatcher
