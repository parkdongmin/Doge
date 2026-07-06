package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query("SELECT * FROM resource_table")
    fun getAll(): Flow<List<ResourceEntity>>

    @Query("SELECT amount FROM resource_table WHERE type = :type")
    suspend fun getAmount(type: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resource: ResourceEntity)

    @Query("UPDATE resource_table SET amount = amount + :delta WHERE type = :type")
    suspend fun addAmount(type: String, delta: Long)
}
