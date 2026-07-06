package com.doge.simulator.domain.model

import java.util.UUID

object StoryContent {

    // ── 챕터별 기록 제목 풀 ────────────────────────────────────────────
    // 챕터 내 기록 수를 인덱스로 사이클 (% size)
    // 마지막 원소는 챕터 전환 클리프행어 (isChapterEnding = true 시 사용)
    val recordTitlePool: Map<Int, List<String>> = mapOf(
        1 to listOf(
            "황량한 암석 행성. 생명의 흔적은 없었다",
            "버려진 탐사선 잔해를 발견했다. 우리 것이 아니다",
            "알 수 없는 주파수가 잡혔다. 패턴이 있다",
            "기지 터에서 타 버린 장비들. 누가, 왜?",
            "행성 표면에 거대한 선형 구조물이 있다",
            "센서가 이상한 반응을 보였다. 일시적인 오류다",
            "보급품 일부가 사라졌다. 대원들은 모두 부인한다",
            "귀환 도중 통신이 잠시 끊겼다. 이유를 모른다"
        ),
        2 to listOf(
            "그 주파수가 다시 잡혔다. 더 강해졌다",
            "신호의 방향을 추적했다. 알파 성계 방향이다",
            "이상한 구조물을 발견했다. 자연물이 아니다",
            "구조물 내부에서 문자를 발견했다. 해독 중",
            "대원 한 명이 며칠째 조용하다",
            "같은 행성에서 같은 신호가 두 번 잡혔다",
            "신호가 우리를 향해 방향을 바꿨다",
            "유적 속 장치가 우리 접근에 반응했다"
        ),
        3 to listOf(
            "그림자가 보였다. 우주복 형태였다",
            "통신을 시도했다. 응답이 없다",
            "그들이 먼저 접근해왔다. 무기는 없었다",
            "언어는 달랐지만, 의도는 읽혔다",
            "교환을 제안해왔다. 뭘 원하는지 몰랐다",
            "같은 별을 바라보고 있었다. 우연이 아닐 수 있다",
            "대원 한 명이 그들을 따라가려 했다",
            "그들은 우리 기술에 전혀 관심이 없었다"
        ),
        4 to listOf(
            "그들의 행성에 초대받았다",
            "우리가 모르는 기술이 있었다",
            "무언가를 요구하고 있다",
            "대원 한 명이 돌아오지 않았다",
            "협상 테이블이 만들어졌다",
            "그들이 먼저 선물을 보내왔다",
            "우리 중 누군가와 이미 접촉했다는 증거가 있다",
            "침묵이 이어졌다. 서로 기다리고 있다"
        ),
        5 to listOf(
            "지도에 없는 성계로 신호가 이어진다",
            "그곳엔 행성이 없다. 무언가가 행성을 지웠다",
            "고대 유물에서 같은 문자를 발견했다",
            "진실에 가까워질수록 대원들이 불안해한다",
            "빛이 없는 곳에서 뭔가가 움직였다",
            "귀환 시간이 계산과 맞지 않는다",
            "우주의 크기가 우리가 알던 것과 다를 수 있다",
            "처음부터 찾는 것이 아니라 찾아지고 있었다"
        )
    )

    // 챕터 전환 시 클리프행어 문장
    val chapterEndingTitles: Map<Int, String> = mapOf(
        1 to "이 행성에... 우리만 있는 게 아닌데?",
        2 to "그들은... 우리가 오기를 기다리고 있었다",
        3 to "적이 아니다. 하지만 친구도 아니다",
        4 to "우리는 이미 선택을 강요받고 있었다",
        5 to "이건 탐사가 아니었다. 처음부터"
    )

    // ── 카테고리별 이벤트 풀 ─────────────────────────────────────────
    data class EventTemplate(
        val title: String,
        val description: String,
        val choice1Label: String,
        val choice1Type: ResourceType,
        val choice1Amount: Long,
        val choice2Label: String,
        val choice2Type: ResourceType,
        val choice2Amount: Long,
        val choice3Label: String? = null,
        val choice3Type: ResourceType? = null,
        val choice3Amount: Long? = null
    )

    val eventPool: Map<ExpeditionCategory, List<EventTemplate>> = mapOf(
        ExpeditionCategory.MINERAL to listOf(
            EventTemplate(
                title = "채굴 중 이상한 결정체를 발견했습니다",
                description = "암반 깊숙이 박혀있는 알 수 없는 결정체. 이전에 본 적 없는 형태다.",
                choice1Label = "표본으로 가져간다",
                choice1Type = ResourceType.RARE_EARTH, choice1Amount = 5L,
                choice2Label = "제자리에 둔다",
                choice2Type = ResourceType.IRON_ORE, choice2Amount = 8L
            ),
            EventTemplate(
                title = "지하 공동(空洞)이 발견됐습니다",
                description = "지표 아래 거대한 빈 공간이 있다. 자연적으로 형성된 것이 아닐 수 있다.",
                choice1Label = "진입한다",
                choice1Type = ResourceType.CRYSTAL, choice1Amount = 6L,
                choice2Label = "안전하게 봉쇄한다",
                choice2Type = ResourceType.MAGMA_STONE, choice2Amount = 8L
            ),
            EventTemplate(
                title = "채굴 장비가 갑자기 멈췄습니다",
                description = "원인 불명의 장비 오류. 이 지역에만 특이한 자기장이 형성돼 있다.",
                choice1Label = "직접 수리한다",
                choice1Type = ResourceType.IRON_ORE, choice1Amount = 4L,
                choice2Label = "본부에 보고한다",
                choice2Type = ResourceType.RARE_EARTH, choice2Amount = 5L
            )
        ),
        ExpeditionCategory.PLANET to listOf(
            EventTemplate(
                title = "예상치 못한 생명체 흔적을 발견했습니다",
                description = "대기 조성 분석 중 유기물 패턴이 감지됐다. 단순한 오염일 수도 있다.",
                choice1Label = "샘플을 채취한다",
                choice1Type = ResourceType.BIOMASS, choice1Amount = 8L,
                choice2Label = "관찰만 한다",
                choice2Type = ResourceType.LIFE_CRYSTAL, choice2Amount = 5L
            ),
            EventTemplate(
                title = "행성 대기가 불안정해지고 있습니다",
                description = "예보에 없던 이온 폭풍이 접근 중. 철수하거나 데이터를 더 모을 시간이 있다.",
                choice1Label = "즉시 철수한다",
                choice1Type = ResourceType.COOLANT, choice1Amount = 6L,
                choice2Label = "데이터를 더 모은다",
                choice2Type = ResourceType.ENERGY_CORE, choice2Amount = 5L
            ),
            EventTemplate(
                title = "미확인 에너지 반응이 감지됐습니다",
                description = "행성 핵 근처에서 비정상적인 에너지 파동이 관측되고 있다.",
                choice1Label = "에너지원을 찾는다",
                choice1Type = ResourceType.ENERGY_CORE, choice1Amount = 8L,
                choice2Label = "안전 거리를 유지한다",
                choice2Type = ResourceType.BIOMASS, choice2Amount = 6L
            )
        ),
        ExpeditionCategory.RUINS to listOf(
            EventTemplate(
                title = "유적 내부에서 봉인된 방을 발견했습니다",
                description = "수천 년간 열리지 않은 것으로 보이는 문. 개방하면 내부 상태를 알 수 없다.",
                choice1Label = "개방한다",
                choice1Type = ResourceType.ANCIENT_ARTIFACT, choice1Amount = 5L,
                choice2Label = "기록만 남긴다",
                choice2Type = ResourceType.DATA_CORE, choice2Amount = 6L
            ),
            EventTemplate(
                title = "고대 장치가 작동을 시작했습니다",
                description = "우리가 접근하자 수면 상태에 있던 장치가 저절로 켜졌다. 반응이 의도적으로 보인다.",
                choice1Label = "함께 작동시킨다",
                choice1Type = ResourceType.NANOBOT, choice1Amount = 8L,
                choice2Label = "전원을 끊는다",
                choice2Type = ResourceType.DATA_CORE, choice2Amount = 5L
            ),
            EventTemplate(
                title = "누군가 최근 이 유적을 방문한 흔적이 있습니다",
                description = "먼지 속에 발자국이 있다. 우리 팀의 것이 아니다. 며칠 되지 않은 것 같다.",
                choice1Label = "흔적을 추적한다",
                choice1Type = ResourceType.ANCIENT_ARTIFACT, choice1Amount = 8L,
                choice2Label = "즉시 본부에 보고한다",
                choice2Type = ResourceType.DATA_CORE, choice2Amount = 5L
            )
        ),
        ExpeditionCategory.ALIEN_CIVILIZATION to listOf(
            EventTemplate(
                title = "외계 생명체와 마주쳤습니다",
                description = "우주복과 유사한 장비를 착용한 존재들. 무기는 보이지 않는다. 우리를 바라보고 있다.",
                choice1Label = "먼저 인사한다",
                choice1Type = ResourceType.ALIEN_TECH, choice1Amount = 8L,
                choice2Label = "조용히 물러난다",
                choice2Type = ResourceType.UNKNOWN_MATTER, choice2Amount = 6L,
                choice3Label = "전투 태세를 취한다",
                choice3Type = ResourceType.QUANTUM_CORE, choice3Amount = 4L
            ),
            EventTemplate(
                title = "외계 기지의 문이 열려 있습니다",
                description = "평소에는 닫혀 있던 구조물의 입구가 열려 있다. 우리를 위한 것일 수도 있다.",
                choice1Label = "조심스럽게 진입한다",
                choice1Type = ResourceType.QUANTUM_CORE, choice1Amount = 6L,
                choice2Label = "외부에서 관찰한다",
                choice2Type = ResourceType.ALIEN_TECH, choice2Amount = 5L
            ),
            EventTemplate(
                title = "외계 언어로 된 메시지를 받았습니다",
                description = "통신 장비가 알 수 없는 주파수를 수신했다. 반복되는 패턴이 있다. 의도적인 신호다.",
                choice1Label = "응답한다",
                choice1Type = ResourceType.UNKNOWN_MATTER, choice1Amount = 8L,
                choice2Label = "수신만 한다",
                choice2Type = ResourceType.ALIEN_TECH, choice2Amount = 6L
            )
        )
    )

    // ── 탐사 마무리 선택 ───────────────────────────────────────────────
    // 성공한 탐사 보고서 맨 끝에 항상 붙는 "떠나기 / 자원 싣고 떠나기" 선택지
    data class DepartureTemplate(
        val title: String,
        val description: String,
        val leaveLabel: String = "행성을 떠난다",
        val lootLabel: String,
        val lootType: ResourceType,
        val lootAmount: Long
    )

    val departurePool: Map<ExpeditionCategory, List<DepartureTemplate>> = mapOf(
        ExpeditionCategory.MINERAL to listOf(
            DepartureTemplate(
                title = "철수 시간",
                description = "탐사를 마쳤다. 떠나기 전, 채굴 현장에 쓸만한 광물이 더 남아있는 게 보인다.",
                lootLabel = "광물을 더 싣는다",
                lootType = ResourceType.IRON_ORE, lootAmount = 4L
            ),
            DepartureTemplate(
                title = "마지막 채굴",
                description = "철수 명령이 내려졌다. 시간이 조금 더 있다면 표본을 채울 수 있을 것 같다.",
                lootLabel = "표본을 더 채운다",
                lootType = ResourceType.CRYSTAL, lootAmount = 2L
            ),
            DepartureTemplate(
                title = "갱도 끝자락",
                description = "장비를 정리하던 중, 갱도 끝에서 빛나는 광맥을 발견했다.",
                lootLabel = "광맥을 마저 캔다",
                lootType = ResourceType.MAGMA_STONE, lootAmount = 3L
            ),
            DepartureTemplate(
                title = "마지막 운반",
                description = "운반선 적재 공간이 조금 남았다. 근처에 흩어진 희귀 광물이 보인다.",
                lootLabel = "희귀 광물을 담는다",
                lootType = ResourceType.RARE_EARTH, lootAmount = 2L
            )
        ),
        ExpeditionCategory.PLANET to listOf(
            DepartureTemplate(
                title = "궤도 진입 전",
                description = "귀환 준비가 끝났다. 행성 표면에 분석할 만한 샘플이 더 남아있다.",
                lootLabel = "샘플을 추가 수집한다",
                lootType = ResourceType.BIOMASS, lootAmount = 3L
            ),
            DepartureTemplate(
                title = "마지막 스캔",
                description = "이륙 직전, 센서가 인근의 에너지 반응을 가리키고 있다.",
                lootLabel = "에너지원을 회수한다",
                lootType = ResourceType.ENERGY_CORE, lootAmount = 2L
            ),
            DepartureTemplate(
                title = "냉각수 저장고",
                description = "이륙 직전 발견한 천연 냉각수 저장고. 퍼올릴 시간이 조금 있다.",
                lootLabel = "냉각수를 채운다",
                lootType = ResourceType.COOLANT, lootAmount = 3L
            ),
            DepartureTemplate(
                title = "결정체 군집",
                description = "착륙장 바로 옆에 생명 결정으로 보이는 군집이 자라 있다.",
                lootLabel = "결정을 채취한다",
                lootType = ResourceType.LIFE_CRYSTAL, lootAmount = 1L
            )
        ),
        ExpeditionCategory.RUINS to listOf(
            DepartureTemplate(
                title = "유적을 떠나기 전",
                description = "조사는 끝났다. 하지만 안쪽에 손대지 않은 유물이 남아있는 것 같다.",
                leaveLabel = "유적을 떠난다",
                lootLabel = "유물을 회수한다",
                lootType = ResourceType.ANCIENT_ARTIFACT, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "철수 신호",
                description = "본대가 철수를 알렸다. 기록 장치 하나가 아직 작동 중이다.",
                leaveLabel = "유적을 떠난다",
                lootLabel = "장치를 회수한다",
                lootType = ResourceType.DATA_CORE, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "무너지는 통로",
                description = "출구로 향하는 길에 작은 기계 부품들이 흩어져 있다. 시간이 많지 않다.",
                leaveLabel = "유적을 떠난다",
                lootLabel = "부품을 챙긴다",
                lootType = ResourceType.NANOBOT, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "마지막 방",
                description = "출구 바로 앞, 아직 들어가 보지 않은 작은 방이 있다.",
                leaveLabel = "유적을 떠난다",
                lootLabel = "방을 마저 살핀다",
                lootType = ResourceType.ANCIENT_ARTIFACT, lootAmount = 1L
            )
        ),
        ExpeditionCategory.ALIEN_CIVILIZATION to listOf(
            DepartureTemplate(
                title = "접촉을 마치며",
                description = "대화는 끝났다. 그들이 남기고 간 물건이 근처에 놓여 있다.",
                leaveLabel = "조용히 떠난다",
                lootLabel = "물건을 챙긴다",
                lootType = ResourceType.UNKNOWN_MATTER, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "마지막 신호",
                description = "그들의 기지를 벗어나기 전, 장비 일부가 여전히 작동하고 있는 게 보인다.",
                leaveLabel = "조용히 떠난다",
                lootLabel = "장비를 회수한다",
                lootType = ResourceType.QUANTUM_CORE, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "남겨진 기술",
                description = "그들이 떠난 자리에 낯선 장치 하나가 그대로 놓여 있다.",
                leaveLabel = "조용히 떠난다",
                lootLabel = "장치를 회수한다",
                lootType = ResourceType.ALIEN_TECH, lootAmount = 1L
            ),
            DepartureTemplate(
                title = "마지막 순간",
                description = "철수하려는 순간, 발밑에서 무언가 옅게 빛나는 게 보인다.",
                leaveLabel = "조용히 떠난다",
                lootLabel = "확인하러 간다",
                lootType = ResourceType.UNKNOWN_MATTER, lootAmount = 2L
            )
        )
    )

    // 자원을 더 싣다가 실패했을 때 보여줄 플레이버 텍스트 (범용)
    val departureFailureFlavors: List<String> = listOf(
        "사고가 발생해 자원을 포기하고 급히 빠져나왔다",
        "예상치 못한 위험 때문에 손에 쥔 것을 놓쳤다",
        "철수가 늦어지며 장비 일부가 손상됐다",
        "혼란 속에서 결국 자원을 챙기지 못한 채 떠났다",
        "마지막 순간 경보가 울려 모든 걸 버리고 탈출했다"
    )

    fun randomDeparture(category: ExpeditionCategory, expeditionId: String): StoryEvent {
        val pool = departurePool[category] ?: departurePool.values.first()
        val t = pool.random()
        return StoryEvent(
            id = UUID.randomUUID().toString(),
            expeditionId = expeditionId,
            title = t.title,
            description = t.description,
            choice1Label = t.leaveLabel,
            choice1ResourceType = t.lootType,
            choice1Amount = 0L,
            choice2Label = t.lootLabel,
            choice2ResourceType = t.lootType,
            choice2Amount = t.lootAmount,
            isDeparture = true
        )
    }

    fun randomEvent(category: ExpeditionCategory, expeditionId: String): StoryEvent? {
        val pool = eventPool[category] ?: return null
        if (pool.isEmpty()) return null
        val template = pool.random()
        return StoryEvent(
            id = UUID.randomUUID().toString(),
            expeditionId = expeditionId,
            title = template.title,
            description = template.description,
            choice1Label = template.choice1Label,
            choice1ResourceType = template.choice1Type,
            choice1Amount = template.choice1Amount,
            choice2Label = template.choice2Label,
            choice2ResourceType = template.choice2Type,
            choice2Amount = template.choice2Amount,
            choice3Label = template.choice3Label,
            choice3ResourceType = template.choice3Type,
            choice3Amount = template.choice3Amount
        )
    }
}
