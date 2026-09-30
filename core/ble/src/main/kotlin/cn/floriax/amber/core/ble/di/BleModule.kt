package cn.floriax.amber.core.ble.di

import cn.floriax.amber.core.ble.BleClient
import cn.floriax.amber.core.ble.NordicBleManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * BLE binding: the BleClient abstraction → the Nordic implementation
 * (tests assemble their own fake in the test source set).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {

    @Binds
    @Singleton
    abstract fun bindBleClient(impl: NordicBleManager): BleClient
}
