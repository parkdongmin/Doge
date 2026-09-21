package com.doge.simulator.domain.model.orbit

import kotlin.random.Random

// 라운드 1회 동안 사용되는 카드 더미. usedCards는 CPU(B01OrbitAi) 판단용 내부 상태이며
// 플레이어 UI에는 지속적으로 노출하지 않는다(FR-004) — 프레젠테이션 계층에서 이 값을
// 구독해 지속 로그로 노출하지 말 것 (research.md #6 참고).
class OrbitDeck internal constructor(
    private val drawPile: MutableList<OrbitCard>
) {
    private val _usedCards = mutableListOf<OrbitCard>()

    val remainingCount: Int get() = drawPile.size

    fun usedCardsSnapshot(): List<OrbitCard> = _usedCards.toList()

    fun draw(): OrbitCard? = if (drawPile.isEmpty()) null else drawPile.removeAt(drawPile.size - 1)

    fun markUsed(card: OrbitCard) {
        _usedCards += card
    }

    companion object {
        fun newShuffled(random: Random = Random.Default): OrbitDeck {
            val all = OrbitCardType.buildFullDeck().map { OrbitCard(it) }.shuffled(random)
            return OrbitDeck(all.toMutableList())
        }

        // 테스트 전용: 정해진 순서의 카드로 덱을 구성한다. 리스트의 마지막 원소부터 draw()된다.
        fun forTest(cards: List<OrbitCardType>): OrbitDeck =
            OrbitDeck(cards.map { OrbitCard(it) }.toMutableList())
    }
}
