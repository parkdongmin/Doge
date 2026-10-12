package com.doge.simulator.domain.model

// ── 초보 임무 (2026-10-12) ──────────────────────────────────────────────
// 신규 유저가 "탐사 보내고 끝?" 하고 이탈하는 걸 막으려고, 지금 할 일 하나를 화면 아래 띠로 보여준다.
// 원칙:
//  - 임무는 받는 즉시 할 수 있어야 한다. 자원이 드는 임무 바로 앞 임무의 보상이 그 자원이다
//    (탐사 → 철광석 20 → 행성 강화 → 바이오매스 50 → 연구).
//  - 보상은 그 임무에 쓴 비용 이상(코인·자원 모두).
//  - 완료는 "그 행동을 했나"가 아니라 "지금 그 상태인가"로 판정 — 임무 순서보다 먼저 해둔 것도
//    그 임무 차례가 오면 바로 완료로 뜬다(띠에 무엇이 몇/몇인지 같이 보여줘 왜 완료인지 알 수 있게).
//  - 행성 수익은 1분마다 자동 수령이라 "수익 받기"는 임무로 만들 행동이 없어 뺐다.

data class MissionProgress(
    val step: Int,               // -1 = 대상 아님, entries.size 이상 = 모두 완료
    val orbitPlayed: Boolean,
    val planetUpgradeTried: Boolean
)

// 임무 완료 판정에 쓰는 게임 상태
data class MissionGameState(
    val expeditionCount: Int,
    val readReportCount: Int,
    val maxPlanetLevel: Int,
    val researchUpgrades: Int,   // 연구 분야 레벨 합 - 시작값(분야당 1)
    val astronautCount: Int,
    val spaceshipCount: Int,
    val orbitPlayed: Boolean,
    val planetUpgradeTried: Boolean
)

// 띠의 [바로가기]가 여는 곳
enum class MissionDestination { EXPLORE, EXPEDITION_LOG, PLANET, HQ_RESEARCH, HQ_ASTRONAUT, HQ_LOUNGE, HQ_HANGAR }

enum class BeginnerMission(
    val title: String,
    val progressLabel: String,
    val unit: String,
    val target: Int,
    val destination: MissionDestination,
    val rewardCoins: Long,
    val rewardResources: Map<ResourceType, Long>,
    private val currentOf: (MissionGameState) -> Int
) {
    SEND_EXPEDITION(
        "탐사 보내기", "보낸 탐사", "회", 1, MissionDestination.EXPLORE,
        300, mapOf(ResourceType.IRON_ORE to 20L), { it.expeditionCount }
    ),
    READ_REPORT(
        "탐사 보고서 확인하기", "확인한 보고서", "개", 1, MissionDestination.EXPEDITION_LOG,
        300, emptyMap(), { it.readReportCount }
    ),
    // 비용: 코인 300 + 철광석 20(1번 보상). Lv1→2도 5% 실패가 있어 "레벨 2"가 아니라 "시도"로 완료
    UPGRADE_PLANET(
        "행성 강화해 보기", "강화 시도", "회", 1, MissionDestination.PLANET,
        300, mapOf(ResourceType.BIOMASS to 50L),
        { if (it.planetUpgradeTried || it.maxPlanetLevel >= 2) 1 else 0 }
    ),
    // 비용: 코인 1,000 + 바이오매스 50(3번 보상)
    RESEARCH(
        "연구소에서 연구하기", "연구", "회", 1, MissionDestination.HQ_RESEARCH,
        1_000, emptyMap(), { it.researchUpgrades }
    ),
    // 비용: 인턴 약 500~700
    HIRE_ASTRONAUT(
        "대원 고용하기", "보유 대원", "명", 2, MissionDestination.HQ_ASTRONAUT,
        1_000, emptyMap(), { it.astronautCount }
    ),
    // 비용: 최소 베팅 500 (중간에 나가면 안 치고 끝까지 한 판)
    PLAY_ORBIT(
        "휴게실에서 ORBIT 한 판", "끝까지 한 판", "판", 1, MissionDestination.HQ_LOUNGE,
        500, emptyMap(), { if (it.orbitPlayed) 1 else 0 }
    ),
    // 비용: 정찰선 1,000
    BUY_SPACESHIP(
        "우주선 하나 더 사기", "보유 우주선", "척", 2, MissionDestination.HQ_HANGAR,
        1_000, emptyMap(), { it.spaceshipCount }
    );

    fun currentIn(state: MissionGameState): Int = currentOf(state).coerceIn(0, target)
}

data class BeginnerMissionStatus(
    val mission: BeginnerMission,
    val index: Int,
    val total: Int,
    val current: Int,
) {
    val isComplete: Boolean get() = current >= mission.target
}

/** 지금 보여줄 임무. 대상이 아니거나 다 끝냈으면 null */
fun beginnerMissionStatus(progress: MissionProgress, state: MissionGameState): BeginnerMissionStatus? {
    val mission = BeginnerMission.entries.getOrNull(progress.step) ?: return null
    return BeginnerMissionStatus(
        mission = mission,
        index = progress.step,
        total = BeginnerMission.entries.size,
        current = mission.currentIn(state)
    )
}
