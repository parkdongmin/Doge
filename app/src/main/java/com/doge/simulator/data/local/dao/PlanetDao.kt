package com.doge.simulator.data.local.dao

import androidx.room.*

import com.doge.simulator.data.local.entity.PlanetEntity

@Dao
interface PlanetDao {

    @Query("SELECT * FROM planet_table")
    suspend fun getOwnedPlanets(): List<PlanetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanet(planet: PlanetEntity)

    @Query("DELETE FROM planet_table WHERE id = :planetId")
    suspend fun deletePlanet(planetId: String)

    @Query("UPDATE planet_table SET currentValue = :newValue WHERE id = :planetId")
    suspend fun updateValue(planetId: String, newValue: Int)
}