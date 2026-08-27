package com.doge.simulator.di

import android.content.Context
import androidx.room.Room
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.local.dao.*
import com.doge.simulator.data.repository.*
import com.doge.simulator.domain.repository.*
import com.doge.simulator.data.repository.StoryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlanetDatabase {
        return Room.databaseBuilder(context, PlanetDatabase::class.java, "planet_db")
            .addMigrations(
                PlanetDatabase.MIGRATION_1_2,
                PlanetDatabase.MIGRATION_2_3,
                PlanetDatabase.MIGRATION_3_4,
                PlanetDatabase.MIGRATION_4_5,
                PlanetDatabase.MIGRATION_5_6,
                PlanetDatabase.MIGRATION_6_7,
                PlanetDatabase.MIGRATION_7_8,
                PlanetDatabase.MIGRATION_8_9,
                PlanetDatabase.MIGRATION_9_10,
                PlanetDatabase.MIGRATION_10_11,
                PlanetDatabase.MIGRATION_11_12,
                PlanetDatabase.MIGRATION_12_13,
                PlanetDatabase.MIGRATION_13_14,
                PlanetDatabase.MIGRATION_14_15,
                PlanetDatabase.MIGRATION_15_16,
                PlanetDatabase.MIGRATION_16_17
            )
            .build()
    }

    // ── DAOs ──────────────────────────────────────────────────────────
    @Provides fun providePlanetDao(db: PlanetDatabase): PlanetDao = db.planetDao()
    @Provides fun provideUserDao(db: PlanetDatabase): UserDao = db.userDao()
    @Provides fun provideAstronautDao(db: PlanetDatabase): AstronautDao = db.astronautDao()
    @Provides fun provideSpaceshipDao(db: PlanetDatabase): SpaceshipDao = db.spaceshipDao()
    @Provides fun provideExpeditionDao(db: PlanetDatabase): ExpeditionDao = db.expeditionDao()
    @Provides fun provideResourceDao(db: PlanetDatabase): ResourceDao = db.resourceDao()
    @Provides fun provideResearchLabDao(db: PlanetDatabase): ResearchLabDao = db.researchLabDao()
    @Provides fun provideStoryProgressDao(db: PlanetDatabase): StoryProgressDao = db.storyProgressDao()
    @Provides fun provideExpeditionReportDao(db: PlanetDatabase): ExpeditionReportDao = db.expeditionReportDao()
    @Provides fun provideRecruitmentDao(db: PlanetDatabase): RecruitmentDao = db.recruitmentDao()
    @Provides fun providePlanetEventLogDao(db: PlanetDatabase): PlanetEventLogDao = db.planetEventLogDao()
    @Provides fun provideSnapshotDao(db: PlanetDatabase): SnapshotDao = db.snapshotDao()

    // ── Repositories ──────────────────────────────────────────────────
    @Provides @Singleton
    fun providePlanetRepository(dao: PlanetDao): PlanetRepository = PlanetRepositoryImpl(dao)

    @Provides @Singleton
    fun provideUserRepository(dao: UserDao): UserRepository = UserRepositoryImpl(dao)

    @Provides @Singleton
    fun provideAstronautRepository(dao: AstronautDao): AstronautRepository = AstronautRepositoryImpl(dao)

    @Provides @Singleton
    fun provideSpaceshipRepository(dao: SpaceshipDao): SpaceshipRepository = SpaceshipRepositoryImpl(dao)

    @Provides @Singleton
    fun provideExpeditionRepository(dao: ExpeditionDao): ExpeditionRepository = ExpeditionRepositoryImpl(dao)

    @Provides @Singleton
    fun provideResourceRepository(dao: ResourceDao): ResourceRepository = ResourceRepositoryImpl(dao)

    @Provides @Singleton
    fun provideResearchLabRepository(dao: ResearchLabDao): ResearchLabRepository = ResearchLabRepositoryImpl(dao)

    @Provides @Singleton
    fun provideStoryRepository(
        progressDao: StoryProgressDao,
        reportDao: ExpeditionReportDao
    ): StoryRepository = StoryRepositoryImpl(progressDao, reportDao)

    @Provides @Singleton
    fun provideRecruitmentRepository(dao: RecruitmentDao): RecruitmentRepository = RecruitmentRepositoryImpl(dao)

    @Provides @Singleton
    fun providePlanetEventLogRepository(dao: PlanetEventLogDao): PlanetEventLogRepository = PlanetEventLogRepositoryImpl(dao)
}
