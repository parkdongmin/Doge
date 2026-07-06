# 구현 계획서: 우주 탐사 타이쿤

**브랜치**: `main` | **작성일**: 2026-06-08 | **명세서**: [spec.md](spec.md)

---

## 네비게이션 구조 (확정)

### 하단 탭 4개

| 탭 | 아이콘 | 역할 |
|----|--------|------|
| 탐사 | 🚀 | 진행 중인 원정 관리 + 새 탐사 파견 |
| 행성 | 🪐 | 보유 행성 목록 + 강화 |
| 본부 | 🏢 | 우주인·우주선·연구소 허브 |
| 자산 | 💰 | 코인·자원 현황 + 회사 점수 |

### 탭별 내부 구조

```
탐사 탭
├── 진행 중인 원정 목록
└── "파견하기" 버튼
    └── 팀 구성 바텀시트
        ├── Step 1: 탐사 카테고리 + 티어 선택
        ├── Step 2: 우주인 선택
        ├── Step 3: 우주선 선택
        └── Step 4: 확인 + 파견

행성 탭
├── 행성 목록
└── 행성 상세 화면
    └── 강화 UI (레벨, 성공률, 재료 표시)

본부 탭
├── 허브 화면 (카드 3개)
├── 우주인 카드 → 우주인 서브화면 (고용·훈련 관리)
├── 격납고 카드 → 우주선 서브화면 (구매·강화)
└── 연구소 카드 → 연구소 서브화면 (4개 분야 레벨업)

자산 탭
├── 자산 대시보드 (코인, 자원, 회사 점수)
└── "랭킹 보기" 버튼 → 랭킹 모달
    └── 종합 점수 기준 (자산 + 탐사 횟수 + 행성 레벨 합산)
```

### 랭킹 점수 산정 방식
- **종합 회사 점수** = 보유 코인 + 행성 가치 합산 + 탐사 완료 횟수 × 가중치 + 행성 레벨 합산 × 가중치
- 하단 탭에서 제외, 자산 탭 내 모달로 진입

---

## 기존 코드 처리 방침

### ✅ 유지
| 파일 | 처리 |
|------|------|
| Clean Architecture 구조 전체 | 그대로 유지 |
| Hilt / Room / Firebase 설정 | 그대로 유지 |
| `ui/theme/` (Color, Type, Theme) | 그대로 유지 |
| Auth 플로우 (Splash, Login, AuthViewModel) | 그대로 유지 |
| Leaderboard (Rank 화면) | 그대로 유지 |
| `UserEntity`, `UserDao`, `UserRepository` | 그대로 유지 (코인 관리) |
| `PlanetType`, `RarityTier`, `PlanetVariant`, `PlanetMetaDataTable` | 그대로 유지 |
| `BuyPlanetUseCase`, `GetOwnedPlanetsUseCase` | 그대로 유지 |
| `ExplorationStore`, `ExplorationCompleteWorker` | 그대로 유지 |
| `PixelComponents` | 그대로 유지 |

### ✏️ 수정
| 파일 | 변경 내용 |
|------|-----------|
| `Planet` 모델 | `status`, `maintenanceCost` 제거 → `level`, `upgradeInvestment` 추가 |
| `PlanetEntity` | 동일 필드 변경, DB 마이그레이션 |
| `PlanetMapper` | 신규 필드 반영 |
| `PlanetDao` | 상태 관련 쿼리 제거 |
| `SellPlanetUseCase` | 매도가 = 매입가 + upgradeInvestment 로 변경 |
| `GeneratePlanetsUseCase` | 탐사 카테고리·티어 연동 |
| `CollectProfitUseCase` | 유지비 로직 제거, 단순 수익 누적만 |
| `ExploreScreen` | 탐사팀 구성 UI로 전면 개편 |
| `PlanetScreen` | 수리 버튼 제거, 강화 버튼 추가 |
| `PlanetDetailScreen` | 수리 제거, 강화 시스템 UI 추가 |
| `NavRoutes` | 신규 화면 라우트 추가 |
| `BottomNavItem` | 탐사·행성·본부·자산 4개로 재편 |
| `MainScreen` | 네비게이션 재편 |
| `GameConstants` | 수수료율, 강화 확률 테이블 추가 |
| `DatabaseModule` | 신규 바인딩 추가 |
| `PlanetDatabase` | v5 마이그레이션 |

### ❌ 제거
| 파일 | 이유 |
|------|------|
| `PlanetStatus` (MAINTENANCE_REQUIRED, LOSS) | 손실 상태 시스템 제거 |
| `PlanetMaintenanceWorker` | 유지비 시스템 제거 |
| `MarketEventWorker` | 시세 변동 시스템 제거 |
| `MarketEvent`, `MarketEventEntity`, `MarketEventDao`, `MarketEventMapper` | 동일 |
| `MarketEventRepository`, `MarketEventRepositoryImpl` | 동일 |
| `UpdatePlanetStatusUseCase` | 상태 전환 시스템 제거 |
| `RepairPlanetUseCase` | 수리 시스템 제거 |
| `ApplyMarketEventUseCase`, `GetMarketEventsUseCase` | 시세 시스템 제거 |
| `SpaceStationScreen` | HQScreen으로 대체 |
| `HomeScreen` | 불필요 (HQ가 허브 역할) |
| `FeedScreen`, `FeedViewModel` | 탐사 기록 화면으로 대체 |

---

## 신규 구현 목록

### 도메인 모델
```
domain/model/
├── AstronautSpecialty.kt     새 enum (MINERAL, PLANET, RUINS, ALIEN)
├── AstronautStatus.kt        새 enum (IDLE, DEPLOYED, TRAINING)
├── Astronaut.kt              새 data class
├── Spaceship.kt              새 data class (grade, crewCapacity, speed, cargo, successRate)
├── ExpeditionCategory.kt     새 enum (MINERAL, PLANET, RUINS, ALIEN)
├── ExpeditionStatus.kt       새 enum (IN_PROGRESS, COMPLETED, FAILED)
├── Expedition.kt             새 data class
├── ResourceType.kt           새 enum (자원 종류)
├── Resource.kt               새 data class (type, amount)
└── ResearchLab.kt            새 data class (4개 분야 레벨)
```

### 데이터 레이어
```
data/local/
├── entity/
│   ├── AstronautEntity.kt
│   ├── SpaceshipEntity.kt
│   ├── ExpeditionEntity.kt
│   ├── ResourceEntity.kt
│   └── ResearchLabEntity.kt
├── dao/
│   ├── AstronautDao.kt
│   ├── SpaceshipDao.kt
│   ├── ExpeditionDao.kt
│   ├── ResourceDao.kt
│   └── ResearchLabDao.kt
└── mapper/
    ├── AstronautMapper.kt
    ├── SpaceshipMapper.kt
    ├── ExpeditionMapper.kt
    └── ResourceMapper.kt

data/repository/
├── AstronautRepositoryImpl.kt
├── SpaceshipRepositoryImpl.kt
├── ExpeditionRepositoryImpl.kt
├── ResourceRepositoryImpl.kt
└── ResearchLabRepositoryImpl.kt
```

### 도메인 레이어 (UseCase)
```
domain/usecase/
├── HireAstronautUseCase.kt
├── TrainAstronautUseCase.kt
├── GetAstronautsUseCase.kt
├── BuySpaceshipUseCase.kt
├── UpgradeSpaceshipUseCase.kt
├── GetSpaceshipsUseCase.kt
├── StartExpeditionUseCase.kt
├── CompleteExpeditionUseCase.kt
├── GetActiveExpeditionsUseCase.kt
├── GetResourcesUseCase.kt
├── UpgradePlanetUseCase.kt        (강화 확률 로직 포함)
├── UpgradeResearchFieldUseCase.kt
└── GetResearchLabUseCase.kt
```

### 프레젠테이션 레이어
```
presentation/
├── screen/
│   ├── explore/
│   │   └── ExploreScreen.kt       전면 개편 (탐사팀 구성 + 다중 원정 관리)
│   ├── planet/
│   │   ├── PlanetScreen.kt        수정 (강화 버튼)
│   │   └── PlanetDetailScreen.kt  수정 (강화 UI)
│   ├── hq/
│   │   ├── HQScreen.kt            신규 (본부 허브)
│   │   ├── AstronautScreen.kt     신규 (고용·훈련 관리)
│   │   ├── HangarScreen.kt        신규 (우주선 관리)
│   │   └── ResearchLabScreen.kt   신규 (연구소 4개 분야)
│   ├── expedition/
│   │   └── ExpeditionHistoryScreen.kt  신규 (탐사 기록)
│   └── asset/
│       └── AssetScreen.kt         수정 (자원 현황 실데이터)
└── viewmodel/
    ├── ExploreViewModel.kt        수정
    ├── PlanetViewModel.kt         수정
    ├── HQViewModel.kt             신규
    ├── AstronautViewModel.kt      신규
    ├── SpaceshipViewModel.kt      신규
    └── ResearchLabViewModel.kt    신규
```

---

## 구현 순서

### Phase 1: 구 시스템 제거 + 모델 수정
> 기존 코드의 불필요한 부분을 걷어내고 Planet 모델을 새 설계에 맞게 수정

1. MarketEvent 시스템 전체 삭제 (Entity, Dao, Repository, UseCase, Worker)
2. PlanetMaintenanceWorker 삭제
3. UpdatePlanetStatusUseCase, RepairPlanetUseCase 삭제
4. Planet 모델 수정 (`status`, `maintenanceCost` → `level`, `upgradeInvestment`)
5. PlanetEntity 수정 + DB MIGRATION_4_5 작성
6. PlanetMapper, PlanetDao 수정
7. SellPlanetUseCase 매도가 공식 변경
8. CollectProfitUseCase 유지비 로직 제거
9. GameConstants 강화 확률 테이블 추가

### Phase 2: 신규 데이터 레이어
> 우주인·우주선·탐사·자원·연구소 DB 구조 구축

1. `ResourceType` enum + `Resource` 모델 정의
2. `AstronautSpecialty`, `AstronautStatus`, `Astronaut` 모델
3. `Spaceship` 모델
4. `ExpeditionCategory`, `ExpeditionStatus`, `Expedition` 모델
5. `ResearchLab` 모델
6. Entity 5종 작성 (Astronaut, Spaceship, Expedition, Resource, ResearchLab)
7. Dao 5종 작성
8. DB MIGRATION_5_6 (신규 테이블 5개 추가)
9. Mapper 4종 작성
10. Repository 인터페이스 5종 + Impl 5종
11. DatabaseModule + WorkerModule 바인딩 추가

### Phase 3: 도메인 UseCase
> 새 시스템의 비즈니스 로직 구현

1. `HireAstronautUseCase` (코인 차감, 고용 한도 체크)
2. `TrainAstronautUseCase` (훈련 상태 전환, 코인+자원 차감)
3. `GetAstronautsUseCase`
4. `BuySpaceshipUseCase` (코인+자원 차감, 보유 수 한도 체크)
5. `UpgradeSpaceshipUseCase`
6. `GetSpaceshipsUseCase`
7. `StartExpeditionUseCase` (팀 구성 검증, 우주인 상태→DEPLOYED)
8. `CompleteExpeditionUseCase` (자원 지급, 행성 발견 판정, 상태 복구)
9. `GetActiveExpeditionsUseCase`
10. `GetResourcesUseCase`
11. `UpgradePlanetUseCase` (확률 계산, Safe/Danger Zone 분기)
12. `UpgradeResearchFieldUseCase` (분야별 레벨업, 효과 적용)
13. `GetResearchLabUseCase`

### Phase 4: 네비게이션 재편
> 화면 구조 변경

1. `NavRoutes` 신규 라우트 추가 (HQ, Astronaut, Hangar, ResearchLab, ExpeditionHistory)
2. `BottomNavItem` 4개로 재편 (탐사/행성/본부/자산)
3. `MainScreen` NavHost 재편

### Phase 5: 화면 구현
> 새 UI 구현 및 기존 화면 수정

1. `ExploreScreen` 전면 개편
    - 활성 원정 목록 + 새 원정 파견 버튼
    - 팀 구성 바텀시트 (카테고리·우주인·우주선 선택)
    - 원정 결과 다이얼로그
2. `PlanetDetailScreen` 강화 UI 추가
    - 현재 레벨, 성공률 표시
    - Safe/Danger Zone 시각화
    - 강화 버튼 + 필요 재료 표시
3. `PlanetScreen` 수정 (수리 제거, 레벨 뱃지 추가)
4. `HQScreen` 신규 (시설 목록 허브)
5. `AstronautScreen` 신규 (우주인 목록, 고용, 훈련)
6. `HangarScreen` 신규 (우주선 목록, 구매, 강화)
7. `ResearchLabScreen` 신규 (4개 분야 레벨업)
8. `AssetScreen` 수정 (자원 현황 실데이터 연결)
9. `ExpeditionHistoryScreen` 신규

### Phase 6: 마무리
> WorkManager·FCM 연동 및 폴리싱

1. `ExplorationCompleteWorker` 신규 탐사 시스템 연동
2. 우주인 훈련 완료 알림 (FCM)
3. 탐사 완료 알림 (FCM)
4. 전체 흐름 통합 테스트

---

## DB 마이그레이션 요약

| 버전 | 변경 내용 |
|------|-----------|
| v4 → v5 | planet_table: `status`, `maintenanceCost` 제거 / `level`, `upgradeInvestment` 추가 |
| v5 → v6 | 신규 테이블 추가: astronaut, spaceship, expedition, resource, research_lab |
