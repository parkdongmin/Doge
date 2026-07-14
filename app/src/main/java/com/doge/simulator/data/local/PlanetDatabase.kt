package com.doge.simulator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doge.simulator.data.local.dao.AstronautDao
import com.doge.simulator.data.local.dao.ExpeditionDao
import com.doge.simulator.data.local.dao.ExpeditionReportDao
import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.local.dao.RecruitmentDao
import com.doge.simulator.data.local.dao.ResearchLabDao
import com.doge.simulator.data.local.dao.ResourceDao
import com.doge.simulator.data.local.dao.SpaceshipDao
import com.doge.simulator.data.local.dao.StoryProgressDao
import com.doge.simulator.data.local.dao.UserDao
import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.data.local.entity.ExpeditionReportEntity
import com.doge.simulator.data.local.entity.PlanetEntity
import com.doge.simulator.data.local.entity.RecruitmentCandidateEntity
import com.doge.simulator.data.local.entity.RecruitmentMetaEntity
import com.doge.simulator.data.local.entity.ResearchLabEntity
import com.doge.simulator.data.local.entity.ResourceEntity
import com.doge.simulator.data.local.entity.SpaceshipEntity
import com.doge.simulator.data.local.entity.StoryEventEntity
import com.doge.simulator.data.local.entity.StoryProgressEntity
import com.doge.simulator.data.local.entity.UserEntity

@Database(
    entities = [
        PlanetEntity::class,
        UserEntity::class,
        AstronautEntity::class,
        SpaceshipEntity::class,
        ExpeditionEntity::class,
        ResourceEntity::class,
        ResearchLabEntity::class,
        StoryProgressEntity::class,
        ExpeditionReportEntity::class,
        StoryEventEntity::class,
        RecruitmentCandidateEntity::class,
        RecruitmentMetaEntity::class,
    ],
    version = 14,
    exportSchema = false
)
abstract class PlanetDatabase : RoomDatabase() {
    abstract fun planetDao(): PlanetDao
    abstract fun userDao(): UserDao
    abstract fun astronautDao(): AstronautDao
    abstract fun spaceshipDao(): SpaceshipDao
    abstract fun expeditionDao(): ExpeditionDao
    abstract fun resourceDao(): ResourceDao
    abstract fun researchLabDao(): ResearchLabDao
    abstract fun storyProgressDao(): StoryProgressDao
    abstract fun expeditionReportDao(): ExpeditionReportDao
    abstract fun recruitmentDao(): RecruitmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_table` (`id` INTEGER NOT NULL, `coins` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE planet_table ADD COLUMN maintenanceCost INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE planet_table ADD COLUMN status TEXT NOT NULL DEFAULT 'NORMAL'")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `event_log_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `planetId` TEXT,
                        `planetType` TEXT,
                        `planetDisplayName` TEXT,
                        `description` TEXT NOT NULL,
                        `valueBefore` INTEGER,
                        `valueAfter` INTEGER,
                        `coinDelta` INTEGER,
                        `occurredAt` INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `market_event_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `targetPlanetType` TEXT NOT NULL,
                        `isPositive` INTEGER NOT NULL,
                        `changeRate` REAL NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `occurredAt` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE planet_table ADD COLUMN variantId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `astronaut_table` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `specialty` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `trainingEndTime` INTEGER,
                        `hiredAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `spaceship_table` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `crewCapacity` INTEGER NOT NULL,
                        `speed` INTEGER NOT NULL,
                        `cargo` INTEGER NOT NULL,
                        `successRate` REAL NOT NULL,
                        `purchasedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `expedition_table` (
                        `id` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `tier` INTEGER NOT NULL,
                        `astronautIds` TEXT NOT NULL,
                        `spaceshipId` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `resourcesResult` TEXT,
                        `discoveredPlanetType` TEXT,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `resource_table` (
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        PRIMARY KEY(`type`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `research_lab_table` (
                        `id` INTEGER NOT NULL,
                        `explorationTechLevel` INTEGER NOT NULL,
                        `celestialAnalysisLevel` INTEGER NOT NULL,
                        `hrLevel` INTEGER NOT NULL,
                        `engineeringLevel` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                // 연구소 초기 데이터 삽입
                database.execSQL(
                    "INSERT OR IGNORE INTO `research_lab_table` VALUES (1, 1, 1, 1, 1)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `story_progress_table` (
                        `id` INTEGER NOT NULL,
                        `totalRecordsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `currentChapter` INTEGER NOT NULL DEFAULT 1,
                        `firstRuinsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `firstAlienCivCompleted` INTEGER NOT NULL DEFAULT 0,
                        `firstT3Completed` INTEGER NOT NULL DEFAULT 0,
                        `firstT5Completed` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `expedition_report_table` (
                        `expeditionId` TEXT NOT NULL,
                        `recordNumber` INTEGER NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `chapterTitle` TEXT NOT NULL,
                        `recordTitle` TEXT NOT NULL,
                        `isChapterEnding` INTEGER NOT NULL DEFAULT 0,
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`expeditionId`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `story_event_table` (
                        `id` TEXT NOT NULL,
                        `expeditionId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `choice1Label` TEXT NOT NULL,
                        `choice1ResourceType` TEXT NOT NULL,
                        `choice1Amount` INTEGER NOT NULL,
                        `choice2Label` TEXT NOT NULL,
                        `choice2ResourceType` TEXT NOT NULL,
                        `choice2Amount` INTEGER NOT NULL,
                        `choice3Label` TEXT,
                        `choice3ResourceType` TEXT,
                        `choice3Amount` INTEGER,
                        `selectedChoiceIndex` INTEGER NOT NULL DEFAULT -1,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // planet_table 재생성 (status, maintenanceCost 제거 / upgradeInvestment 추가)
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `planet_table_new` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `production` INTEGER NOT NULL,
                        `risk` INTEGER NOT NULL,
                        `investment` INTEGER NOT NULL,
                        `eventRate` INTEGER NOT NULL,
                        `buyPrice` INTEGER NOT NULL,
                        `acquireTime` INTEGER NOT NULL,
                        `currentValue` INTEGER NOT NULL,
                        `level` INTEGER NOT NULL,
                        `totalProfit` INTEGER NOT NULL,
                        `variantId` TEXT NOT NULL DEFAULT '',
                        `upgradeInvestment` INTEGER NOT NULL DEFAULT 0,
                        `lastProfitTime` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """INSERT INTO `planet_table_new`
                        (id, type, production, risk, investment, eventRate, buyPrice, acquireTime,
                         currentValue, level, totalProfit, variantId, lastProfitTime)
                       SELECT id, type, production, risk, investment, eventRate, buyPrice, acquireTime,
                              currentValue, level, totalProfit, variantId, lastProfitTime
                       FROM `planet_table`"""
                )
                database.execSQL("DROP TABLE `planet_table`")
                database.execSQL("ALTER TABLE `planet_table_new` RENAME TO `planet_table`")

                // market_event_table 제거
                database.execSQL("DROP TABLE IF EXISTS `market_event_table`")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE story_event_table ADD COLUMN isDeparture INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE story_event_table ADD COLUMN outcomeNote TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_table ADD COLUMN discoveredVariantIds TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 기존에 이미 완료된 탐사는 결과를 다시 띄우지 않도록 기본값 1(처리됨)로 채운다
                database.execSQL(
                    "ALTER TABLE expedition_table ADD COLUMN resultHandled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE expedition_table ADD COLUMN coinsEarned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // astronaut_table 재생성 (level 제거 / grade, proficiency, trainingType 추가)
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `astronaut_table_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `specialty` TEXT NOT NULL,
                        `grade` TEXT NOT NULL DEFAULT 'INTERN',
                        `proficiency` INTEGER NOT NULL DEFAULT 10,
                        `status` TEXT NOT NULL,
                        `trainingEndTime` INTEGER,
                        `trainingType` TEXT,
                        `hiredAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """INSERT INTO `astronaut_table_new`
                        (id, name, specialty, grade, proficiency, status, trainingEndTime, trainingType, hiredAt)
                       SELECT id, name, specialty, 'INTERN', MIN(level * 5, 50), status, trainingEndTime, NULL, hiredAt
                       FROM `astronaut_table`"""
                )
                database.execSQL("DROP TABLE `astronaut_table`")
                database.execSQL("ALTER TABLE `astronaut_table_new` RENAME TO `astronaut_table`")

                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recruitment_candidate_table` (
                        `slotIndex` INTEGER NOT NULL,
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `specialty` TEXT NOT NULL,
                        `grade` TEXT NOT NULL,
                        `proficiency` INTEGER NOT NULL,
                        PRIMARY KEY(`slotIndex`)
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `recruitment_meta_table` (
                        `id` INTEGER NOT NULL,
                        `lastRefreshTime` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // event_log_table 제거 (아무 화면에서도 읽지 않는 로그 전용 테이블이었음)
                database.execSQL("DROP TABLE IF EXISTS `event_log_table`")
            }
        }
    }
}
