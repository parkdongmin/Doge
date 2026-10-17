package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doge.simulator.data.local.entity.StoryProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryProgressDao {
    @Query("SELECT * FROM story_progress_table WHERE id = 1")
    fun get(): Flow<StoryProgressEntity?>

    @Query("SELECT * FROM story_progress_table WHERE id = 1")
    suspend fun getOnce(): StoryProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StoryProgressEntity)

    // 한 칸만 켠다 — 진행 전체를 덮어쓰면 그사이 탐사 기록이 바꾼 진행이 되돌아갈 수 있다
    @Query("UPDATE story_progress_table SET part2IntroShown = 1 WHERE id = 1")
    suspend fun markPart2IntroShown()
}
