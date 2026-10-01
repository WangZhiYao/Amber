package cn.floriax.amber.data.device.di

import cn.floriax.amber.core.ble.BleClient
import cn.floriax.amber.core.common.di.qualifier.ApplicationIOScope
import cn.floriax.amber.core.common.di.qualifier.ApplicationMainScope
import cn.floriax.amber.data.device.logger.FrameLogAggregator
import cn.floriax.amber.data.device.repository.BleDeviceScanner
import cn.floriax.amber.data.device.repository.ClockRepositoryImpl
import cn.floriax.amber.data.device.repository.DeviceRepositoryImpl
import cn.floriax.amber.domain.device.ClockRepository
import cn.floriax.amber.domain.device.DeviceManagerCoordinator
import cn.floriax.amber.domain.device.DeviceScanner
import cn.floriax.amber.domain.device.repository.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDateTime
import javax.inject.Singleton

/**
 * Hilt module: the clock repository factory and the domain-interface binding.
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindClockRepository(impl: ClockRepositoryImpl): ClockRepository

    @Binds
    @Singleton
    abstract fun bindDeviceScanner(impl: BleDeviceScanner): DeviceScanner

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    companion object {
        @Provides
        @Singleton
        fun provideClockRepository(
            ble: BleClient,
            logger: FrameLogAggregator,
            // The ApplicationMainScope (Main.immediate) single-threads the
            // repository's mutable state: session/generation fields are
            // touched on the main thread only, lock-free. It carries the
            // repository's long-lived collection jobs and backoff loops;
            // writes themselves run in the caller's context — the BLE
            // library is main-safe.
            @ApplicationMainScope scope: CoroutineScope,
        ): ClockRepositoryImpl = ClockRepositoryImpl(
            ble = ble,
            scope = scope,
            logger = logger,
            now = { LocalDateTime.now() },
            // TODO: read from PrefsRepository once preferences persistence lands.
            autoSync = { true },
        )

        /**
         * The device manager sheet coordinator: one shared instance for
         * the clock/light/settings pills. A plain domain class — the scope
         * it runs collection jobs on can only come from here (domain has
         * no DI framework), the same hand-off as ClockRepositoryImpl.
         */
        @Provides
        @Singleton
        fun provideDeviceManagerCoordinator(
            devices: DeviceRepository,
            scanner: DeviceScanner,
            clock: ClockRepository,
            @ApplicationIOScope scope: CoroutineScope,
        ): DeviceManagerCoordinator = DeviceManagerCoordinator(devices, scanner, clock, scope)
    }
}
