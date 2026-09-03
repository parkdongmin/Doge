package com.doge.simulator.data.local

import android.content.Context
import androidx.core.content.edit
import com.doge.simulator.domain.model.ObtainedResource
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.RarityTier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExplorationStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("exploration_prefs", Context.MODE_PRIVATE)

    fun saveSession(startTime: Long, endTime: Long, planet: Planet, resources: List<ObtainedResource>) {
        prefs.edit {
            putBoolean("active", true)
            putLong("start_time", startTime)
            putLong("end_time", endTime)
            putString("planet_id", planet.id)
            putString("planet_type", planet.type.name)
            putString("planet_variant_id", planet.variantId)
            putInt("planet_buy_price", planet.buyPrice)
            putInt("planet_production", planet.production)
            putInt("planet_risk", planet.risk)
            putInt("planet_investment", planet.investment)
            putInt("planet_event_rate", planet.eventRate)
            putString("resources", serializeResources(resources))
        }
    }

    fun hasActiveSession(): Boolean = prefs.getBoolean("active", false)

    fun getStartTime(): Long = prefs.getLong("start_time", 0L)
    fun getEndTime(): Long = prefs.getLong("end_time", 0L)

    fun getPlanet(): Planet? {
        if (!hasActiveSession()) return null
        val typeStr = prefs.getString("planet_type", null) ?: return null
        return try {
            Planet(
                id = prefs.getString("planet_id", "") ?: "",
                type = PlanetType.valueOf(typeStr),
                variantId = prefs.getString("planet_variant_id", "") ?: "",
                buyPrice = prefs.getInt("planet_buy_price", 0),
                production = prefs.getInt("planet_production", 0),
                risk = prefs.getInt("planet_risk", 0),
                investment = prefs.getInt("planet_investment", 0),
                eventRate = prefs.getInt("planet_event_rate", 0)
            )
        } catch (e: Exception) { null }
    }

    fun getResources(): List<ObtainedResource> {
        val str = prefs.getString("resources", null) ?: return emptyList()
        return deserializeResources(str)
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun serializeResources(resources: List<ObtainedResource>): String =
        resources.joinToString("|") { "${it.id}:${it.name}:${it.rarity.name}:${it.amount}" }

    private fun deserializeResources(str: String): List<ObtainedResource> {
        if (str.isBlank()) return emptyList()
        return str.split("|").mapNotNull { part ->
            val f = part.split(":")
            if (f.size == 4) runCatching {
                ObtainedResource(f[0], f[1], RarityTier.valueOf(f[2]), f[3].toInt())
            }.getOrNull() else null
        }
    }
}
