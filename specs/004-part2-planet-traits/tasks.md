---

description: "Task list for 2부 확장 — 행성 특성 · T11~T15 · 스토리 6~10장"
---

# Tasks: 2부 확장 — 행성 특성 · T11~T15 · 스토리 6~10장

**Input**: Design documents from `/specs/004-part2-planet-traits/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md (모두 있음) + balance.md(수치 원본), story-content.md(문장 원본)

**Tests**: 스펙 FR-019가 특성 수치·티어 경제·진행 속도를 자동 테스트로 검증하라고 요구하므로 테스트 작업을 포함한다. 도메인 로직은 테스트를 먼저 쓰고(실패 확인) 구현한다. 방식은 JUnit4 + Fake이며 Mock 프레임워크는 쓰지 않는다(기존 관례).

**Organization**: spec.md의 사용자 스토리(US1 특성 P1, US2 사다리 P2, US3 스토리 P3)별로 묶었다.

## Path Conventions

- `M/` = `app/src/main/java/com/doge/simulator/`
- `T/` = `app/src/test/java/com/doge/simulator/`
- 수치는 반드시 [balance.md](balance.md)에서, 플레이어 문장은 [story-content.md](story-content.md)에서 **그대로** 옮긴다. 화면 문구(특성 설명·숫자 표기·ⓘ·해금 조건)는 [contracts/part2-ui-contract.md](contracts/part2-ui-contract.md) 3~5절을 따른다. 앱에 이미 있는 용어(생산량, 악재/호재, 자원 드롭량, 강화 성공률, 시세, 매도가, T11, 특성 Ⅰ·Ⅱ·Ⅲ = "등급")를 쓴다.
- 빌드·테스트는 Bash에서 `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` 후 `./gradlew :app:testDebugUnitTest -q; echo "exit=$?"`로 실행하고 **종료 코드**로 판단한다.

---

## Phase 1: Setup

**Purpose**: 2부 출시 기준선 맞추기

- [X] T001 `main`을 이 브랜치에 병합해 v1.5.3 버전 커밋(2e18c59, versionCode 13)을 가져오고, 충돌이 없는지와 전체 단위 테스트 통과를 확인 (`app/build.gradle.kts`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 세 스토리가 모두 기대는 모델·표·저장 구조. 특성 모델과 DB/스냅샷이 없으면 어떤 스토리도 시작할 수 없다.

**⚠️ CRITICAL**: 이 단계를 마치기 전에는 사용자 스토리 작업을 시작하지 않는다

### 특성 도메인 모델

- [X] T002 [P] `TraitType`(11종: displayName·isGood·axis·opposite), `TraitAxis`, `TraitFit`(HIGH "높음"/MEDIUM "보통"/LOW "낮음"), `PlanetTrait(type, intensity)`와 `encode`/`decode`(`"HIGH_PRODUCTION:3,UNSTABLE_ENVIRONMENT:1"` 형식, 모르는 이름·깨진 조각은 건너뜀)를 만든다. 정의는 data-model 1.1~1.4. 파일: `M/domain/model/PlanetTrait.kt`
- [X] T003 [P] `PlanetTraitTable`을 만든다. `fit(type, trait)`, `value(type, trait, intensity)`는 balance.md 5절 12×6 표, `badValue(trait)`는 4절 고정값이다. `dailyProfitEffect(type, trait, intensity)`는 5절 하단 표, 나쁜 특성은 6절 행성별 표, 강화 적성은 시도 감소율 × 0.4, 강화 부적성은 −0.08이다. 행 순서는 `PlanetType` enum과 맞춘다(습지=TERRAN_WET, 사막=TERRAN_DRY, 군도=ISLANDS, 무대기=NO_ATMOSPHERE, 가스 I/II=GAS_GIANT_1/2, 소행성=ASTEROID, 빙하=ICE_WORLD, 용암=LAVA_WORLD, 블랙홀=BLACK_HOLE, 은하=GALAXY, 항성=STAR). 파일: `M/domain/model/PlanetTraitTable.kt`

### 저장 구조 (DB 22 → 23, 스냅샷 4 → 5)

- [X] T004 [P] `PlanetEntity`에 `traits: String = ""` 추가 (`M/data/local/entity/PlanetEntity.kt`), `PlanetMapper` 양방향에 `PlanetTrait.encode/decode` 연결 (`M/data/local/mapper/PlanetMapper.kt`), `Planet`에 `traits: List<PlanetTrait> = emptyList()` 필드와 `val Planet.hasTraits` 추가 (`M/domain/model/Planet.kt`)
- [X] T005 [P] `UserEntity`에 `traitPlanetsAcquired`, `highFitTier2Acquired`, `highFitTier3Acquired` 추가(Int = 0, 기존 필드 주석 스타일). 파일: `M/data/local/entity/UserEntity.kt`
- [X] T006 [P] `StoryProgressEntity`와 도메인 `StoryProgress`에 `firstT11Completed`, `firstT12Completed`, `firstT13Completed`, `firstT15Completed`, `part2Completed`, `part2IntroShown`(Boolean = false), `settlementLogCursor`(Int = 0)를 추가하고, `StoryRepositoryImpl`의 toDomain/toEntity 매핑을 늘린다. 도메인에는 `val part1Completed get() = storyCompleted`를 둔다. 파일: `M/data/local/entity/StoryProgressEntity.kt`, `M/domain/model/StoryProgress.kt`, `M/data/repository/StoryRepositoryImpl.kt`
- [X] T007 [P] `ExpeditionEntity`에 `discoveredPlanetJson: String? = null` 추가 (`M/data/local/entity/ExpeditionEntity.kt`), 도메인 `Expedition`에 `discoveredPlanet: Planet? = null` 추가 (`M/domain/model/Expedition.kt`), `ExpeditionMapper`에서 `snapshotJson`으로 `PlanetEntity` 직렬화/역직렬화(실패 시 null) (`M/data/local/mapper/ExpeditionMapper.kt`)
- [X] T008 `PlanetDatabase.VERSION = 23`, `MIGRATION_22_23`을 추가한다(data-model 2절의 `ALTER TABLE … ADD COLUMN` 11개만, 데이터 변환 없음, 기존 마이그레이션처럼 주석 한 줄). `DatabaseModule`의 `addMigrations`에 등록한다. 파일: `M/data/local/PlanetDatabase.kt`, `M/di/DatabaseModule.kt` (T004~T007 이후)
- [X] T009 `GameSnapshot.SCHEMA_VERSION = 5`로 올리고 주석에 "5: 2부 — 행성 특성·획득 카운터·스토리 6~10장·발견 행성 저장(필드 추가만, step 없음)"을 적는다. `SnapshotMigrations`에는 step을 추가하지 않는다. 파일: `M/data/local/snapshot/GameSnapshot.kt`

### 기반 테스트

- [X] T010 [P] `PlanetTraitTableTest`를 작성한다. 검증 항목:
  - 행성마다 좋은 특성 6종 중 적합도 높음 2·낮음 2개
  - 모든 칸에서 Ⅰ < Ⅱ < Ⅲ (악재 축은 절댓값)
  - 상한: 튼튼한 구조 ≤ 0.07, 호황 ≤ 0.6, 풍부한 자원 ≤ 1.5(×2.5), 안정된 환경 ≤ 10, 높은 생산력 ≤ 0.40
  - SC-001: `dailyProfitEffect` Ⅲ의 적합도별 평균이 높음 > 보통 > 낮음이고 각각 약 +29% / +17% / +6% (±3%p). **강화 적성은 빼고** 나머지 5개 축으로만 평균을 낸다(verify.js 1절과 같은 기준, 강화 적성은 수익이 아니라 시도 감소율이라 단위가 다름)
  - 나쁜 특성 값이 모두 음수
  - 파일: `T/domain/model/PlanetTraitTableTest.kt`
- [X] T011 [P] `SnapshotLoaderTest`에 스키마 4 JSON 케이스를 추가한다. 특성·카운터·2부 필드가 없는 v4 세이브가 v5로 `Loaded`되고 기본값으로 채워지는지, `storyCompleted = true`가 유지되는지 확인한다(SC-007). 파일: `T/data/local/snapshot/SnapshotLoaderTest.kt`
- [X] T012 컴파일과 전체 단위 테스트를 통과시킨다. `UserRepository`·`StoryRepository`를 구현한 테스트 Fake(`T/domain/usecase/orbit/FakeOrbitUserRepository.kt`, `T/domain/usecase/GenerateExpeditionReportUseCaseTest.kt`의 FakeStoryRepository)가 이후 인터페이스 변경에도 컴파일되는지 이 시점에 함께 확인한다.

**Checkpoint**: 특성을 저장·복원할 수 있고, 옛 세이브가 그대로 열린다. 아직 특성이 붙는 경로는 없다.

---

## Phase 3: User Story 1 — 특성이 붙은 행성 발견 (Priority: P1) 🎯 MVP

**Goal**: T11 이상에서 발견한 행성에 특성이 붙는다. 효과가 생산·이벤트·고장·자원·강화에 실제로 반영되고, 가격(구매·매도·랭킹)에도 들어가며, 화면에서 확인할 수 있다.

**Independent Test**: 1부 완주 세이브에서 T11 탐사로 특성 행성을 발견한다. 발견 창에 특성 정보가 뜨고, 구매 후 상세 화면의 값과 실제 수익·이벤트·강화 결과가 특성 수치대로 달라지는지 확인한다(quickstart 3~5). 사다리(US2)가 없어도 1부를 완주하면 T11은 열려야 하므로, 이 단계에서는 T11 상수와 `Part1Completed` 조건만 먼저 넣는다(T021).

### Tests for User Story 1 ⚠️ (먼저 작성 → 실패 확인)

- [X] T013 [P] [US1] `PlanetTraitRollerTest`를 작성한다. 검증 항목:
  - T1~T10은 항상 빈 리스트
  - T11+는 1~2개, 같은 종류·반대 짝 없음
  - 시드 고정 2만 회에서 티어별 2개 확률, 칸당 나쁜 특성 비율, 좋은 특성 Ⅰ/Ⅱ/Ⅲ 비율이 balance.md 3절과 ±2%p
  - SC-004: 티어당 2만 개를 **행성 종류부터** 굴린다(`GeneratePlanetsUseCase.rollPlanetType(tier, random)` → `PlanetTraitRoller.roll`). 판정은 앱과 같은 `traitEffects.priceAdjustment`(효과 % 단순 합)로 하고, 꽝/무난/좋음/대박 비율과 평균이 balance.md 7절 **"앱 계산 기준"** 표와 ±2%p 안
  - FR-010 범위: 같은 표본의 시세 보정 최소·최대가 −30% ~ +75% 안
  - 파일: `T/domain/model/PlanetTraitRollerTest.kt`
- [X] T014 [P] [US1] `PlanetTraitEffectsTest`를 작성한다. 검증 항목:
  - 축별 확장 값: 생산 배율이 `normalProductionAtLevel`에 곱해짐, `effectiveBadChance` 0..100 클램프, `breakdownThreshold`, `boomMultiplier`, `resourceDropMultiplier`, `upgradeSuccessRate()` 상한 0.99
  - `RollPlanetEventUseCase.applyEvent`: 튼튼한 구조 행성은 0.20 악재에 고장 나지 않고 약한 구조 행성은 0.13 악재에 고장, 호황 행성의 호재 상승폭 확대, 고장 중 동작 불변
  - 특성 없는 행성은 기존 값과 완전히 같음
  - 가격: 항성 기본가 + 높은 생산력 Ⅲ → `buyPrice` 약 285,000 (balance.md 6절)
  - 고장 중인 특성 행성: `maintenanceCost`와 고장 손해(`preciseProduction`)가 특성이 반영된 정상 생산량 기준(높은 생산력 Ⅲ 행성의 정비 비용이 같은 스탯의 특성 없는 행성보다 그 배율만큼 큼)
  - 파일: `T/domain/model/PlanetTraitEffectsTest.kt`
  - (SC-002 고장 빈도는 확률 하나로는 재현되지 않는다. 고장 난 시간 동안 이벤트 흐름이 바뀌기 때문이다. 그래서 값 상한은 T010, 빈도는 T046 시간 기반 시뮬레이션에서 확인한다)

### Implementation for User Story 1

- [X] T015 [US1] `PlanetTraitRoller.roll(type, tier, random): List<PlanetTrait>`를 구현한다(research R5, balance.md 3절 확률표를 상수 맵으로, 반대·중복이면 그 칸 다시 뽑기, 최대 20회). 파일: `M/domain/model/PlanetTraitRoller.kt` (T013 통과)
- [X] T016 [US1] `TraitEffects`와 `val Planet.traitEffects`를 만들고 효과 확장을 추가한다(data-model 1.6~1.7).
  - `normalProductionAtLevel()`에 `× traitEffects.productionMultiplier`
  - `effectiveBadChance`, `breakdownThreshold`, `boomMultiplier`, `resourceDropMultiplier`, `upgradeSuccessRate()`
  - 파일: `M/domain/model/Planet.kt` (T014 일부 통과)
- [X] T017 [US1] `RollPlanetEventUseCase`에 특성을 반영한다. `isBad` 판정을 `planet.effectiveBadChance`로 바꾸고, `applyEvent`에서 정상 + 호재면 `delta × planet.boomMultiplier`, 고장 판정은 `delta >= planet.breakdownThreshold`로 한다. 파일: `M/domain/usecase/RollPlanetEventUseCase.kt` (T014 통과)
- [X] T018 [P] [US1] `CollectProfitUseCase`의 자원 드롭 기대값에 `planet.resourceDropMultiplier`를 곱한다(`rollResourceAmount` 호출부). 파일: `M/domain/usecase/CollectProfitUseCase.kt`
- [X] T019 [P] [US1] `UpgradePlanetUseCase`의 성공률을 `planet.upgradeSuccessRate()`로 바꾼다. 파일: `M/domain/usecase/UpgradePlanetUseCase.kt`
- [X] T020 [US1] `GeneratePlanetsUseCase.discoveredPlanet(type, tier, random)`을 추가한다(contracts/part2-domain-api 2절). 스탯·변종을 굴리고, `PlanetTraitRoller`로 특성을 붙이고, `buyPrice = round((basePrice + production×20 + risk×10) × (1 + priceAdjustment))`로 정한다. 기존 `generateRandomPlanet`의 스탯 굴리기와 코드를 공유한다. `CompleteExpeditionUseCase`의 private `rollPlanetType(tier)`도 이리로 옮겨 `rollPlanetType(tier, random)`으로 공개한다(T013 SC-004 테스트와 T022가 같은 함수를 씀). 파일: `M/domain/usecase/GeneratePlanetsUseCase.kt`
- [X] T021 [US1] T11을 열 최소 조건을 넣는다.
  - `GameConstants`: `EXPEDITION_BASE_MINUTES`, `TIER_LABELS`, `PLANET_RARITY_WEIGHTS`에 T11~T15 값(balance.md 1절), `expeditionTierMultiplier`를 `tier >= 10 → 1.8 + (tier − 10) × 0.05`로
  - `TierUnlockCondition`을 sealed 계층으로 바꾸고(T3~T10 `RarityDiscovered`, T11 `Part1Completed`, T12~T15는 US2에서 채울 자리), `TierProgress`와 `firstLockedTier(progress, maxTier = 15)` 추가(data-model 1.8)
  - `StartExpeditionUseCase`·`ExploreViewModel`·`ExploreScreen` 배너가 `TierProgress`를 쓰도록 바꾼다. 이 단계에서 T12~T15는 임시로 항상 잠김 처리
  - `GameConstantsTierLadderTest` 기존 케이스를 새 시그니처로 옮긴다
  - 파일: `M/domain/model/GameConstants.kt`, `M/domain/usecase/StartExpeditionUseCase.kt`, `M/presentation/viewmodel/ExploreViewModel.kt`, `M/presentation/screen/explore/ExploreScreen.kt`, `T/domain/model/GameConstantsTierLadderTest.kt`
- [X] T022 [US1] 탐사가 끝날 때 발견 행성을 확정하고 저장한다(research R1).
  - `ExpeditionDao.complete`·`ExpeditionRepository.complete`·`ExpeditionRepositoryImpl`에 `discoveredPlanetJson` 파라미터 추가
  - `CompleteExpeditionUseCase`가 `rollPlanetType` 뒤에 `generatePlanets.discoveredPlanet(type, expedition.tier)`를 호출해 함께 저장
  - 파일: `M/data/local/dao/ExpeditionDao.kt`, `M/domain/repository/ExpeditionRepository.kt`, `M/data/repository/ExpeditionRepositoryImpl.kt`, `M/domain/usecase/CompleteExpeditionUseCase.kt`
- [X] T023 [US1] `ExploreViewModel.presentExpeditionResult`가 `expedition.discoveredPlanet`이 있으면 그대로 쓰게 한다(스탯 재굴림 제거). null이고 `discoveredPlanetType`만 있는 옛 행에서만 기존처럼 굴리되, 그 경로도 `GeneratePlanetsUseCase`를 호출하게 정리한다. "코인으로 받기" 금액은 이 `buyPrice`(특성 시세 포함) 기준이다. 파일: `M/presentation/viewmodel/ExploreViewModel.kt`
- [X] T024 [P] [US1] 공용 특성 UI를 만든다(contracts/part2-ui-contract 1·3·4·5절).
  - `PlanetTraitRow`: 이름 + Ⅰ·Ⅱ·Ⅲ + 적합도 글자, 설명 · 실제 숫자
  - `PlanetTraitList` + ⓘ: `InfoDialog` 재사용, 여러 줄은 BodyReading 글꼴
  - 색은 역할색만(금색 X). 문구는 계약 문서 표 그대로
  - 파일: `M/presentation/component/PlanetTraitViews.kt`
- [X] T025 [US1] 발견 결과 창(`DiscoveryRevealContent`)에서 리빌이 끝난 뒤 `PlanetTraitList`와 가격 줄 옆 "특성 시세 ±n%"를 보여 준다. 행성 바꾸기 창(`SwapPickerDialog`)의 보유 행성 줄에는 특성 이름 칩을 붙인다. 파일: `M/presentation/screen/explore/ExploreScreen.kt`
- [X] T026 [P] [US1] 행성 상세 화면에 `PlanetTraitList`를 추가하고, "악재 확률"·자원 드롭 표·강화 성공률을 특성이 반영된 값(`effectiveBadChance`, `resourceDropMultiplier`, `upgradeSuccessRate()`)으로 바꾼다. 파일: `M/presentation/screen/planet/PlanetDetailScreen.kt`
- [X] T027 [P] [US1] 행성 목록 카드에 특성 이름 칩(최대 2개, 이름표 아이콘 없음)을 붙인다. 파일: `M/presentation/screen/planet/PlanetScreen.kt`
- [X] T028 [US1] 전체 단위 테스트를 통과시킨다(T013·T014 포함). 기존 `PlanetBreakdownTest`·`PlanetPendingProfitTest`·`ResourceCostTest`가 특성 없는 행성에서 결과가 그대로인지 확인한다.

**Checkpoint**: 1부 완주 → T11 탐사 → 특성 행성 발견·구매·효과까지 동작한다. 앱을 껐다 켜도 같은 발견 행성이 나온다(quickstart 1·3·4·5).

---

## Phase 4: User Story 2 — 특성 행성 획득 기록으로 다음 항로 열기 (Priority: P2)

**Goal**: 특성 행성을 획득(구매 / 슬롯이 가득 찼을 때 "코인으로 받기")할 때마다 누적 카운터가 오르고, T12~T15가 사다리로 열린다. 행성을 팔아도 잠기지 않는다.

**Independent Test**: 카운터를 조건 직전까지 채운 상태에서 획득 1건으로 다음 티어가 열리고, 행성을 팔아도 유지되는지 확인한다(quickstart 6~8).

### Tests for User Story 2 ⚠️

- [X] T029 [P] [US2] `GameConstantsTierLadderTest`에 케이스를 추가한다.
  - 1부 미완주면 T11에서 막힘
  - 특성 행성 9 → T12 막힘 / 10 → 열림
  - T13 Ⅱ+ 2개, T14 40개, T15 Ⅲ 2개
  - 구멍 규칙: T14 조건만 채우고 T13이 미달이면 T13에서 막힘
  - `progressFor(tier)` 값
  - 파일: `T/domain/model/GameConstantsTierLadderTest.kt`
- [X] T030 [P] [US2] `ExpeditionEconomyTest`의 티어 범위를 1..15로 넓혀 분당 성공 코인이 줄지 않는지 확인한다(SC-003, T15 ≈ 24.6/분). 파일: `T/domain/ExpeditionEconomyTest.kt`

### Implementation for User Story 2

- [X] T031 [US2] `UserDao`에 원자 증가 쿼리 `incrementTraitAcquisition(t2: Int, t3: Int)`(`SET traitPlanetsAcquired = traitPlanetsAcquired + 1, highFitTier2Acquired = highFitTier2Acquired + :t2, …`)와 카운터 Flow를 추가한다. `UserRepository`에 `recordPlanetAcquisition(planet)`(도감 등록 + 특성 행성이면 카운터 증가, data-model 4절)과 `getTraitAcquisitionCounts(): Flow<TraitAcquisitionCounts>`를 추가하고 `UserRepositoryImpl`에서 구현한다. 테스트 Fake(`T/domain/usecase/orbit/FakeOrbitUserRepository.kt`)에도 추가한다. 파일: `M/data/local/dao/UserDao.kt`, `M/domain/repository/UserRepository.kt`, `M/data/repository/UserRepositoryImpl.kt`
- [X] T032 [US2] 획득 기록 시점을 통일한다. `BuyPlanetUseCase`의 `recordVariantDiscovery`와 `ExploreViewModel.convertSlotFullToCoinInternal`의 `recordVariantDiscovery`를 `recordPlanetAcquisition(planet)`으로 바꾼다. 발견 창을 그냥 닫는 경로(`dismissResultInternal`)는 호출하지 않는다. 파일: `M/domain/usecase/BuyPlanetUseCase.kt`, `M/presentation/viewmodel/ExploreViewModel.kt`
- [X] T033 [US2] T12~T15 조건(`TraitPlanetsAcquired(10/40)`, `HighFitTraitAcquired(2, 2)`, `HighFitTraitAcquired(3, 2)`)을 `TIER_UNLOCK_CONDITIONS`에 넣고, T021의 임시 잠금을 없앤다. 라벨은 contracts/part2-ui-contract 3절 해금 조건 표 문구를 쓴다. 파일: `M/domain/model/GameConstants.kt` (T029 통과)
- [X] T034 [US2] `ExploreViewModel`이 도감·스토리 진행·획득 카운터 Flow를 묶어 `TierProgress`를 내보내게 하고, `StartExpeditionUseCase`는 같은 입력을 읽어 검증한다. 파일: `M/presentation/viewmodel/ExploreViewModel.kt`, `M/domain/usecase/StartExpeditionUseCase.kt`
- [X] T035 [US2] 탐사 화면의 "다음 해제 목표" 배너가 T11~T15 조건과 진행도("특성 행성 7 / 10" 등)를 보여 주게 한다. 팀 구성 시트의 티어 줄은 `(1..15)`로 늘리고, 잠긴 다음 티어 칩에만 조건 문구를 붙인다(1부 미완주면 T11에 "1부 완주"). 파일: `M/presentation/screen/explore/ExploreScreen.kt`
- [X] T036 [US2] 전체 단위 테스트를 통과시킨다(T029·T030 포함).

**Checkpoint**: 특성 행성을 모으면 T12 → T15가 차례로 열리고, 매도해도 유지된다.

---

## Phase 5: User Story 3 — 2부 스토리 6~10장 진행 (Priority: P3)

**Goal**: 1부 엔딩 직후 관제탑이 2부를 안내하고, 탐사 기록마다 6~10장 이야기가 이어진다. 조건 대기 중에는 막힘 1 : 일상 2로 섞이고, T15 성공으로 2부 완주 기록이 나온다.

**Independent Test**: 1부 완주 세이브로 기록을 쌓아 5→6, 6→7 전환, 조건 미달 시 막힘·일상 혼합, T15 성공 시 2부 완주가 나오는지 확인한다(quickstart 1·9).

### Tests for User Story 3 ⚠️

- [X] T037 [P] [US3] `GenerateExpeditionReportUseCaseTest`에 케이스를 추가하고 기존 케이스를 고친다. 기존 "T10을 달성하면 챕터5는 그대로 유지…"의 완주 뒤 기록은 이제 6장 1번이다. 추가할 케이스:
  - 1부 완주 상태의 다음 기록이 6장 1번
  - 6장 16개 → 마무리 → 7장
  - 7~10장 조건 미달 시 `k % 3 == 0`이면 막힘, 그 외 일상
  - 일상 40줄은 한 바퀴 안에서 중복 없음, 바퀴 경계(40번째 → 41번째)를 포함해 같은 줄이 연달아 나오지 않음(커서 0~199 전체 확인)
  - 조건을 몰아서 채워도 한 기록에 한 장씩
  - 10장 + T15 → 2부 완주(`isStoryEnding`, chapter 10) 1회, 이후 일상만
  - 6장 이상에서는 5장 전용 이벤트가 나오지 않음
  - 파일: `T/domain/usecase/GenerateExpeditionReportUseCaseTest.kt`

### Implementation for User Story 3

- [X] T038 [P] [US3] `StoryContent`에 2부 원문을 추가한다(story-content.md에서 글자 그대로).
  - `recordTitlePool[6..10]`, `chapterEndingTitles[6..9]`, `part2EndingTitle`
  - `stalledRecordTitlePool[7..10]`, `settlementLogPool`(40줄)과 `settlementLog(cursor)`(바퀴 번호 시드 순열, 새 바퀴 첫 줄이 직전 바퀴 마지막 줄과 같으면 첫·둘째 줄 교체)
  - `part2EventPool`(8개, 8번은 선택지 3개)과 `randomPart2Event(expeditionId)`
  - `nextStoryHints[6..10]`, `part2IntroLines`(5줄, 2번째 줄은 "T11부터")
  - 파일: `M/domain/model/StoryContent.kt`
- [X] T039 [P] [US3] `StoryProgress`의 `chapterTitle`(6 붉은 돔 / 7 균열 / 8 다시 바깥으로 / 9 재회 / 10 귀환 신호)과 `isCurrentChapterExitMet`(6장 true, 7 T11, 8 T12, 9 T13, 10 T15)을 늘린다. 파일: `M/domain/model/StoryProgress.kt`
- [X] T040 [US3] `GenerateExpeditionReportUseCase`를 확장한다(research R8, contracts/part2-domain-api 6절).
  - T11/T12/T13/T15 마일스톤 플래그
  - `chapter == 5 && part1Completed` → 6장으로 시작
  - 6~9장 마무리, 10장 2부 완주
  - 막힘·일상 섞기와 `settlementLogCursor` 증가
  - 2부 완주 후 일상만
  - 이벤트: `newChapter >= 6`이면 이벤트마다 50% `randomPart2Event`, 나머지 카테고리 풀
  - 파일: `M/domain/usecase/GenerateExpeditionReportUseCase.kt` (T037 통과)
- [X] T041 [US3] 탐사 일지 표시를 바꾼다(contracts/part2-ui-contract 7절).
  - 완주 배지: `isStoryEnding && chapter <= 5` → "1부 완주", `chapter >= 10` → "2부 완주"
  - ~~"다음 이야기" 칸~~ — 2026-10-14 칸 자체를 제거(사용자 결정)
  - 2부 완주 후에는 숨김
  - 파일: `M/presentation/screen/expedition/ExpeditionHistoryScreen.kt`, 필요하면 `M/presentation/viewmodel/ExpeditionHistoryViewModel.kt`
- [X] T042 [US3] `StoryRepository.markPart2IntroShown()`을 추가하고 구현한다(진행 상태의 `part2IntroShown = true` 저장). 테스트 FakeStoryRepository에도 추가한다. 파일: `M/domain/repository/StoryRepository.kt`, `M/data/repository/StoryRepositoryImpl.kt`, `T/domain/usecase/GenerateExpeditionReportUseCaseTest.kt`
- [X] T043 [US3] 관제탑 2부 안내를 붙인다(research R9, contracts/part2-ui-contract 6절).
  - `TutorialStep.Part2StoryIntro(page)` 추가. 조건은 1부 완주 && `!part2IntroShown` && 안 읽은 1부 완주 기록 없음 && 탐사 탭이고, **`!startedFresh` 분기보다 앞에서** 판정한다
  - `TutorialViewModel`이 `StoryRepository.getProgress()`와 미읽음 기록을 구독한다
  - `TutorialOverlay`는 5줄을 페이지로 넘기고, 5번째 줄에 `EXPLORE_FIRST_CATEGORY` 스포트라이트를 둔다. 마지막에 `markPart2IntroShown()`
  - 파일: `M/presentation/tutorial/TutorialViewModel.kt`, `M/presentation/tutorial/TutorialOverlay.kt`
- [X] T044 [US3] 팀 구성 시트에서 1부 완주 && `!firstT11Completed`인 동안 티어 줄을 T11까지 스크롤(`LazyListState.animateScrollToItem`)하고 T11 칩에 강조 테두리를 보여 준다. 파일: `M/presentation/screen/explore/ExploreScreen.kt`
- [X] T045 [US3] 전체 단위 테스트를 통과시킨다(T037 포함).

**Checkpoint**: 1부 완주 → 관제탑 안내 → 6장 → … → T15 성공 → 2부 완주까지 이어진다.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T046 [P] `Part2ProgressSimTest`를 작성한다(research R11). progress_sim.js 모델(가벼운/보통/헤비: 우주선 2/4/6척, 속도 30/35/45%, 발견률 30/40/55%, 공백 4/2/0.7h, 성공률 85/90/93%)을 Kotlin으로 옮기되, 특성 굴리기와 사다리 판정은 실제 `PlanetTraitRoller`·`TierProgress`를 호출한다.
  - SC-005: 보통 유저 2부 완주 중앙값 11~17일, 9장·10장에서 조건 대기 발생
  - SC-006: 보통 유저 기준 장 하나에서 같은 막힘·일상 문장 반복 ≤ 2
  - SC-002: 튼튼한 구조 Ⅲ(각 행성의 그 값)과 안정된 환경 Ⅲ 행성의 하루 고장 횟수가 특성 없는 같은 행성의 56% 이상(verify.js 2절: 최저 항성 튼튼한 구조 56%가 경계값이라, 난수 차이를 감안해 테스트 단언은 54% 이상). 실제 `RollPlanetEventUseCase.applyEvent`와 `planetEventIntervalHours`로 시간을 흘리고, 고장 후 0.5~8시간 안에 정비한다고 가정한다(verify.js 2절과 같은 조건)
  - 시드 고정, 유형당 300회 정도로 테스트 시간을 짧게
  - 파일: `T/domain/Part2ProgressSimTest.kt`
- [X] T047 [P] 화면 문구를 점검한다. 새 화면 문자열에 "나쁜 소식", "스킨", "항로"(관제탑·기록 원문 제외), "11티어" 같은 기존과 다른 표현이 없는지, 특성 Ⅰ·Ⅱ·Ⅲ을 "등급"으로 부르는지, ⓘ가 해요체로 짧은지 확인한다. 대상: `M/presentation/`
- [X] T048 TODO.md에 2부 진행 섹션을 추가한다(구현 완료 항목, 실기기 확인 대기 목록 = quickstart 표 10개, 미정 사항: 화면 배치 시안, Ⅲ 발견 연출 여부, versionName). 파일: `TODO.md`
- [X] T049 컴파일·전체 단위 테스트를 최종 확인하고(종료 코드 기준), quickstart.md의 node 스크립트(verify.js / bad_traits.js / progress_sim.js)로 수치 근거가 재현되는지 확인한다.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 먼저
- **Foundational (Phase 2)**: Setup 이후. 모든 스토리를 막는다
- **US1 (Phase 3)**: Foundational 이후. MVP
- **US2 (Phase 4)**: Foundational + T021(사다리 구조) 이후. US1의 발견·구매 경로(T022~T023)를 써서 카운터를 올리므로 US1 다음이 자연스럽다
- **US3 (Phase 5)**: Foundational 이후 시작할 수 있다. 스토리 진행 자체는 특성과 무관하다. 다만 7~10장 조건(T11+ 성공)을 실제로 밟으려면 T021의 T11 개방이 필요하다
- **Polish (Phase 6)**: 세 스토리 이후. T046은 US1(굴리기)과 US2(사다리)가 필요하다

### Within Each User Story

- 테스트를 먼저 쓰고 실패를 확인한 뒤 구현한다
- 모델·표 → 유스케이스 → ViewModel → 화면 순서
- 같은 파일을 고치는 작업(`ExploreScreen.kt`: T021·T025·T035·T044 / `ExploreViewModel.kt`: T021·T023·T032·T034 / `GameConstants.kt`: T021·T033)은 순서대로 한다

### Parallel Opportunities

- Phase 2: T002·T003 (새 파일), T004·T005·T006·T007 (서로 다른 엔티티), T010·T011 (서로 다른 테스트)
- US1: T013·T014 (테스트), T018·T019 (다른 유스케이스), T024·T026·T027 (다른 화면 파일)
- US2: T029·T030
- US3: T037·T038·T039
- US3는 Foundational 이후 US1과 병렬로 진행할 수 있다(파일이 겹치는 곳은 `ExploreScreen.kt`의 T044뿐이다)

## Parallel Example: User Story 1

```text
# 테스트 먼저 (동시에):
T013 PlanetTraitRollerTest
T014 PlanetTraitEffectsTest

# 효과 연결 (T016 이후 동시에):
T018 CollectProfitUseCase 자원 드롭
T019 UpgradePlanetUseCase 성공률

# 화면 (T020~T023 이후 동시에):
T024 PlanetTraitViews.kt
T026 PlanetDetailScreen.kt
T027 PlanetScreen.kt
```

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1~2: 병합, 모델·표·DB·스냅샷
2. Phase 3: 특성 발견·효과·가격·화면 + T11 개방
3. **멈추고 검증**: 단위 테스트, 사용자가 실기기 확인(quickstart 1·3·4·5)
4. 여기까지만으로도 "1부 완주 후 T11에서 특성 행성 수집" 루프가 동작한다

### Incremental Delivery

1. US1 → T11 특성 수집 (MVP)
2. US2 → T12~T15 사다리 (성장 목표)
3. US3 → 6~10장 이야기와 관제탑 안내 (동기와 마무리)
4. Polish → 진행 속도 시뮬 테스트, 문구 점검, TODO

세 스토리를 모두 마친 뒤 한 번에 출시한다(2부는 스토리가 함께 있어야 완결). 출시 시 versionCode 14 이상, versionName은 사용자가 정한다(1.6.0 제안).

## Notes

- [P] = 다른 파일, 앞선 미완료 작업에 의존하지 않음
- 실기기 앱 DB는 adb로 고치지 않는다. 실기기 확인은 사용자가 직접 한다
- 커밋은 사용자가 요청할 때만, 논리적 묶음 단위로 한다
