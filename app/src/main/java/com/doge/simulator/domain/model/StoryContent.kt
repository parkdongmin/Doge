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
            // 화성 궤도 통과 시점 — 엔딩의 복선. 그때는 그냥 넘겼던 반응이었다는 게 나중에 회수됨
            "화성 궤도를 지날 때 센서가 이상 반응을 보였다. 대수롭지 않게 넘겼다",
            "보급품 일부가 사라졌다. 대원들은 모두 부인한다",
            "귀환 도중 통신이 잠시 끊겼다. 이유를 모른다",
            // 2026-09-04 추가 (초안 — 검토 필요)
            "낡은 궤도 위성이 여전히 신호를 보내고 있었다. 발신 목적은 불명",
            "얼음 표면 아래 인공적으로 보이는 균열 패턴을 발견했다",
            "탐사 기록에 우리가 남기지 않은 좌표가 하나 더 있었다",
            "동면 중인 대원이 며칠째 같은 꿈을 꾼다고 말했다",
            "같은 항로를 두 번 지났는데 풍경이 미묘하게 달라져 있었다",
            "무전기에서 정체불명의 카운트다운이 들렸다. 끝나기 전에 꺼버렸다",
            "탐사 기록 보관함 하나가 잠겨 있었다. 열쇠는 아무도 갖고 있지 않다",
            "밤하늘의 별자리가 지도와 미세하게 어긋나 있었다"
        ),
        2 to listOf(
            "그 주파수가 다시 잡혔다. 더 강해졌다",
            "신호의 방향을 추적했다. 알파 성계 방향이다",
            "이상한 구조물을 발견했다. 자연물이 아니다",
            "구조물 내부에서 문자를 발견했다. 해독 중",
            "대원 한 명이 며칠째 조용하다",
            "같은 행성에서 같은 신호가 두 번 잡혔다",
            "신호가 우리를 향해 방향을 바꿨다",
            "유적 속 장치가 우리 접근에 반응했다",
            // 2026-09-04 추가 (초안 — 검토 필요)
            "해독된 문자 중 일부가 우리 숫자 체계와 겹쳤다",
            "신호 발신원 주변에서만 자기장이 흐트러졌다",
            "유적 벽화 하나가 우리 탐사선의 항로와 똑같았다",
            "신호를 끄려 하자 주파수가 세 배로 커졌다",
            "오래된 유적인데 먼지가 거의 쌓여있지 않았다",
            "다른 탐사팀 채널에서도 같은 신호가 잡혔다는 보고가 왔다",
            "구조물 중심부에 사람 크기의 빈 공간이 있었다. 무언가 있던 자리처럼",
            "신호 패턴을 거꾸로 재생하자 우리 언어의 조각이 섞여 있었다"
        ),
        3 to listOf(
            // 조우 — 사람이 아니다. 그들도 우리처럼 우주를 표류하며 행성 탐사로
            // 자원을 모아 생존하는 처지(정복욕이 없는 이유). 말은 안 통해도
            // "고향으로 돌아가고 싶다"는 마음만은 서로 읽힘. 실제 소통(숫자·좌표)은
            // 챕터4에서 본격적으로 쌓인다 — 여기선 감정적 신뢰만 트임
            "그림자가 보였다. 사람의 형태가 아니었다",
            "통신을 시도했다. 응답이 없다",
            "그들이 먼저 접근해왔다. 무기는 없었다",
            "언어는 달랐지만, 의도는 읽혔다",
            "교환을 제안해왔다. 뭘 원하는지 몰랐다",
            "같은 별을 바라보고 있었다. 우연이 아닐 수 있다",
            "대원 한 명이 그들을 따라가려 했다",
            "그들은 우리 기술에 전혀 관심이 없었다",
            // 2026-09-04 개정 (초안 — 검토 필요)
            "그들의 우주선도 여기저기 급조해 고친 흔적투성이였다. 우리처럼",
            "손짓 하나로 대화가 절반은 통했다. 나머지 절반은 여전히 미궁",
            "숫자를 세는 듯한 몸짓을 반복했다. 뭔가를 맞춰보려는 것 같았다",
            "경계 태세를 풀자 그들도 똑같이 풀었다",
            "선물이라며 건넨 물건은 우리 우주선 부품과 비슷한 합금이었다",
            "그들도 누군가를 기다리는 듯 자꾸 하늘을 올려다봤다",
            "정복도, 지배도 아니었다. 그들이 원한 건 그저 생존이었다",
            "헤어지기 직전, 그들의 몸짓이 처음으로 편안해 보였다"
        ),
        4 to listOf(
            // 동맹인가, 적인가 — 실제론 적대가 아니라 "말이 안 통해서 생기는 오해"와
            // 그걸 뚫고 자라는 신뢰. 핵심 줄기: 숫자 → 같은 별의 깜빡임 주기로 시간
            // 단위 맞추기(펄서 주기 — 실제 보이저 금박판에도 쓰인 우주 공통 기준) →
            // 좌표계 대조 → 공동 제작 지도 완성 → 각자 고향 방향으로 작별.
            // "돌아오지 않은 대원"은 사실 그들과 가장 깊이 통하게 된 다리 역할로 귀환
            "그들의 임시 거처에 초대받았다",
            "우리가 모르는 생존 기술이 있었다",
            "그들이 뭔가 도움을 요청하고 있었다",
            "대원 한 명이 돌아오지 않았다",
            "숫자를 하나씩 맞춰가며 대화를 시도했다",
            "그들이 먼저 선물을 보내왔다",
            "돌아오지 않았던 대원이, 그들과 함께 걸어서 돌아왔다",
            "침묵이 이어졌다. 서로 기다리고 있다",
            // 2026-09-04 개정 (초안 — 검토 필요)
            "그 대원이 그들과 손짓만으로 숫자를 주고받고 있었다",
            "같은 별의 깜빡이는 주기를 짚어가며 시간 단위를 맞췄다",
            "숫자와 별의 주기로, 서로의 위치를 그려보기 시작했다",
            "며칠째, 서로의 항법 자료를 맞춰보는 작업이 이어졌다",
            "마침내 두 좌표계가 하나의 지도 위에서 겹쳐졌다",
            "지도는 완성됐다. 이제 각자, 고향이라 믿는 방향으로",
            "떠나기 전, 그들이 마지막으로 우리 쪽을 오래 바라봤다",
            "같은 우주를 향해, 서로 다른 방향으로 흩어졌다"
        ),
        5 to listOf(
            // 미지의 공간 — 함께 만든 지도를 따라갔지만 거기엔 아무것도 없다(첫 반전).
            // 연료·희망이 바닥나는 절망 구간 → 포기하려는 순간 오래된 신호 재포착 →
            // 챕터1의 "화성 궤도 오류"를 떠올리는 회수 구간 → 엔딩(storyEndingTitle)으로 이어짐
            "함께 만든 지도를 따라, 가장 먼 곳까지 나아갔다",
            "우리가 계산한 좌표엔, 그저 텅 빈 우주뿐이었다",
            "다시 계산해도 결과는 같았다. 여기가 맞는데, 아무것도 없다",
            "고대 유물에서 낯익은 문자를 또 발견했다. 그들의 흔적일지도 모른다",
            "연료 게이지가 처음으로 절반 밑으로 떨어졌다",
            "귀환 시간이 계산과 맞지 않는다",
            "누구도 입 밖에 내지 않았지만, 모두 같은 생각을 하고 있었다",
            "며칠째, 누구도 농담을 하지 않았다",
            // 2026-09-04 개정 (초안 — 검토 필요)
            "포기하려던 순간, 오래되고 희미한 신호 하나가 다시 잡혔다",
            "신호는 미약했지만, 분명 어딘가에서 반복해서 보내오고 있었다",
            "문득 떠올랐다 — 태양계를 떠나던 날, '오류'라 넘겼던 그 신호",
            "그날의 기록을 다시 꺼내 봤다. 좌표가... 낯익다",
            "이건 새로운 발견이 아닐지도 모른다는 예감이 들었다",
            "돌아갈 곳이 아니라, 잊고 있던 곳을 향하는 기분이었다",
            "마지막 남은 연료로, 신호를 향해 방향을 돌렸다",
            "처음부터 찾는 것이 아니라 찾아지고 있었다"
        )
    )

    // 챕터 전환 시 클리프행어 문장 (5는 챕터6이 없어 미사용 — storyEndingTitle이 그 자리를 대신함)
    val chapterEndingTitles: Map<Int, String> = mapOf(
        1 to "이 행성에... 우리만 있는 게 아닌데?",
        2 to "그들은... 우리가 오기를 기다리고 있었다",
        3 to "적이 아니다. 하지만 친구도 아니다",
        // 2026-09-04 개정 — 강요/위협 뉘앙스에서 "함께 만든 지도로 작별"로 톤 변경
        4 to "지도는 완성됐다. 이제, 각자의 별을 향해"
    )

    // 스토리 엔딩 — 챕터5("미지의 공간")에서 최초로 T10 탐사에 성공한 기록에 딱 한 번 붙는
    // 타이틀. chapterEndingTitles와 달리 챕터를 6으로 넘기진 않고(챕터는 계속 5), 이
    // 기록 하나만 "완주" 기록으로 특별 표시하는 데 쓰인다.
    // 2026-09-04 개정 — 신호를 따라간 곳이 화성(챕터1 "센서 오류"의 정체)이었다는 반전.
    // 화성엔 지구와 오래전 연락이 끊긴 인류 정착지가 있었다 — "가장 먼 곳"인 줄 알았던
    // 여정의 끝이 사실 "가장 가까운 곳"이었다는 아이러니로 마무리
    const val storyEndingTitle = "가장 먼 곳까지 왔다고 믿었다. 그 끝에서 만난 건, 화성에 남아 있던 우리였다"

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

    // ── 챕터5 전용 이벤트 (2026-09-04 개정, 초안 — 검토 필요) ─────────────
    // 카테고리 무관 — 챕터5("미지의 공간": 함께 만든 지도가 빈 좌표로 끝나고, 옛 신호를
    // 다시 좇는 시기)에 도달하면 지금 뭘 채굴하든 이 풀에서만 뽑는다. "누군가 조작한다"는
    // 뉘앙스 대신, 우리 자신의 오래된 기록을 다시 마주하는 쪽으로 개정 — 미지의 존재가
    // 아니라 우리가 지나쳐온 것들이 진짜 정체라는 결말과 톤을 맞춤
    val chapter5EventPool: List<EventTemplate> = listOf(
        EventTemplate(
            title = "잊고 있던 항법 기록을 뒤늦게 찾았습니다",
            description = "정리 안 된 옛 기록을 뒤지다, 태양계를 떠나던 날의 로그를 다시 찾았다. 그때는 대수롭지 않게 넘겼다.",
            choice1Label = "기록을 정밀 분석한다",
            choice1Type = ResourceType.UNKNOWN_MATTER, choice1Amount = 7L,
            choice2Label = "일단 계속 나아간다",
            choice2Type = ResourceType.QUANTUM_CORE, choice2Amount = 5L
        ),
        EventTemplate(
            title = "신호가 예상보다 훨씬 오래된 것으로 확인됐습니다",
            description = "발신 시점을 역산해보니 수십 년은 된 신호였다. 누군가 그만큼 오래 기다려온 걸까.",
            choice1Label = "발신원을 추적한다",
            choice1Type = ResourceType.QUANTUM_CORE, choice1Amount = 6L,
            choice2Label = "일지에 기록만 남긴다",
            choice2Type = ResourceType.UNKNOWN_MATTER, choice2Amount = 5L
        ),
        EventTemplate(
            title = "대원 하나가 낯익은 지형이라고 말했습니다",
            description = "본 적 없는 곳이라기엔 이상하게 익숙하다는 반응. 착각으로 넘기기엔 표정이 진지했다.",
            choice1Label = "그 말을 믿고 더 살펴본다",
            choice1Type = ResourceType.UNKNOWN_MATTER, choice1Amount = 8L,
            choice2Label = "피로 때문이라 여기고 넘어간다",
            choice2Type = ResourceType.QUANTUM_CORE, choice2Amount = 5L
        ),
        EventTemplate(
            title = "연료 재계산 결과가 아슬아슬하게 나왔습니다",
            description = "돌아갈 걱정과 계속 갈 걱정을 동시에 해야 하는 처지가 됐다. 그래도 신호는 계속 온다.",
            choice1Label = "신호를 향해 계속 나아간다",
            choice1Type = ResourceType.QUANTUM_CORE, choice1Amount = 7L,
            choice2Label = "안전하게 회항 경로부터 확보한다",
            choice2Type = ResourceType.UNKNOWN_MATTER, choice2Amount = 5L
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

    private fun eventFromTemplate(template: EventTemplate, expeditionId: String): StoryEvent = StoryEvent(
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

    fun randomEvent(category: ExpeditionCategory, expeditionId: String): StoryEvent? {
        val pool = eventPool[category] ?: return null
        if (pool.isEmpty()) return null
        return eventFromTemplate(pool.random(), expeditionId)
    }

    // 챕터5 전용 — chapter5EventPool은 항상 비어있지 않으므로(고정 목록) null 없이 반환
    fun randomChapter5Event(expeditionId: String): StoryEvent =
        eventFromTemplate(chapter5EventPool.random(), expeditionId)
}
