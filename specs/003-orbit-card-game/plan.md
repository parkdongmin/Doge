# Implementation Plan: ORBIT — Crew Lounge Card Mini-Game

**Branch**: `003-orbit-card-game` | **Date**: 2026-09-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-orbit-card-game/spec.md`

## Summary

정거장 탭에 새로운 "휴게실" 구역을 추가하고, B-01 로봇을 상대로 하는 러브레터류 1v1 카드게임 ORBIT을 구현한다. 기획 원문 16번 항목의 지시대로 게임 로직(카드/덱/턴/이펙트/라운드·매치 판정/B-01 AI)을 UI와 완전히 분리한 순수 Kotlin 도메인 계층으로 먼저 구현하고 유닛 테스트로 검증한 뒤, 그 위에 Compose UI(휴게실/베팅/게임/결과 화면)와 베팅·일일 리워드 광고를 얹는다. 기존 프로젝트의 Clean Architecture 계층 구조(domain/usecase → data/repository → presentation/viewmodel·screen), Hilt DI, AdMob 리워드 광고 인프라(`RewardedAdManager`, `AdFrequencyGate`)를 그대로 재사용하고, 새 백엔드/외부 서비스는 도입하지 않는다.

## Technical Context

**Language/Version**: Kotlin (기존 앱과 동일한 컴파일러/JVM 타깃, Android Gradle Plugin 관리)

**Primary Dependencies**: Jetpack Compose(BOM), Hilt(DI), AndroidX Navigation(기존 `NavRoutes`/`BottomNavItem` 확장), Google Mobile Ads SDK(기존 `RewardedAdManager`/`AdFrequencyGate` 재사용), kotlin.random.Random(셔플/AI 확률 처리 — 기존 `GenerateExpeditionReportUseCase`와 동일한 방식)

**Storage**: 진행 중인 매치/라운드 상태는 영속화하지 않는 인메모리 상태(스펙에서 "이탈 시 그냥 패배 처리, 재개 지원 없음"으로 확정됨). 재화(지갑) 차감/지급은 기존 재화 저장소를 재사용. 일일 리워드 광고 시청 횟수는 `AdFrequencyGate`와 동일하게 `SharedPreferences` 로컬 저장(클라우드 세이브 미동기화, 스펙에서 확정됨)

**Testing**: JUnit4 기반 순수 Kotlin 유닛 테스트(`app/src/test`) — 프로젝트 기존 관례를 따라 Mock 프레임워크 없이 도메인 로직을 직접 테스트. Android 프레임워크 의존이 없는 도메인 계층부터 유닛 테스트로 완결시킨 뒤 UI를 얹는 순서를 유지한다.

**Target Platform**: Android (minSdk 24 / compileSdk 36, 기존 앱과 동일한 단일 앱 모듈)

**Project Type**: Mobile app — 기존 `app` 모듈 내부에 신규 기능 패키지를 추가(별도 모듈 분리 없음)

**Performance Goals**: 턴 진행/카드 이펙트 반영에 UI 상에서 체감 가능한 지연이 없어야 함(로컬 연산이므로 네트워크 대기 없음). 라운드 1회 30초~1분, 매치 1회 2~4분(SC-002, SC-003)이라는 사용자 경험 목표를 게임 로직/연출 타이밍이 방해하지 않아야 함

**Constraints**: 게임 판정 로직 자체는 네트워크 연결 없이 완결되어야 함(광고 시청과 재화 저장소 접근만 예외). 게임 진행 중 기존 BottomNavigation을 숨기되 시스템 뒤로가기는 "이탈=패배" 규칙을 트리거해야 함(FR-016)

**Scale/Scope**: 로컬 단일 플레이어 vs 고정 CPU 상대(B-01) 1개, 동시 접속/서버 확장성 이슈 없음. 위험도 티어 3종 × 고정 베팅 금액 4종의 조합을 지원

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md`가 아직 템플릿 placeholder 상태(프로젝트 고유 원칙이 채워지지 않음)이므로 이 기능에 적용할 프로젝트 헌법 게이트가 존재하지 않는다. 대신 기존 코드베이스에서 관찰되는 확립된 관례를 게이트로 삼아 계획을 검증했다:

- ✅ **도메인/프레젠테이션 분리**: 게임 로직(Card/Deck/Round/Match/AI)은 Android·Compose 의존 없는 순수 Kotlin으로 구현 — 기존 `domain/usecase` 관례와 일치.
- ✅ **DI 일관성**: 신규 매니저/게이트는 기존처럼 Hilt `@Singleton` + `@Inject constructor`로 제공 — `AdFrequencyGate`/`RewardedAdManager` 패턴과 동일.
- ✅ **테스트 우선 검증 가능성**: 도메인 계층이 Android 의존 없이 순수 함수/클래스로 구성되어 `app/src/test`에서 즉시 유닛 테스트 가능 — 기획 원문이 요구하는 "UI 전에 유닛 테스트로 게임 전체 검증" 순서를 그대로 지원.
- ✅ **불필요한 신규 모듈/외부 서비스 없음**: 새 백엔드, 새 DB, 새 서드파티 SDK를 도입하지 않음(기존 AdMob/Hilt/Room 인프라만 재사용) — 단순성 원칙에 부합.

위반 사항 없음 — Complexity Tracking 불필요.

*Post-Phase 1 재평가*: `data-model.md`/`contracts/`/`quickstart.md` 작성 후에도 신규 외부 서비스·모듈·DB 스키마가 추가되지 않았고, 도메인/프레젠테이션 경계(특히 "AI는 정보를 내부에 유지하되 UI에는 노출하지 않는다"는 원칙, research.md #6)도 계층 분리로 해결되어 위 4개 게이트 모두 유지됨. 재확인 완료.

## Project Structure

### Documentation (this feature)

```text
specs/003-orbit-card-game/
├── plan.md              # 이 파일
├── research.md          # Phase 0 산출물
├── data-model.md        # Phase 1 산출물
├── quickstart.md        # Phase 1 산출물
├── contracts/           # Phase 1 산출물
│   ├── orbit-domain-api.md
│   └── orbit-viewmodel-ui-contract.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks 산출물 (이 명령에서는 생성하지 않음)
```

### Source Code (repository root)

기존 단일 Android 앱 모듈(`app`) 안에, 기존 feature별 패키지 분리 관례를 따라 `orbit` 하위 패키지로 격리한다(별도 Gradle 모듈 분리는 이 규모에 과함).

```text
app/src/main/java/com/doge/simulator/
├── domain/
│   ├── model/orbit/
│   │   ├── OrbitCardType.kt          # 8종 카드 종류 + Power(enum)
│   │   ├── OrbitCard.kt              # 카드 인스턴스
│   │   ├── OrbitDeck.kt              # 셔플/드로우/사용 카드 내부 추적
│   │   ├── OrbitPlayerState.kt       # 손패/OUT/SHIELD 보호 상태
│   │   ├── OrbitRoundState.kt        # 라운드 진행 상태 + 종료 사유
│   │   ├── OrbitMatchState.kt        # SIGNAL 누적, 라운드 이력
│   │   ├── OrbitRiskTier.kt          # 위험도 3단계(AI 실수 빈도 + 보상 배율)
│   │   └── OrbitBet.kt               # 베팅 금액 + 위험도 티어 + 결과
│   ├── ai/
│   │   └── B01OrbitAi.kt             # 공개 정보만으로 판단하는 CPU 의사결정 로직
│   └── usecase/orbit/
│       ├── StartOrbitMatchUseCase.kt
│       ├── PlayOrbitCardUseCase.kt        # 턴 1회(드로우+카드 사용+이펙트) 처리
│       ├── ResolveOrbitRoundEndUseCase.kt # OUT/덱소진 비교/DRAW 판정
│       ├── SettleOrbitBetUseCase.kt       # 매치 종료 시 재화 반영
│       └── ClaimOrbitDailyAdRewardUseCase.kt
├── ads/
│   └── OrbitDailyRewardGate.kt       # AdFrequencyGate와 동일 패턴, 일 5회+로컬 자정 리셋
├── data/
│   └── (신규 Room 테이블 없음 — 기존 재화 저장소만 사용)
└── presentation/
    ├── screen/orbit/
    │   ├── LoungeScreen.kt           # 휴게실(정거장 하위 진입점, 리워드 광고 버튼 포함)
    │   ├── OrbitBetScreen.kt         # 베팅 금액 + 위험도 티어 선택
    │   ├── OrbitGameScreen.kt        # 실제 카드 게임 화면
    │   └── OrbitResultScreen.kt      # 승/패 결과 + 다시하기/휴게실로
    └── viewmodel/
        └── OrbitViewModel.kt

app/src/test/java/com/doge/simulator/domain/
├── model/orbit/OrbitDeckTest.kt
├── usecase/orbit/PlayOrbitCardUseCaseTest.kt        # 8종 카드 이펙트별 케이스
├── usecase/orbit/ResolveOrbitRoundEndUseCaseTest.kt # 덱소진 3단계 비교, DRAW 포함
├── usecase/orbit/SettleOrbitBetUseCaseTest.kt
└── ai/B01OrbitAiTest.kt                              # 실수 빈도 파라미터별 동작
```

**Structure Decision**: 기존 앱과 동일한 단일 모듈·Clean Architecture 패키지 구조를 그대로 따르되, 모든 ORBIT 관련 파일을 `orbit` 하위 패키지(또는 파일명 접두사)로 묶어 기존 기능과 섞이지 않게 격리한다. 신규 Gradle 모듈이나 신규 외부 서비스는 도입하지 않는다.

## Complexity Tracking

*Constitution Check에 위반 사항이 없어 이 표는 비워둔다.*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
