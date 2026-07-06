package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_table WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user_table WHERE id = 1")
    suspend fun getUserOnce(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE user_table SET coins = :coins WHERE id = 1")
    suspend fun updateCoins(coins: Long)

    @Query("UPDATE user_table SET discoveredVariantIds = :ids WHERE id = 1")
    suspend fun updateDiscoveredVariantIds(ids: String)
}