# Data Model: 2부 확장 — 행성 특성 · T11~T15 · 스토리 6~10장

Phase 1 산출물. 기존 모델에 **추가되는 것만** 적는다. 수치는 [balance.md](balance.md), 문장은 [story-content.md](story-content.md)가 원본이며, 코드의 표는 이 두 문서를 그대로 옮긴다.

---

## 1. 도메인 모델 (순수 Kotlin, `domain/model/`)

### 1.1 `TraitType` (enum, 새 파일 `PlanetTrait.kt`)

| 값 | 표시 이름 | 좋음/나쁨 | 효과 축 | 반대 짝 |
|---|---|---|---|---|
| `HIGH_PRODUCTION` | 높은 생산력 | 좋음 | 생산량 | `LOW_PRODUCTION` |
| `STABLE_ENVIRONMENT` | 안정된 환경 | 좋음 | 악재 확률 | `UNSTABLE_ENVIRONMENT` |
| `STURDY_STRUCTURE` | 튼튼한 구조 | 좋음 | 고장 기준 | `WEAK_STRUCTURE` |
| `BOOM_PRONE` | 호황 체질 | 좋음 | 호재 상승폭 | — |
| `RICH_RESOURCES` | 풍부한 자원 | 좋음 | 자원 드롭 | `BARREN_RESOURCES` |
| `UPGRADE_APTITUDE` | 강화 적성 | 좋음 | 강화 성공률 | `UPGRADE_INAPTITUDE` |
| `LOW_PRODUCTION` | 낮은 생산력 | 나쁨 | 생산량 | `HIGH_PRODUCTION` |
| `UNSTABLE_ENVIRONMENT` | 불안정한 환경 | 나쁨 | 악재 확률 | `STABLE_ENVIRONMENT` |
| `WEAK_STRUCTURE` | 약한 구조 | 나쁨 | 고장 기준 | `STURDY_STRUCTURE` |
| `BARREN_RESOURCES` | 메마른 자원 | 나쁨 | 자원 드롭 | `RICH_RESOURCES` |
| `UPGRADE_INAPTITUDE` | 강화 부적성 | 나쁨 | 강화 성공률 | `UPGRADE_APTITUDE` |

프로퍼티: `displayName`, `isGood`, `axis: TraitAxis`, `opposite: TraitType?`.

### 1.2 `TraitAxis` (enum)

`PRODUCTION`, `BAD_CHANCE`, `BREAKDOWN_THRESHOLD`, `BOOM`, `RESOURCE_DROP`, `UPGRADE_SUCCESS`

### 1.3 `TraitFit` (enum)

`HIGH("높음")`, `MEDIUM("보통")`, `LOW("낮음")`. 플레이어에게는 글자로만 보인다(FR-009, 별 표시 없음).

### 1.4 `PlanetTrait` (data class)

| 필드 | 타입 | 규칙 |
|---|---|---|
| `type` | `TraitType` | |
| `intensity` | `Int` | 좋은 특성 1~3(Ⅰ/Ⅱ/Ⅲ). 나쁜 특성은 항상 1이고 쓰지 않음 |

인코딩: `"HIGH_PRODUCTION:3,UNSTABLE_ENVIRONMENT:1"`. 디코딩할 때 모르는 이름이나 잘못된 조각은 건너뛴다.
검증(`PlanetTraitRoller`가 보장): 최대 2개, 같은 `type` 없음, 서로 `opposite` 아님.

### 1.5 `PlanetTraitTable` (object)

balance.md 5절과 6절을 그대로 옮긴 정적 표.

| 조회 | 반환 | 출처 |
|---|---|---|
| `fit(planetType, goodTrait)` | `TraitFit` | 5절 표 굵은 글씨 |
| `value(planetType, goodTrait, intensity)` | `Double` | 5절 표 (생산 +0.14, 악재 −3.5(%p), 고장 +0.025, 호황 +0.20, 자원 +0.40, 강화 ×1.05 → 1.05) |
| `badValue(badTrait)` | `Double` | 4절 고정값 (−0.10 / +3 / −0.025 / ×0.5 / ×0.95) |
| `dailyProfitEffect(planetType, trait, intensity)` | `Double` (예: 0.35) | 5절 하단 표, 나쁜 특성은 6절 행성별 표, 강화 적성·부적성은 시도 감소율 × 0.4 |

### 1.6 `TraitEffects` (data class, `Planet.traitEffects`로 계산)

| 필드 | 기본값(특성 없음) | 계산 |
|---|---|---|
| `productionMultiplier` | 1.0 | 1 + 생산 축 값 합 |
| `badChanceDeltaPercent` | 0.0 | 악재 축 %p 합 |
| `breakdownThresholdDelta` | 0.0 | 고장 축 값 합 |
| `boomMultiplier` | 1.0 | 1 + 호황 값 |
| `resourceDropMultiplier` | 1.0 | 자원 축 배율 곱 |
| `upgradeSuccessMultiplier` | 1.0 | 강화 축 배율 곱 |
| `priceAdjustment` | 0.0 | `dailyProfitEffect` 합 |

반대·중복 금지 규칙 때문에 한 축에는 특성이 최대 1개만 붙는다. 그래서 "합"과 "곱"이 실제로 섞이는 경우는 없다.

### 1.7 `Planet` 변경

| 추가 | 설명 |
|---|---|
| 필드 `traits: List<PlanetTrait> = emptyList()` | |
| `val Planet.hasTraits` | |
| `val Planet.traitEffects: TraitEffects` | 1.6 |
| `normalProductionAtLevel()` | 기존 식 × `traitEffects.productionMultiplier` |
| `val Planet.effectiveBadChance: Int` | `(eventRate + badChanceDelta).roundToInt().coerceIn(0, 100)` |
| `val Planet.breakdownThreshold: Double` | `PLANET_BREAKDOWN_DELTA_THRESHOLD + breakdownThresholdDelta` |
| `val Planet.boomMultiplier: Double` | |
| `val Planet.resourceDropMultiplier: Double` | |
| `fun Planet.upgradeSuccessRate(): Float` | `(UPGRADE_SUCCESS_RATES[level] × 배율).coerceAtMost(0.99)` |

`buyPrice`는 발견 시점에 특성 시세가 이미 반영된 값이다(research R4). `marketValue`·`marketAdjustmentFor`의 식은 바꾸지 않는다.

### 1.8 `TierUnlockCondition` (sealed, `GameConstants`)

| 티어 | 조건 | 진행도 |
|---|---|---|
| 1, 2 | 없음 | — |
| 3~10 | `RarityDiscovered(rarity, count)` (기존 그대로) | 도감에서 그 등급 이상 개수 |
| 11 | `Part1Completed` | 0/1 |
| 12 | `TraitPlanetsAcquired(10)` | `traitPlanetsAcquired` |
| 13 | `HighFitTraitAcquired(minIntensity = 2, count = 2)` | `highFitTier2Acquired` |
| 14 | `TraitPlanetsAcquired(40)` | `traitPlanetsAcquired` |
| 15 | `HighFitTraitAcquired(minIntensity = 3, count = 2)` | `highFitTier3Acquired` |

`TierProgress(discoveredVariantIds, part1Completed, traitPlanetsAcquired, highFitTier2Acquired, highFitTier3Acquired)`
- `firstLockedTier(progress, maxTier = 15): Int?`: 1부터 순서대로 훑어 가장 먼저 막힌 티어를 돌려준다(기존 규칙 그대로).
- `progressFor(tier): Pair<Int, Int>`: (현재, 필요) 값. 배너에 표시한다.

### 1.9 `StoryProgress` 변경

| 추가 필드 | 기본값 | 의미 |
|---|---|---|
| `firstT11Completed` … `firstT13Completed`, `firstT15Completed` | false | 7·8·9·10장 탈출 조건 |
| `part2Completed` | false | 2부 완주 기록이 나왔는지 |
| `part2IntroShown` | false | 관제탑 2부 안내를 봤는지 |
| `settlementLogCursor` | 0 | 정착지 일상 기록을 몇 줄 썼는지(누적) |
| `val part1Completed` | = `storyCompleted` | 의미가 드러나는 이름 |

`isCurrentChapterExitMet`, `chapterTitle`은 6~10장으로 늘린다. 6장은 탈출 조건이 없다(이야기만 보면 됨).

**장 상태 전이**

```text
1 →(유적)→ 2 →(외계)→ 3 →(T3)→ 4 →(T5)→ 5 ─(T10 + 이야기 끝)→ [1부 완주 기록, chapter=5]
                                                     │
                                       다음 기록부터 ▼
6 ─(이야기 16개)→ 7 ─(T11+)→ 8 ─(T12+)→ 9 ─(T13+)→ 10 ─(T15 + 이야기 끝)→ [2부 완주 기록, chapter=10]
                                                     │
                                                     ▼
                                              정착지 일상 기록만
```

각 화살표의 공통 규칙: 그 장의 기록 16개를 다 봐야 하고, 한 기록에 한 장씩만 넘어간다. 조건을 못 채운 동안에는 막힘 1 : 일상 2로 섞는다.

### 1.10 `ExpeditionReport`

필드는 바꾸지 않는다. 표시 규칙만 추가한다: `isStoryEnding && chapter <= 5` → "1부 완주", `isStoryEnding && chapter >= 10` → "2부 완주".

### 1.11 `StoryContent` 추가

| 이름 | 내용 |
|---|---|
| `recordTitlePool[6..10]` | 장마다 16줄 |
| `chapterEndingTitles[6..9]` | 장 마무리 |
| `part2EndingTitle` | 2부 완주 문장 |
| `stalledRecordTitlePool[7..10]` | 막힘 3 / 5 / 8 / 16줄 (6장 없음) |
| `settlementLogPool` | 정착지 일상 40줄 |
| `part2EventPool` | 공용 이벤트 8개 (`EventTemplate`, 8번은 선택지 3개) |
| `nextStoryHints[6..10]` | 안내 문장 + 조건 표시 |
| `part2IntroLines` | 관제탑 안내 5줄 |
| `settlementLog(cursor)` | 바퀴 번호 `cursor / 20`을 시드로 섞은 순열의 `cursor % 20`번째 줄. 새 바퀴의 첫 줄이 직전 바퀴 마지막 줄과 같으면 첫·둘째 줄을 바꿈(연속 중복 방지) |

---

## 2. Room 엔티티 변경 (DB 22 → 23)

| 테이블 | 칼럼 | 타입 / 기본값 |
|---|---|---|
| `planet_table` | `traits` | TEXT NOT NULL DEFAULT '' |
| `user_table` | `traitPlanetsAcquired` | INTEGER NOT NULL DEFAULT 0 |
| `user_table` | `highFitTier2Acquired` | INTEGER NOT NULL DEFAULT 0 |
| `user_table` | `highFitTier3Acquired` | INTEGER NOT NULL DEFAULT 0 |
| `story_progress_table` | `firstT11Completed`, `firstT12Completed`, `firstT13Completed`, `firstT15Completed`, `part2Completed`, `part2IntroShown` | INTEGER NOT NULL DEFAULT 0 |
| `story_progress_table` | `settlementLogCursor` | INTEGER NOT NULL DEFAULT 0 |
| `expedition_table` | `discoveredPlanetJson` | TEXT (nullable) |

- `MIGRATION_22_23`: 위 `ALTER TABLE … ADD COLUMN`만 한다. 데이터 변환은 없다.
- **획득 카운터 DAO**: `UPDATE user_table SET traitPlanetsAcquired = traitPlanetsAcquired + 1, highFitTier2Acquired = highFitTier2Acquired + :t2, highFitTier3Acquired = highFitTier3Acquired + :t3 WHERE id = 1`. 한 문장의 원자 증가다.
- **`discoveredPlanetJson`**: 탐사 완료 시 굴린 `PlanetEntity`를 `snapshotJson`으로 직렬화한 값. 발견이 없으면 null.

## 3. 클라우드 스냅샷 (스키마 4 → 5)

- `GameSnapshot.SCHEMA_VERSION = 5`. 주석에 "5: 2부 — 행성 특성·획득 카운터·스토리 6~10장·발견 행성 저장(필드 추가만, step 없음)"을 적는다.
- `SnapshotMigrations`에는 step을 추가하지 않는다. 기본값이 있는 필드만 추가했고 `ignoreUnknownKeys`가 처리한다.
- 옛 세이브 복원 결과: 특성 없음, 카운터 0, 2부 미시작. `storyCompleted = true`인 세이브는 T11이 열리고 다음 기록에서 6장이 시작된다.

## 4. 획득 판정 (`UserRepository.recordPlanetAcquisition(planet)`)

```text
recordVariantDiscovery(planet.variantId)          // 기존 도감 (변종 기준, 특성 무관 — FR-011a)
if planet.hasTraits:
    t2 = planet.traits.any { good && fit == HIGH && intensity >= 2 } ? 1 : 0
    t3 = planet.traits.any { good && fit == HIGH && intensity == 3 } ? 1 : 0
    incrementTraitAcquisition(t2, t3)            // traitPlanetsAcquired는 항상 +1
```

호출하는 곳: `BuyPlanetUseCase`(구매·팔고 구매하기), "코인으로 받기"(`ExploreViewModel.convertSlotFullToCoinInternal`, 지금 `recordVariantDiscovery`를 부르는 자리). 발견 창을 그냥 닫으면 호출하지 않는다.
