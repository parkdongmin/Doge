package com.doge.simulator.domain.model

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    /** 총 자산 = 보유 코인 + 행성 시세 합산 */
    val totalAsset: Long = 0L,
    val coins: Long = 0L,
    val planetCount: Int = 0,
    val updatedAt: Long = 0L,
    /** 클라이언트에서 계산되는 순위 (1-based) */
    val rank: Int = 0
)

// 랭킹이 비어 보이지 않게 Firebase 콘솔에서 users에 직접 넣어둔 NPC 문서의 이름 접두어.
// 앱이 저장하는 이름은 항상 maskDisplayName을 거치므로 실제 유저는 이 접두어를 가질 수 없다
const val NPC_NAME_PREFIX = "NPC - "

/** 랭킹 화면에 보여줄 이름 — NPC는 그대로, 실제 유저는 마스킹 */
fun rankingDisplayName(name: String?): String =
    if (name != null && name.startsWith(NPC_NAME_PREFIX)) name else maskDisplayName(name)

/**
 * 랭킹에 노출되는 이름 마스킹: 첫·끝 글자만 남기고 가운데를 '*'로 가린다 (최대 4개).
 * "박동민" → "박*민", "남궁민수" → "남**수", "Dongmin Park" → "D****k", 두 글자는 "박*".
 * 첫 글자만 남기면 "박**"끼리 구분이 안 돼서 끝 글자도 남긴다.
 * 이미 마스킹된 값에 다시 적용해도 결과가 같다.
 * users/{uid}는 로그인한 누구나 읽을 수 있어서, 화면뿐 아니라 저장할 때도 이걸 거친다.
 */
fun maskDisplayName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return "익명"
    val codePoints = trimmed.codePoints().toArray()
    val first = String(codePoints, 0, 1)
    return when (codePoints.size) {
        1 -> trimmed
        2 -> "$first*"
        else -> {
            val last = String(codePoints, codePoints.size - 1, 1)
            first + "*".repeat((codePoints.size - 2).coerceAtMost(4)) + last
        }
    }
}
