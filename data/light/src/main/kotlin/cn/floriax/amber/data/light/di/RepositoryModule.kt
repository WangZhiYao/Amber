package cn.floriax.amber.data.light.di

import cn.floriax.amber.data.light.repository.PresetRepositoryImpl
import cn.floriax.amber.domain.light.repository.PresetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Light data layer bindings: the preset repository on Room.
 *
 * @author WangZhiYao
 * @since 2026/10/1
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository
}
