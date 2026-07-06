package com.doge.simulator.domain.model

data class EventLog(
    val id: Long = 0,
    val type: EventLogType,
    val planetId: String? = null,
    val planetType: String? = null,
    val planetDisplayName: String? = null,
    val description: String,
    val valueBefore: Int? = null,
    val valueAfter: Int? = null,
    val coinDelta: Long? = null,
    val occurredAt: Long = System.currentTimeMillis()
)
