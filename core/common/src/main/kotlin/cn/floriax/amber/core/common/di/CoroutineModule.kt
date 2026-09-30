package cn.floriax.amber.core.common.di

import cn.floriax.amber.core.common.di.qualifier.ApplicationIOScope
import cn.floriax.amber.core.common.di.qualifier.ApplicationMainScope
import cn.floriax.amber.core.common.di.qualifier.IODispatcher
import cn.floriax.amber.core.common.di.qualifier.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provides the app's coroutine dispatchers and named coroutine scopes.
 * Scopes are composed from the qualified dispatchers (R50-style).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    @IODispatcher
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    @Provides
    @Singleton
    @ApplicationIOScope
    fun provideApplicationIOScope(@IODispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(ioDispatcher + SupervisorJob())

    @Provides
    @Singleton
    @ApplicationMainScope
    fun provideApplicationMainScope(@MainDispatcher mainDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(mainDispatcher + SupervisorJob())
}
