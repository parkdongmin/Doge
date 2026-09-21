package com.doge.simulator.domain.ai

import com.doge.simulator.domain.model.orbit.OrbitCard

// B-01이 플레이어의 손패에 대해 "확실히 아는" 정보만 보관한다. 실제 손패를 직접 열람하는
// 치팅은 하지 않으며(FR-010), Sensor로 확인했거나 Warp Gate로 교환해서 정확히 아는 경우에만
// 값을 갖는다. 플레이어가 자기 턴을 마치거나(드로우+사용으로 손패 구성이 바뀜) EMP로 카드가
// 교체되면 더 이상 확신할 수 없으므로 즉시 모른다(forget)로 되돌린다.
class B01Memory {
    var knownPlayerCard: OrbitCard? = null
        private set

    fun reveal(card: OrbitCard) {
        knownPlayerCard = card
    }

    fun forget() {
        knownPlayerCard = null
    }
}
