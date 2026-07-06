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
}
