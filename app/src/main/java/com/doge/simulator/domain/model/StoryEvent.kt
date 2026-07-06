package com.doge.simulator.domain.model

data class StoryEvent(
    val id: String,
    val expeditionId: String,
    val title: String,
    val description: String,
    val choice1Label: String,
    val choice1ResourceType: ResourceType,
    val choice1Amount: Long,
    val choice2Label: String,
    val choice2ResourceType: ResourceType,
    val choice2Amount: Long,
    val choice3Label: String? = null,
    val choice3ResourceType: ResourceType? = null,
    val choice3Amount: Long? = null,
    // -1 = 미선택, 0/1/2 = 선택된 선택지 인덱스
    val selectedChoiceIndex: Int = -1,
    // 탐사 보고서 맨 끝에 항상 붙는 "떠나기 / 자원 싣고 떠나기" 선택인지 여부.
    // true인 이벤트에서 choice2(자원 싣기)를 고르면 후속 이벤트가 하나 더 생성된다.
    val isDeparture: Boolean = false,
    // null이 아니면 선택 결과가 보상이 아니라 이 문구(실패 등)로 표시된다
    val outcomeNote: String? = null
) {
    val isPending: Boolean get() = selectedChoiceIndex == -1

    fun choiceAt(index: Int): Triple<String, ResourceType, Long>? = when (index) {
        0 -> Triple(choice1Label, choice1ResourceType, choice1Amount)
        1 -> Triple(choice2Label, choice2ResourceType, choice2Amount)
        2 -> if (choice3Label != null && choice3ResourceType != null && choice3Amount != null)
            Triple(choice3Label, choice3ResourceType, choice3Amount) else null
        else -> null
    }
}
