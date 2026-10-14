# Implementation Plan: 2부 확장 — 행성 특성 · T11~T15 · 스토리 6~10장

**Branch**: `004-part2-planet-traits` | **Date**: 2026-10-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-part2-planet-traits/spec.md` (+ [balance.md](balance.md), [story-content.md](story-content.md))

## Summary

1부(1~5장, T1~T10)를 완주한 플레이어를 위한 엔드게임 확장이다. 세 가지를 기존 시스템 위에 얹는다.
1. T11 이상에서 발견한 행성에 좋은/나쁜 **특성**을 붙인다. 행성 종류별 **적합도**에 따라 같은 특성이라도 효과가 다르다.
2. **특성 행성 획득 기록**(매도해도 줄지 않는 누적 카운터 3개)으로 T12~T15를 연다.
3. 티어 성공을 따라 **스토리 6~10장**이 진행된다.

새 자원·새 행성 종류·새 외부 서비스는 없다.

기술 접근:
- 효과는 `Planet` 확장 프로퍼티 한 곳에서 계산해 모든 정산·화면 경로가 같은 값을 쓰게 한다(research R3).
- 특성 시세는 발견 시 `buyPrice`에 넣어 매도가·랭킹에 자동 반영한다(R4).
- 발견 행성은 탐사 완료 시 한 번만 굴려 DB에 저장한다. 앱을 다시 켜서 특성을 다시 굴리는 일을 막는다(R1).
- 티어 사다리와 스토리 진행은 기존 규칙(가장 먼저 막힌 티어, 장 이야기 끝 + 조건 → 한 장씩)을 그대로 늘린다(R6, R8).

함께 들어가는 경제 조정(FR-020 행성 강화 자원 −10%, FR-021 천체 분석 자원 −15%·코인 +15%)은 이미 구현·커밋됐다(`4a73623`).

## Technical Context

**Language/Version**: Kotlin (기존 앱과 동일, AGP 관리)

**Primary Dependencies**: Jetpack Compose(BOM), Hilt, Room, kotlinx.serialization(스냅샷·발견 행성 JSON), WorkManager(기존 탐사/이벤트 워커), Firebase Firestore(기존 클라우드 세이브). 새 의존성 없음

**Storage**: Room DB 22 → 23 (칼럼 추가만, data-model 2절), 클라우드 스냅샷 스키마 4 → 5 (필드 추가만, 마이그레이션 step 없음)

**Testing**: JUnit4 JVM 단위 테스트(`app/src/test`), Fake 저장소. 시뮬레이션 성격의 테스트는 기존 `OrbitDifficultySimTest`처럼 시드 고정 Monte Carlo로 작성

**Target Platform**: Android minSdk 24 / compileSdk 36, 단일 `app` 모듈

**Project Type**: Mobile app (기존 Clean Architecture 패키지 구조)

**Performance Goals**: 특성 효과 계산은 정산 루프(60초마다 보유 행성 전체)에서 행성당 상수 시간. 표 조회만 하므로 체감 영향 없음

**Constraints**:
- 온라인·오프라인·화면 값 일치(FR-008)
- 옛 세이브 100% 변환(SC-007)
- T1~T10 밸런스·경험 불변(Assumptions)
- 플레이어 문구는 story-content.md 원문 그대로, 설명 문구 규칙(해요체), "스킨" 대신 "행성"

**Scale/Scope**:
- 특성 11종 × 행성 12종 표, 티어 5개
- 스토리 80줄 + 막힘 24줄 + 일상 20줄 + 이벤트 8개 + 안내 5줄
- 화면 6곳 수정(발견 창, 바꾸기 창, 행성 목록, 행성 상세, 팀 구성 시트, 탐사 일지) + 관제탑 오버레이 1단계

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md`는 아직 템플릿 상태라 프로젝트 헌법 게이트가 없다. 003과 같이, 코드베이스에서 확립된 관례와 사용자 피드백 규칙을 게이트로 쓴다.

- ✅ **도메인/프레젠테이션 분리**: 특성 표·굴리기·효과·사다리·스토리 규칙은 모두 Android 의존 없는 `domain/`에 둔다. 지금 ViewModel 안에 있는 발견 행성 굴리기도 도메인(`GeneratePlanetsUseCase`)으로 옮긴다(R1).
- ✅ **단일 계산 경로**: 효과는 `Planet` 확장 하나로만 계산한다. 시장가치는 기존 `marketValue` 단일 경로를 유지한다(가격은 `buyPrice`에 반영).
- ✅ **세이브 호환 규칙 준수**: Room은 `ADD COLUMN` 마이그레이션, 스냅샷은 `SnapshotMigrations` 파일 규칙(필드 추가 = step 불필요, 버전 +1)을 따른다.
- ✅ **수치는 시뮬레이션으로 검증**(사용자 피드백): 수치표와 실제 코드를 테스트로 대조한다(R11). 진행 속도 테스트는 실제 굴리기·사다리 코드를 호출한다.
- ✅ **불필요한 신규 모듈·서비스 없음**: 새 테이블·Gradle 모듈·SDK 없음.
- ✅ **UI 규칙**: 기존 FacilityPanel/GameDialog/InfoDialog/TutorialOverlay를 재사용한다. 연출은 결정적 순간(발견 리빌)에만 쓴다.

위반 없음 — Complexity Tracking 불필요.

*Post-Phase 1 재평가*: data-model·contracts 작성 후에도 위 게이트가 모두 유지된다. 설계 중 찾은 두 가지를 설계에 반영했다.
- (1) 발견 행성을 다시 굴릴 수 있는 기존 문제(R1)
- (2) 나쁜 특성의 행성별 시세 값이 문서에 없던 빈칸(R4 → balance.md 6절 표 추가)

**남은 열린 항목**: 특성 쉬운 설명과 ⓘ 문구가 초안이라 사용자 확인이 필요하다(R12, contracts/part2-ui-contract.md 3·5절). 구현을 막는 항목은 아니다.

## Project Structure

### Documentation (this feature)

```text
specs/004-part2-planet-traits/
├── spec.md              # 기능 스펙
├── balance.md           # 수치표 (6절 나쁜 특성 행성별 값 추가)
├── story-content.md     # 2부 원문
├── plan.md              # 이 파일
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── part2-domain-api.md
│   └── part2-ui-contract.md
├── sim/                 # 수치 재현 스크립트 (bad_traits.js 추가)
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks 산출물 (이 명령에서는 생성하지 않음)
```

### Source Code (repository root)

```text
app/src/main/java/com/doge/simulator/
├── domain/
│   ├── model/
│   │   ├── PlanetTrait.kt            # [새] TraitType·TraitAxis·TraitFit·PlanetTrait·인코딩
│   │   ├── PlanetTraitTable.kt       # [새] 적합도·강도별 값·하루 순수익 효과 (balance.md 5·6절)
│   │   ├── PlanetTraitRoller.kt      # [새] 티어별 특성 굴리기 (balance.md 3절)
│   │   ├── Planet.kt                 # traits 필드, traitEffects 및 효과 확장, normalProductionAtLevel
│   │   ├── GameConstants.kt          # T11~15 시간·라벨·희귀도·배율, TierUnlockCondition sealed, TierProgress
│   │   ├── StoryProgress.kt          # 6~10장 플래그·part2·커서·장 제목
│   │   ├── StoryContent.kt           # 6~10장 원문, 막힘·일상·2부 이벤트·안내·관제탑 5줄
│   │   └── Expedition.kt             # discoveredPlanet: Planet?
│   ├── repository/
│   │   ├── UserRepository.kt         # recordPlanetAcquisition, 카운터 Flow
│   │   └── StoryRepository.kt        # markPart2IntroShown
│   └── usecase/
│       ├── GeneratePlanetsUseCase.kt          # discoveredPlanet(type, tier) — 스탯+특성+가격
│       ├── CompleteExpeditionUseCase.kt       # 완료 시 행성 확정·저장
│       ├── BuyPlanetUseCase.kt                # recordPlanetAcquisition
│       ├── StartExpeditionUseCase.kt          # TierProgress 기반 검증
│       ├── RollPlanetEventUseCase.kt          # effectiveBadChance·breakdownThreshold·boomMultiplier
│       ├── CollectProfitUseCase.kt            # resourceDropMultiplier
│       ├── UpgradePlanetUseCase.kt            # upgradeSuccessRate()
│       └── GenerateExpeditionReportUseCase.kt # 6~10장, 막힘/일상 섞기, 2부 이벤트, 2부 완주
├── data/
│   ├── local/
│   │   ├── PlanetDatabase.kt         # VERSION 23, MIGRATION_22_23
│   │   ├── entity/{Planet,User,StoryProgress,Expedition}Entity.kt   # 칼럼 추가
│   │   ├── dao/UserDao.kt            # 카운터 원자 증가
│   │   ├── dao/ExpeditionDao.kt      # complete()에 discoveredPlanetJson
│   │   ├── mapper/{Planet,Expedition}Mapper.kt
│   │   └── snapshot/GameSnapshot.kt  # SCHEMA_VERSION 5
│   └── repository/{User,Story,Expedition}RepositoryImpl.kt
└── presentation/
    ├── component/PlanetTraitViews.kt          # [새] PlanetTraitRow·PlanetTraitList·특성 ⓘ
    ├── screen/explore/ExploreScreen.kt        # 발견 창 특성, 바꾸기 창 칩, T11~15 칩·배너·T11 강조
    ├── screen/planet/PlanetScreen.kt          # 목록 특성 칩
    ├── screen/planet/PlanetDetailScreen.kt    # 특성 목록, 반영 값 표시
    ├── screen/expedition/ExpeditionHistoryScreen.kt  # 1부/2부 완주 배지, 6~10장 안내 칸
    ├── viewmodel/ExploreViewModel.kt          # 저장된 발견 행성 사용, TierProgress, 코인으로 받기 기록
    └── tutorial/{TutorialViewModel,TutorialOverlay}.kt  # Part2StoryIntro 단계

app/src/test/java/com/doge/simulator/
├── domain/model/PlanetTraitTableTest.kt       # [새] 표 무결성·적합도 순서·상한 (SC-001)
├── domain/model/PlanetTraitRollerTest.kt      # [새] 규칙·티어 분포·꽝/대박 (SC-004)
├── domain/model/PlanetTraitEffectsTest.kt     # [새] 축별 효과·applyEvent·가격 (SC-002 포함)
├── domain/model/GameConstantsTierLadderTest.kt   # T11~15 사다리
├── domain/ExpeditionEconomyTest.kt            # 1..15 분당 효율 (SC-003)
├── domain/usecase/GenerateExpeditionReportUseCaseTest.kt  # 6~10장 규칙
├── domain/Part2ProgressSimTest.kt             # [새] 진행 속도·반복 횟수 (SC-005, SC-006)
└── data/local/snapshot/SnapshotLoaderTest.kt  # v4 → v5 (SC-007)
```

**Structure Decision**: 기존 단일 `app` 모듈과 Clean Architecture 패키지를 그대로 쓴다. 새 파일은 특성 도메인 3개와 화면 컴포넌트 1개뿐이고, 나머지는 기존 파일을 확장한다. 특성은 행성의 속성이므로 별도 하위 패키지로 떼지 않고 `domain/model`의 `Planet*` 파일 옆에 둔다.

### 구현 순서 (tasks 생성 시 참고)

1. **기반** — 특성 모델·표·굴리기 + 단위 테스트, DB 23·스냅샷 5·엔티티/매퍼. 모든 스토리의 전제
2. **US1 특성 (P1)** — 완료 시 발견 행성 확정·저장, 효과 확장 연결(7개 호출부), 가격, 발견 창·상세·목록 UI
3. **US2 사다리 (P2)** — 획득 카운터·기록 시점, `TierProgress`, T11~15 상수, 배너·칩, 경제 테스트
4. **US3 스토리 (P3)** — StoryContent 원문, 진행 규칙, 2부 이벤트, 탐사 일지 표시, 관제탑 안내
5. **검증** — 진행 속도 시뮬 테스트, 전체 테스트, 실기기 체크리스트(quickstart.md)

US1만으로도 T11 특성 수집 루프가 동작한다(스펙 Independent Test). US2·US3은 US1 위에 독립적으로 얹힌다.

## Complexity Tracking

*Constitution Check에 위반 사항이 없어 이 표는 비워 둔다.*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
