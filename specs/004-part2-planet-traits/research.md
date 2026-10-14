# Research: 2부 확장 — 행성 특성 · T11~T15 · 스토리 6~10장

Phase 0 산출물. 스펙·balance.md·story-content.md는 수치와 문장을 이미 확정했으므로, 여기서는 "기존 코드에 어떻게 얹을지"의 기술 결정만 정리한다. 모든 항목은 2026-10-14 기준 코드(`004-part2-planet-traits` 브랜치, DB 22 / 스냅샷 스키마 4)를 직접 읽고 정했다.

---

## R1. 발견 행성(특성 포함)을 언제 굴리고 어디에 저장하나

**Decision**: 행성 스탯·변종·특성·가격을 **탐사 완료 시점(`CompleteExpeditionUseCase`)에 한 번만** 굴리고, 결과를 `expedition_table`의 새 칼럼 `discoveredPlanetJson`(PlanetEntity JSON, nullable)에 저장한다. 결과 창(`ExploreViewModel.presentExpeditionResult`)은 이 값을 읽기만 한다.

**Rationale**:
- 지금은 결과 창을 띄울 때마다 ViewModel이 스탯을 새로 굴린다(`ExploreViewModel.kt:183~200`). 결과를 처리하기 전에 앱을 끄면 `resultHandled = false`가 그대로라 다음 실행 때 **다시 굴린다**. 스탯만 바뀔 때는 작은 문제였지만, 특성이 붙으면 "앱을 껐다 켜서 Ⅲ 특성이 나올 때까지 다시 굴리기"가 가능해진다.
- 완료 처리는 포그라운드 폴링과 백그라운드 워커 중 하나만 통과하므로(`expeditionRepository.complete`의 IN_PROGRESS 조건), 여기서 굴리면 정확히 한 번만 굴러간다.
- `ExpeditionEntity`는 이미 클라우드 스냅샷에 들어 있어서, 결과를 보기 전에 기기를 바꿔도 같은 행성이 나온다.

**Alternatives considered**:
- *ViewModel에서 계속 굴리고 특성만 시드 고정*: 시드를 expeditionId로 고정하면 재롤은 막히지만, 굴리는 로직이 presentation 계층에 남고 테스트하기 어렵다.
- *특성만 별도 칼럼에 저장*: 스탯(생산량·위험도)은 여전히 재롤된다. 가격은 스탯과 특성을 함께 써서 계산하므로 같이 고정해야 한다.

`discoveredPlanetJson`이 null인 행(업데이트 전에 완료됐지만 아직 결과를 안 본 탐사)은 기존처럼 ViewModel에서 굴린다(특성 없음). 이런 행은 업데이트 전에 보낸 탐사라 모두 T10 이하다.

---

## R2. 특성을 행성에 어떻게 저장하나

**Decision**: `planet_table`에 `traits TEXT NOT NULL DEFAULT ''` 칼럼을 추가하고 `"HIGH_PRODUCTION:3,UNSTABLE_ENVIRONMENT:1"` 형식으로 저장한다. 도메인에서는 `List<PlanetTrait>`(종류 + 강도)로 다룬다. 적합도는 저장하지 않고 `PlanetType × TraitType` 표에서 매번 찾는다. 나쁜 특성의 강도는 항상 1로 저장하고 무시한다.

**Rationale**:
- 이미 같은 형식을 쓰는 곳이 있어서(`resourcesResult`, `astronautIds`) 별도 테이블이나 TypeConverter 없이 맞출 수 있다.
- 적합도는 행성 종류에서 결정되는 값이라 저장하면 표와 어긋날 위험만 생긴다.
- 모르는 이름은 읽을 때 건너뛴다. 나중에 특성을 추가해도 옛 앱이 죽지 않는다.

**Alternatives considered**:
- *별도 `planet_trait_table`*: 행성당 최대 2개라 조인 비용만 늘어난다. 스냅샷에 리스트도 하나 더 생긴다.
- *JSON 배열 칼럼*: 가능하지만 기존 관례(콤마 문자열)와 다르다.

---

## R3. 특성 효과를 한 곳에서 계산하기 (FR-008: 온라인·오프라인·화면이 같은 값)

**Decision**: `Planet`에 특성이 반영된 **확장 프로퍼티**를 추가하고, 기존 코드가 원시 값 대신 이것을 읽게 바꾼다.

| 효과 축 | 지금 읽는 값 | 바꿀 값 (Planet 확장) | 적용 위치 |
|---|---|---|---|
| 생산량 | `normalProductionAtLevel()` 안의 `production × …` | 같은 함수 안에서 `× traitEffects.productionMultiplier` | 수령·미리보기·정비 비용·고장 손해·코인 스킵 계산이 모두 이 함수를 거침 |
| 악재 확률 | `planet.eventRate` | `planet.effectiveBadChance` (eventRate ± %p, 0..100으로 제한) | `RollPlanetEventUseCase`, 상세 화면 "악재 확률" |
| 고장 기준 | `GameConstants.PLANET_BREAKDOWN_DELTA_THRESHOLD` | `planet.breakdownThreshold` (0.15 ± %p) | `RollPlanetEventUseCase.applyEvent` |
| 호재 상승폭 | `delta` | 정상 상태의 호재일 때만 `delta × planet.boomMultiplier` (이후 기존 상한 2.0 그대로) | `applyEvent` |
| 자원 드롭 | `baseDropChance × 등급 × 레벨` | `× planet.resourceDropMultiplier` | `CollectProfitUseCase.rollResourceAmount` 호출부, 상세 화면 드롭 표시 |
| 강화 성공률 | `UPGRADE_SUCCESS_RATES[level]` | `planet.upgradeSuccessRate` (`× 배율`, 상한 0.99) | `UpgradePlanetUseCase`, 상세 화면 성공률 |

**Rationale**: 생산량은 이미 `normalProductionAtLevel` 한 함수가 모든 경로(60초 루프, 오프라인 정산 `PeekPendingProfitUseCase`, 이벤트 적립 `bankedProfitAt`, 정비 비용, `SkipExpeditionWithCoinsUseCase`)의 기준이다. 그래서 여기 하나만 고치면 FR-008이 구조적으로 보장된다. 나머지 축도 같은 방식(확장 프로퍼티 하나)으로 맞추면, "화면은 특성이 반영됐는데 정산은 안 됐다" 같은 드리프트가 생길 자리가 없다.

`applyEvent`는 이미 순수 함수(companion)라, 특성 행성의 고장 기준·호재폭은 기존 `PlanetBreakdownTest`와 같은 방식으로 단위 테스트할 수 있다.

**Alternatives considered**: *`TraitEffects`를 유스케이스마다 따로 조회* — 호출부가 많아(최소 7곳) 누락 위험이 크다.

---

## R4. 특성 가격(FR-010)을 어디에 반영하나

**Decision**: 발견 시 `buyPrice = round(현행가 × (1 + 특성 시세 보정))`으로 **구매가 자체에 넣는다**. 시세 보정은 `PlanetTraitTable`의 "하루 순수익 효과" 값(balance.md 5절, 나쁜 특성은 6절의 행성별 표)을 더해서 구한다. 강화 적성·부적성은 시도 증감율 × 0.4로 넣는다. 이 보정값은 `Planet.traitPriceAdjustment`로 다시 계산할 수 있어서 발견 화면의 "특성 시세 ±n%" 표시에 쓴다.

**Rationale**: 매도가·랭킹·이벤트 시세는 모두 `buyPrice`에서 출발한다(`marketValue = buyPrice + upgradeInvestment + marketAdjustment`, `marketAdjustmentFor`는 `buyPrice`에 비례). 따라서 `buyPrice`에 넣으면 FR-010의 "구매가·매도가·시장가치(랭킹)에 그대로 반영"이 추가 코드 없이 성립한다. "코인으로 받기" 금액(`buyPrice × 비율`)도 같이 따라간다. 좋은 특성이 붙은 행성을 코인으로 받으면 더 받는 것이 자연스럽다.

**Alternatives considered**: *`marketValue` 계산식에 배율 추가* — 시세 이벤트 보정(`marketAdjustmentFor`)과 이중으로 곱해지는 경로가 생기고, 랭킹 동기화 쪽도 따로 손봐야 한다.

**리서치로 메운 빈칸**: balance.md 4절의 나쁜 특성 효과는 범위(예: −7 ~ −9%)로만 적혀 있어 가격에 쓸 행성별 값이 없었다. 같은 시뮬레이션으로 행성별 값을 뽑아(`sim/bad_traits.js`) balance.md 6절에 표로 추가했다.

---

## R5. 특성 굴리기 (FR-005, FR-007)

**Decision**: 순수 Kotlin `PlanetTraitRoller.roll(type, tier, random): List<PlanetTrait>`를 만든다. 티어별 확률표(balance.md 3절)를 따른다.
1. 특성 개수: `random < twoChance[tier]`이면 2개, 아니면 1개.
2. 칸마다: `random < badChance[tier]`이면 나쁜 특성(5종 균등), 아니면 좋은 특성(6종 균등) + 강도(Ⅰ/Ⅱ/Ⅲ 가중치).
3. 이미 뽑힌 특성과 같거나 반대 짝이면 그 칸을 다시 뽑는다. 후보가 넉넉해서 무한 루프는 없지만, 안전장치로 최대 20회까지만 시도한다.

`kotlin.random.Random`을 인자로 받아 테스트에서 시드를 고정한다. 기존 `CompleteExpeditionUseCase`의 `rollPlanetType`과 같은 위치·스타일이다.

**Rationale**: verify.js의 조합 시뮬레이션(balance.md 7절)과 같은 절차라서 SC-004(꽝/대박 비율)를 같은 기준으로 테스트할 수 있다.

---

## R6. 티어 해금 사다리를 T15까지 늘리기 (FR-002, FR-003)

**Decision**: `GameConstants.TierUnlockCondition`을 sealed 계층으로 바꾼다.

```kotlin
sealed interface TierUnlockCondition { val label: String }
data class RarityDiscovered(val rarity: RarityTier, val count: Int, ...)  // T3~T10 (기존 그대로)
data object Part1Completed                                                // T11
data class TraitPlanetsAcquired(val count: Int)                           // T12(10), T14(40)
data class HighFitTraitAcquired(val minIntensity: Int, val count: Int)    // T13(Ⅱ+, 2), T15(Ⅲ, 2)
```

`firstLockedTier(progress: TierProgress, maxTier)`는 `TierProgress(discoveredVariantIds, part1Completed, traitPlanetsAcquired, highFitTier2Acquired, highFitTier3Acquired)`를 받아 지금처럼 1..maxTier를 순서대로 훑는다. 진행도 숫자(`current/required`)도 같은 객체가 계산한다. 탐사 화면의 배너·티어 칩·`StartExpeditionUseCase` 검증이 모두 같은 함수를 쓴다.

**획득 기록 저장**: `user_table`에 정수 칼럼 3개(`traitPlanetsAcquired`, `highFitTier2Acquired`, `highFitTier3Acquired`)를 추가한다. 늘릴 때는 **SQL 원자 증가**(`SET x = x + 1`)를 쓴다. 지금 `recordVariantDiscovery`는 읽고-고치고-쓰는 방식이라 동시 호출에 약한데, 카운터는 그 방식으로 만들지 않는다.

**기록 시점 통합**: `UserRepository.recordPlanetAcquisition(planet)` 하나를 만들어 도감 등록 + 카운터 증가를 함께 한다. 호출하는 곳은 지금 `recordVariantDiscovery`를 부르는 두 곳(`BuyPlanetUseCase`, `ExploreViewModel.convertSlotFullToCoinInternal`)이다. "행성 팔고 구매하기"는 결국 `BuyPlanetUseCase`를 거치므로 따로 처리할 필요가 없다.

**T11 이상 노출**: 1부 완주 전에도 T11~T15 칩을 항상 보여 준다(스펙 US2-5의 "'1부 완주' 조건으로 잠겨"를 택함, 2026-10-14 사용자 결정). 사다리 규칙 그대로라 T10까지 열린 플레이어에게는 T11 칩에 "1부 완주" 조건이 보이고, 다음 해제 목표 배너도 "1부 완주"를 가리킨다. 앞으로 갈 곳이 보여야 1부를 끝까지 갈 동기가 된다.

**Rationale**: 기존 사다리가 "가장 먼저 막힌 티어 앞까지" 규칙과 테스트(`GameConstantsTierLadderTest`)를 이미 갖고 있어서 그대로 확장한다. 매도해도 줄지 않는 누적 카운터는 도감과 같은 성격의 기록이다.

**Alternatives considered**: *보유 행성에서 매번 계산* — 매도하면 잠기므로 스펙 위반이다. *특성 획득 이력 테이블* — 숫자 3개면 충분하다.

---

## R7. 티어 상수 확장 (FR-004)

**Decision**: `EXPEDITION_BASE_MINUTES`, `TIER_LABELS`, `PLANET_RARITY_WEIGHTS`에 11~15를 추가한다. `expeditionTierMultiplier`는 `tier >= 10 → 1.8 + (tier − 10) × 0.05`로 바꾼다(T10 1.80 → T15 2.05, balance.md 1절). 탐사 이벤트 확률(`eventChance`)은 8시간 이상이면 60%라 T12+에도 그대로 적용된다.

**Rationale**: 모두 티어 키 맵이라 값만 추가하면 된다. `ExpeditionEconomyTest`의 "분당 효율 역전 없음" 검사 범위를 1..15로 넓혀 SC-003을 검증한다.

---

## R8. 스토리 6~10장 진행 (FR-012~FR-016)

**Decision**: 기존 `GenerateExpeditionReportUseCase`의 "장 이야기를 다 보고 + 조건 충족 → 한 기록에 한 장씩" 구조를 그대로 확장한다.

- **1부 → 2부 전환**: `currentChapter == 5 && storyCompleted(=1부 완주)`이면 이번 기록을 **6장 1번 기록**으로 만든다(장 마무리 기록 없이 바로). 1부 엔딩 기록 자체가 5장의 마무리 역할을 하기 때문이다. 이미 1부를 완주한 기존 플레이어도 업데이트 후 첫 기록에서 같은 경로로 6장이 시작된다(SC-007).
- **6장 기록 수 세기**: 기존 `countRegularRecordsInChapter(6)`이 장별로 세므로, 완주 후 쌓인 5장 반복 기록은 자연히 빠진다(스펙 Edge Case).
- **장 탈출 조건**: 6 = 이야기만, 7 = T11+ 성공, 8 = T12+ 성공, 9 = T13+ 성공, 10 = T15 성공. 1부처럼 "처음 달성" 플래그로 저장한다(`firstT11Completed` … `firstT15Completed`, 실패 탐사에서도 이미 쌓인 플래그로 따라잡기 가능).
- **2부 완주**: 10장 이야기를 다 보고 T15 성공 플래그가 있으면 `isStoryEnding = true`, `chapter = 10` 기록을 한 번 만들고 `part2Completed = true`로 둔다. **1부/2부 완주 구분은 기록의 장 번호로 판단**한다(5 → "1부 완주", 10 → "2부 완주"). 그래서 `expedition_report_table`은 바꾸지 않는다.
- **막힘·일상 기록 섞기**(FR-014): 장 이야기를 다 쓴 뒤 `k = 장 내 기록 수 − 16`일 때, `k % 3 == 0`이면 그 장의 막힘 문장 `stalled[(k / 3) % n]`, 아니면 정착지 일상 기록 1줄을 낸다. 일상 기록은 장과 무관하게 이어지는 순번 `settlementLogCursor`(StoryProgress에 저장)로 고른다. 순서는 바퀴마다 `Random(바퀴 번호)`로 섞은 순열이라 **한 바퀴 안에서는 중복이 없고** 바퀴마다 순서가 다르다. 새 바퀴의 첫 줄이 직전 바퀴의 마지막 줄과 같으면 첫 줄과 둘째 줄을 바꿔 **바퀴 경계에서도 같은 줄이 연달아 나오지 않게** 한다(2026-10-14 사용자 결정, 저장할 상태 추가 없음). 2부 완주 뒤에는 일상 기록만 낸다.
- **이벤트 풀**(FR-015): `newChapter >= 6`이면 이벤트마다 50% 확률로 2부 공용 8개, 나머지는 기존 카테고리 풀에서 고른다. 5장 전용 풀은 `newChapter == 5`일 때만 쓴다.

**Rationale**: 1부 규칙과 테스트(`GenerateExpeditionReportUseCaseTest`의 FakeStoryRepository)를 그대로 재사용할 수 있다. 막힘 문장 위치는 장 내 기록 수에서 계산되므로 저장할 필요가 없고, 장을 넘나드는 일상 기록 순번만 저장한다.

**영향받는 기존 테스트**: "T10을 달성하면 챕터5는 그대로 유지되고 완주 기록이 딱 한 번 뜬다"의 완주 뒤 기록은 이제 6장이 된다. 이 테스트는 새 규칙에 맞게 고친다.

**Alternatives considered**: *`isStoryEnding`을 정수(`endingPart`)로 바꾸기* — 리포트 엔티티와 스냅샷이 바뀌고 이득이 없다. *일상 기록을 매번 무작위로 고르기* — "한 바퀴 전 중복 없음"(SC-006)을 보장하지 못한다.

---

## R9. 관제탑 2부 안내와 T11 강조 (FR-001)

**Decision**:
- 기존 튜토리얼 오버레이(`TutorialViewModel` + `TutorialOverlay`, 관제탑 NPC 대화창)에 `TutorialStep.Part2StoryIntro(page)`를 추가하고 5줄을 페이지로 넘긴다.
- **노출 조건**: 1부 완주 && 아직 안 봄 && 안 읽은 1부 완주 기록이 없음 && 현재 탭이 탐사. 기존 `!startedFresh → None` 분기보다 **앞에서** 판정한다. 그래야 클라우드에서 복원한 기존 플레이어(startedFresh = false)에게도 뜬다.
- **"봤음" 저장**: `TutorialPrefs`(기기 로컬)가 아니라 `StoryProgress.part2IntroShown`에 저장한다(FR-017, 클라우드 동기화). 기기를 바꿔도 다시 뜨지 않는다.
- **T11 강조**: 티어 칩은 탐사 화면이 아니라, 카테고리를 누르면 열리는 팀 구성 시트(`TeamBuilderContent`) 안의 가로 스크롤에 있다. 그래서 오버레이 스포트라이트로 직접 가리킬 수 없다. 마지막 줄의 스포트라이트는 기존 타깃인 카테고리 카드(`EXPLORE_FIRST_CATEGORY`)에 두고, 팀 구성 시트에서는 **T11을 아직 한 번도 성공하지 않았으면**(`!firstT11Completed`) 티어 줄을 T11까지 스크롤하고 T11 칩에 금색 강조 테두리를 보여 준다. 이 강조는 저장할 상태 없이 진행 상태에서 바로 계산한다.

**Rationale**: 기존 NPC 대화창·스포트라이트를 그대로 쓰고(스펙 Assumptions), 새로 저장할 상태는 "안내를 봤는지" 하나뿐이다.

**Alternatives considered**: *안내가 끝나면 팀 구성 시트를 자동으로 열기* — 어떤 카테고리로 열지 정해야 하고 플레이어의 흐름을 빼앗는다.

---

## R10. DB·스냅샷 버전 (FR-017, FR-018, SC-007)

**Decision**:
- **Room 22 → 23**, `MIGRATION_22_23`에서 `ALTER TABLE … ADD COLUMN`만 한다(모두 기본값 있음).
  - `planet_table.traits` (TEXT, '')
  - `user_table.traitPlanetsAcquired` / `highFitTier2Acquired` / `highFitTier3Acquired` (INTEGER, 0)
  - `story_progress_table.firstT11Completed` / `firstT12Completed` / `firstT13Completed` / `firstT15Completed` / `part2Completed` / `part2IntroShown` (INTEGER, 0), `settlementLogCursor` (INTEGER, 0)
  - `expedition_table.discoveredPlanetJson` (TEXT, NULL)
- **스냅샷 스키마 4 → 5**: 모두 기본값이 있는 필드 추가라 `SnapshotMigrations`에 step을 넣지 않는다(파일 상단 규칙대로). 옛 세이브는 특성 없음·2부 미시작으로 채워진다. 1부를 완주한 세이브는 `storyCompleted = true`라 복원 직후 T11이 열리고, 다음 기록에서 6장이 시작된다.
- **새 세이브를 옛 앱에서 복원**: 옛 앱은 스키마 5를 "미래 버전"으로 보고 복원을 건너뛴다(`SnapshotLoad.Incompatible`, 로컬 데이터 유지). 스펙 Edge Case의 "1부 데이터는 깨지지 않아야"를 **"복원을 건너뛰어 아무것도 덮어쓰지 않음"**으로 충족한다. v1.2.0·v1.5.2 때와 같은 정책이다.
- `storyCompleted` 칼럼 이름은 그대로 둔다(이름을 바꾸면 스냅샷 step이 필요함). 도메인에서는 의미가 드러나게 `part1Completed`로 읽는다.

---

## R11. 검증 방법 (FR-019)

**Decision**: 모두 `app/src/test`의 JVM 단위 테스트로 한다(기존 관례: Mock 프레임워크 없이 Fake 사용).

| 검증 대상 | 테스트 | 기준 |
|---|---|---|
| 적합도 표 무결성 | `PlanetTraitTableTest` | 행성마다 높음 2·낮음 2, 강도 Ⅰ<Ⅱ<Ⅲ, 상한(balance.md 5절) |
| SC-001 적합도 순서 | `PlanetTraitTableTest` | 표의 하루 순수익 값이 행성별로 높음 > 보통 > 낮음, Ⅲ 평균이 밴드 안 |
| 굴리기 규칙 | `PlanetTraitRollerTest` | 반대·중복 없음, T10 이하 0개, 티어별 2개 확률·나쁜 특성 비율이 표와 ±2%p |
| SC-004 꽝/대박 비율 | `PlanetTraitRollerTest` (시드 고정 2만 회) | T11 꽝 약 24%·대박 약 1%, T15 꽝 약 12%·대박 약 26% (±3%p) |
| 효과 반영 | `PlanetTraitEffectsTest` | 축마다 확장 프로퍼티 값, `applyEvent`의 고장 기준·호재폭, 강화 상한 0.99 |
| 가격 | `PlanetTraitEffectsTest` | 항성 + 높은 생산력 Ⅲ → 약 285,000 (balance.md 6절 예시) |
| 사다리 | `GameConstantsTierLadderTest` 확장 | T11~T15 조건, 구멍 규칙, 매도해도 유지 |
| SC-003 분당 효율 | `ExpeditionEconomyTest` 확장 | 1..15 역전 없음, T15 ≈ 24.6/분 |
| 스토리 | `GenerateExpeditionReportUseCaseTest` 확장 | 5→6 전환, 장 조건, 막힘 1:일상 2, 일상 한 바퀴 무중복·연속 중복 없음, 2부 완주 1회, 이벤트 풀 |
| SC-005/006 진행 속도 | `Part2ProgressSimTest` (OrbitDifficultySimTest처럼 시뮬 테스트) | 보통 유저 완주 중앙값 11~17일, 장당 같은 문장 반복 ≤ 2 |
| 세이브 | `SnapshotLoaderTest` 확장 | v4 JSON → v5 기본값, 1부 완주 세이브 유지 |

진행 속도 테스트는 progress_sim.js의 모델(우주선 수·속도·발견률·공백 시간)을 Kotlin으로 옮기되, 특성 굴리기와 사다리 판정은 **실제 앱 코드**(`PlanetTraitRoller`, `firstLockedTier`)를 호출한다. 수치표와 코드가 어긋나면 이 테스트에서 드러난다.

---

## R12. 플레이어용 문구 중 원문이 없는 것

스펙은 특성 정보에 "쉬운 설명 한 줄"과 ⓘ 설명을 요구하지만, story-content.md에는 특성 문구가 없다. **초안을 [contracts/part2-ui-contract.md](contracts/part2-ui-contract.md) 3절에 적었고, 구현 전에 사용자 확인이 필요하다**(설명 문구 규칙: 해요체, 짧게, 뻔한 말 빼기 / "스킨" 대신 "행성").
