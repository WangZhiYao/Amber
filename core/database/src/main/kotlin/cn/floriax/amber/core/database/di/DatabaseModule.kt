package cn.floriax.amber.core.database.di

import android.content.Context
import androidx.room.Room
import cn.floriax.amber.core.database.AmberDatabase
import cn.floriax.amber.core.database.dao.ClockDeviceDao
import cn.floriax.amber.core.database.dao.PresetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database provisioning: one in-process [AmberDatabase], DAOs exposed
 * per feature area (the same pattern as :core:ble's BleModule).
 *
 * @author WangZhiYao
 * @since 2026/9/30
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AmberDatabase =
        Room.databaseBuilder(context, AmberDatabase::class.java, "amber.db")
            .build()

    @Provides
    fun provideClockDeviceDao(database: AmberDatabase): ClockDeviceDao =
        database.clockDeviceDao()

    @Provides
    fun providePresetDao(database: AmberDatabase): PresetDao =
        database.presetDao()
}
