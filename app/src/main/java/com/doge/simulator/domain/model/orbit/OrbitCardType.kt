package com.doge.simulator.domain.model.orbit

// ORBIT 1차 버전 8종 카드(총 17장). GLITCH(Power 0)는 확장 카드로 이번 스코프에서 제외한다.
enum class OrbitCardType(val power: Int, val count: Int) {
    SCOUT_DRONE(power = 1, count = 4),
    SENSOR(power = 2, count = 3),
    PROBE(power = 3, count = 2),
    SHIELD(power = 4, count = 2),
    EMP(power = 5, count = 2),
    WARP_GATE(power = 6, count = 2),
    AI_CORE(power = 7, count = 1),
    CAPTAIN(power = 8, count = 1);

    companion object {
        const val TOTAL_CARD_COUNT = 17

        fun buildFullDeck(): List<OrbitCardType> =
            entries.flatMap { type -> List(type.count) { type } }
    }
}
