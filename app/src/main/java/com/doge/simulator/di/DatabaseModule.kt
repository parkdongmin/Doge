package com.doge.simulator.di

import android.content.Context
import androidx.room.Room
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.repository.PlanetRepositoryImpl
import com.doge.simulator.domain.repository.PlanetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(context: Context): PlanetDatabase {
        return Room.databaseBuilder(
            context,
            PlanetDatabase::class.java,
            "planet_db"
        ).build()
    }

    @Provides
    fun providePlanetDao(db: PlanetDatabase) = db.planetDao()

    @Provides
    @Singleton
    fun providePlanetRepository(
        dao: PlanetDao,
    ): PlanetRepository {
        return PlanetRepositoryImpl(dao)
    }
}