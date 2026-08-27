package com.doge.simulator.di

import com.doge.simulator.data.repository.CloudSaveRepositoryImpl
import com.doge.simulator.domain.repository.CloudSaveRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudSaveModule {

    @Binds
    @Singleton
    abstract fun bindCloudSaveRepository(
        impl: CloudSaveRepositoryImpl
    ): CloudSaveRepository
}
