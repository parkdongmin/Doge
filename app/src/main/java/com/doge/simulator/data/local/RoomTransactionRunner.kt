package com.doge.simulator.data.local

import androidx.room.withTransaction
import com.doge.simulator.domain.repository.TransactionRunner

class RoomTransactionRunner(private val db: PlanetDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
