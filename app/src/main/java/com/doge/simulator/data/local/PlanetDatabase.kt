package com.doge.simulator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.local.entity.PlanetEntity

@Database(
    entities = [PlanetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlanetDatabase : RoomDatabase() {
    abstract fun planetDao(): PlanetDao
}