# Contract: 2부 도메인 API

presentation 계층과 테스트가 기대는 도메인 쪽 함수와 동작. 모델 정의는 [data-model.md](../data-model.md)를 본다.

## 1. 특성 굴리기

```kotlin
object PlanetTraitRoller {
    /** T10 이하는 항상 빈 리스트. T11+는 balance.md 3절 확률표를 따른다. */
    fun roll(type: PlanetType, tier: Int, random: Random = Random): List<PlanetTrait>
}
```

- 결과는 1~2개다. 같은 종류나 반대 짝은 함께 나오지 않는다.
- 좋은 특성의 강도는 티어별 Ⅰ/Ⅱ/Ⅲ 가중치를 따르고, 나쁜 특성의 강도는 1이다.
- `random`을 주입받는다. 테스트는 시드를 고정한다.

## 2. 발견 행성 생성

```kotlin
class GeneratePlanetsUseCase {
    /** 탐사 완료 시 한 번만 호출. 스탯·변종·특성·특성 시세 반영 가격을 확정한다. */
    fun discoveredPlanet(type: PlanetType, tier: Int, random: Random = Random): Planet
}
```

- `buyPrice = round((basePrice + production×20 + risk×10) × (1 + traitEffects.priceAdjustment))`
- `CompleteExpeditionUseCase`가 이 함수를 호출하고, 결과를 `ExpeditionRepository.complete(..., discoveredPlanet = planet)`로 `discoveredPlanetJson`에 저장한다.
- `ExploreViewModel`은 `Expedition.discoveredPlanet`(디코딩된 값)을 그대로 쓴다. null이고 `discoveredPlanetType`만 있는 옛 행만 기존 방식으로 굴린다.

## 3. 효과 (Planet 확장 — 유일한 계산 경로)

| API | 쓰는 곳 |
|---|---|
| `normalProductionAtLevel(level)` (특성 생산 배율 포함) | 수령·오프라인·이벤트 적립·정비 비용·코인 스킵·화면 |
| `effectiveBadChance` | `RollPlanetEventUseCase`, 상세 화면 |
| `breakdownThreshold`, `boomMultiplier` | `RollPlanetEventUseCase.applyEvent` |
| `resourceDropMultiplier` | `CollectProfitUseCase`, 상세 화면 드롭 표 |
| `upgradeSuccessRate()` | `UpgradePlanetUseCase`, 상세 화면 강화 성공률 |
| `traitEffects.priceAdjustment` | 발견 화면 "특성 시세 ±n%" |

`applyEvent(planet, isBad, delta)`는 정상 + 호재일 때 `delta × boomMultiplier`를 쓰고, 정상 + 악재일 때 `delta >= breakdownThreshold`이면 고장 낸다. 고장 중 동작은 바뀌지 않는다.

## 4. 획득 기록

```kotlin
interface UserRepository {
    /** 도감 등록 + (특성 행성이면) 획득 카운터 원자 증가. 구매·코인으로 받기에서만 호출. */
    suspend fun recordPlanetAcquisition(planet: Planet)
    fun getTierProgressInputs(): Flow<TraitAcquisitionCounts>   // 카운터 3개
}
```

## 5. 티어 사다리

```kotlin
data class TierProgress(
    val discoveredVariantIds: Set<String>,
    val part1Completed: Boolean,
    val traitPlanetsAcquired: Int,
    val highFitTier2Acquired: Int,
    val highFitTier3Acquired: Int,
) {
    fun firstLockedTier(maxTier: Int = 15): Int?
    fun progressFor(tier: Int): Pair<Int, Int>?   // (현재, 필요), 조건 없으면 null
}
```

- `StartExpeditionUseCase`는 `firstLockedTier(maxTier = tier) != null`이면 `TierRequirementNotMet(label)`을 돌려준다.
- 탐사 화면 배너·티어 칩·검증이 모두 같은 `TierProgress`를 쓴다. `ExploreViewModel`이 도감·스토리·카운터 Flow를 묶어 하나로 만든다.

## 6. 스토리 진행 (`GenerateExpeditionReportUseCase`)

입력·출력은 지금과 같다(`invoke(expedition, isSuccess): ExpeditionReport`). 동작 규칙:

| 상황 | 결과 기록 |
|---|---|
| `chapter == 5 && part1Completed` | 6장 1번 기록 (장 마무리 없이) |
| 6장, 기록 16개 다 봄 | 6장 마무리 → 다음 기록부터 7장 |
| 7~9장, 이야기 끝 + 조건 충족 | 그 장 마무리, 다음 장으로 |
| 10장, 이야기 끝 + T15 성공 | 2부 완주 기록 (`isStoryEnding`, chapter 10), `part2Completed = true` |
| 7~10장, 이야기 끝 + 조건 미달 | `k = 장 내 기록 수 − 16`: `k % 3 == 0` → 막힘 `stalled[(k/3) % n]`, 그 외 → 일상 기록(`settlementLogCursor++`) |
| 2부 완주 후 | 일상 기록만 |
| 이벤트 (chapter ≥ 6) | 이벤트마다 50% 2부 공용 풀 / 50% 카테고리 풀 |
| 이벤트 (chapter == 5) | 5장 전용 풀 (지금과 같음) |

마일스톤 플래그(`firstT11Completed` 등)는 성공한 탐사에서 `tier >=` 조건으로 켜고, 1부처럼 미리 쌓아 둘 수 있다.

## 7. 2부 안내 상태

```kotlin
interface StoryRepository {
    suspend fun markPart2IntroShown()
}
```

`TutorialViewModel`이 `StoryProgress`를 구독해 `Part2StoryIntro`를 정한다. 마지막 페이지를 닫을 때 `markPart2IntroShown()`을 호출한다.
