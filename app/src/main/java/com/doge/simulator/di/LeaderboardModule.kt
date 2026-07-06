package com.doge.simulator.di

import com.doge.simulator.data.repository.LeaderboardRepositoryImpl
import com.doge.simulator.domain.repository.LeaderboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LeaderboardModule {

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(
        impl: LeaderboardRepositoryImpl
    ): LeaderboardRepository
}