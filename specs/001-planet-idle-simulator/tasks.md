# Tasks: 행성 방치형 투자 시뮬레이션 게임

**Input**: `specs/001-planet-idle-simulator/` 설계 문서

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**경로 기준**: `app/src/main/java/com/doge/simulator/` = `[src]`

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 다른 파일, 의존성 없음 → 병렬 실행 가능
- **[USx]**: 해당 유저 스토리 태스크 (spec.md 기준)
- 테스트는 명세서에 명시 없음 → 미포함

---

## Phase 1: Setup (공유 인프라)

**목적**: Firebase, WorkManager, Coil, 폰트 의존성 추가

- [X] T001 `app/build.gradle.kts`에 Firebase BOM + firebase-messaging-ktx, firebase-firestore-ktx, firebase-storage-ktx 의존성 추가
- [X] T002 [P] `app/build.gradle.kts`에 WorkManager(work-runtime-ktx) 의존성 추가
- [X] T003 [P] `app/build.gradle.kts`에 coil-compose 의존성 추가
- [ ] T004 [P] PF Stardust ExtraBold 폰트 파일(.ttf/.otf)을 `app/src/main/res/font/pf_stardust_extrabold.ttf`에 추가 ← 수동 추가 필요
- [ ] T005 Firebase Console에서 FCM·Firestore·Storage 활성화 후 `google-services.json`을 `app/`에 추가, 프로젝트·앱 레벨 `build.gradle.kts`에 google-services 플러그인 적용 ← 수동 설정 필요
- [X] T006 [P] `app/build.gradle.kts`에 `FIREBASE_STORAGE_BASE_URL` BuildConfig 필드 추가, `local.properties`에 실제 Firebase Storage base URL 기록, `.gitignore`에 `local.properties` 포함 확인

---

## Phase 2: Foundational (모든 유저 스토리 전제 조건)

**목적**: 신규 데이터 모델, Room v3 마이그레이션, 공통 상수, 디자인 토큰 정의

**⚠️ 이 Phase 완료 전에는 어떤 유저 스토리도 시작 불가**

- [X] T007 `[src]/domain/model/GameConstants.kt` 생성 — SELL_FEE_RATE=0.05, REPAIR_COST_RATE=0.20, MAX_OFFLINE_MINUTES=1440, MAX_EVENT_LOG_COUNT=200, MAINTENANCE_PRODUCTION_RATE=0.5f, MIN_VALUE_RATE=0.3f, MAX_VALUE_RATE=3.0f, ALERT_CHANGE_THRESHOLD=0.20f 상수 정의
- [X] T008 [P] `[src]/domain/model/PlanetStatus.kt` 생성 — NORMAL, MAINTENANCE_REQUIRED, LOSS enum 정의
- [X] T009 [P] `[src]/domain/model/EventLogType.kt` 생성 — PLANET_BOUGHT, PLANET_SOLD, MARKET_POSITIVE, MARKET_NEGATIVE, STATUS_CHANGED, MAINTENANCE_COLLECTED, PLANET_REPAIRED enum 정의
- [X] T010 `[src]/domain/model/Planet.kt`에 `maintenanceCost: Int = 0`, `status: PlanetStatus = PlanetStatus.NORMAL` 필드 추가
- [X] T011 `[src]/domain/model/PlanetMetaData.kt`에 `baseMaintenanceCostMin: Int`, `baseMaintenanceCostMax: Int` 필드 추가
- [X] T012 [P] `[src]/domain/model/EventLog.kt` 생성 — id, type(EventLogType), planetId?, planetType?, planetDisplayName?, description, valueBefore?, valueAfter?, coinDelta?, occurredAt 필드
- [X] T013 [P] `[src]/domain/model/MarketEvent.kt` 생성 — id, targetPlanetType, isPositive, changeRate(Float), title, description, occurredAt 필드
- [X] T014 `[src]/data/local/entity/PlanetEntity.kt`에 `maintenanceCost: Int = 0`, `status: String = "NORMAL"` 필드 추가
- [X] T015 [P] `[src]/data/local/entity/EventLogEntity.kt` 생성 — tableName="event_log_table", autoGenerate PK, EventLog 모든 필드 대응
- [X] T016 [P] `[src]/data/local/entity/MarketEventEntity.kt` 생성 — tableName="market_event_table", autoGenerate PK, MarketEvent 모든 필드 대응
- [X] T017 `[src]/data/local/PlanetDatabase.kt` v3 업데이트 — EventLogEntity·MarketEventEntity 등록, MIGRATION_2_3 작성, addMigrations에 MIGRATION_2_3 추가
- [X] T018 [P] `[src]/data/local/dao/EventLogDao.kt` 생성 — insert, getLogs(limit, offset): Flow, count(), deleteOldest(count) 메서드
- [X] T019 [P] `[src]/data/local/dao/MarketEventDao.kt` 생성 — insert, getRecentEvents(): Flow<List<MarketEventEntity>> 메서드
- [X] T020 `[src]/data/local/mapper/PlanetMapper.kt` 업데이트 — maintenanceCost, status(PlanetStatus.valueOf) 필드 매핑 추가
- [X] T021 [P] `[src]/data/local/mapper/EventLogMapper.kt` 생성 — EventLogEntity ↔ EventLog 양방향 변환
- [X] T022 [P] `[src]/data/local/mapper/MarketEventMapper.kt` 생성 — MarketEventEntity ↔ MarketEvent 양방향 변환
- [X] T023 `[src]/ui/theme/Type.kt` 업데이트 — PF Stardust ExtraBold 폰트패밀리 정의, 제목·본문·수치·뱃지 TextStyle 정의
- [X] T024 `[src]/ui/theme/Color.kt` 업데이트 — 기준색 `#24448F` 기반의 어두운 우주 팔레트: SpaceDark, SpaceNavy, SpaceMid, SpaceBlue, SpaceAccent, GoldAccent, StatusGreen, StatusYellow, StatusRed, TextPrimary, TextSecondary

**Checkpoint**: 데이터 모델·Room v3 마이그레이션·디자인 토큰 완료

---

## Phase 3: User Story 1 - 행성 탐험 및 매입 (Priority: P1) 🎯 MVP

**Goal**: 탐험 → 픽셀 아트 행성 이미지와 예상 순수익 확인 → 구매

**Independent Test**: 탐험 → 행성 카드(이미지·예상 순수익 표시) → 구매 → 보유 목록 추가 확인

- [X] T025 [US1] `[src]/domain/model/PlanetMetaDataTable.kt`의 `generateVariants()` 수정 — variantId 기반 Firebase Storage URL 자동 구성, 12개 모든 행성 타입에 baseMaintenanceCostMin·Max 값 설정
- [X] T026 [US1] `[src]/domain/usecase/GeneratePlanetsUseCase.kt` 수정 — maintenanceCost 랜덤 생성, status=NORMAL 초기화, 랜덤 variant 선택
- [X] T027 [US1] `[src]/presentation/screen/explore/ExploreUiState.kt`에 `exploredPlanetImageUrl: String? = null` 필드 추가
- [X] T028 [US1] `[src]/presentation/screen/explore/ExploreScreen.kt`의 `ExploredPlanetCard` 수정 — Coil AsyncImage, 예상 순수익 표시, PF Stardust ExtraBold·우주 팔레트 적용

**Checkpoint**: 탐험 → 이미지·순수익 확인 → 구매 흐름 완전 동작

---

## Phase 4: User Story 2 - 방치 수익 수집 (Priority: P1)

**Goal**: 앱 재실행 시 오프라인 순수익(생산량 - 유지비) 자동 반영

**Independent Test**: 행성 보유 → 시간 경과 → 앱 재실행 → 코인 잔액에 순수익 반영 확인

- [X] T029 [US2] `[src]/domain/usecase/CollectProfitUseCase.kt` 수정 — MAX_OFFLINE_MINUTES 처리, MAINTENANCE_REQUIRED 상태 생산량 50% 적용, 순수익=(effectiveProduction-maintenanceCost)×elapsedMinutes(음수→0), EventLog MAINTENANCE_COLLECTED 기록
- [X] T030 [US2] `[src]/domain/repository/PlanetRepository.kt`에 `updateStatus`, `repairPlanet` 메서드 추가
- [X] T031 [US2] `[src]/data/repository/PlanetRepositoryImpl.kt`에 `updateStatus`, `repairPlanet` 구현

**Checkpoint**: 오프라인 경과 후 코인에 순수익(생산-유지비) 정확히 반영 확인

---

## Phase 5: User Story 3 - 행성 상태 관리 (Priority: P1)

**Goal**: 행성 상태(정상/유지보수 필요/손실) 확인 및 수리

**Independent Test**: 유지비 미납 → MAINTENANCE_REQUIRED·생산량 50% 감소 확인 → 수리 비용 납부 → NORMAL 복구 확인

- [X] T032 [US3] `[src]/domain/repository/EventLogRepository.kt` 인터페이스 생성 — insertLog, getLogs(limit, offset), getLogCount 메서드
- [X] T033 [US3] `[src]/data/repository/EventLogRepositoryImpl.kt` 구현 — insert 후 count > MAX_EVENT_LOG_COUNT 시 deleteOldest 호출
- [X] T034 [US3] `[src]/domain/usecase/UpdatePlanetStatusUseCase.kt` 생성 — 상태 전환 시 EventLog STATUS_CHANGED 기록
- [X] T035 [US3] `[src]/domain/usecase/RepairPlanetUseCase.kt` 생성 — 수리 비용=buyPrice×REPAIR_COST_RATE, 코인 차감 성공 시 NORMAL 복구, EventLog PLANET_REPAIRED 기록
- [X] T036 [US3] `[src]/di/DatabaseModule.kt` 업데이트 — EventLogDao·MarketEventDao·EventLogRepository·MarketEventRepository 바인딩, MIGRATION_2_3 등록
- [X] T037 [US3] `[src]/presentation/screen/planet/PlanetScreen.kt`의 `PlanetCard` 수정 — 상태 뱃지, 수리 버튼, PF Stardust ExtraBold·우주 팔레트 적용
- [X] T038 [US3] `[src]/presentation/viewmodel/PlanetViewModel.kt`에 RepairPlanetUseCase 주입, repairPlanet 함수, 수리 결과 메시지 StateFlow
- [X] T039 [US3] `[src]/presentation/screen/planet/PlanetDetailScreen.kt` 신규 생성 — 대형 이미지, 스탯, 시세·손익률, 상태 뱃지, 수리·매도 버튼
- [X] T040 [US3] `[src]/presentation/navigation/NavRoutes.kt`에 PlanetDetail 라우트 추가, PlanetCard 탭 시 PlanetDetailScreen 네비게이션 연결

**Checkpoint**: 상태 전환·수리·상세 화면 독립 동작 확인

---

## Phase 6: User Story 5 - 행성 매도 및 수수료 (Priority: P2)

**Goal**: 매도 확인 화면에서 수수료·수령액 사전 확인 후 매도

**Independent Test**: 행성 선택 → 다이얼로그에서 시세/수수료/수령액 표시 → 매도 → 코인 증가 확인

- [X] T041 [US5] `[src]/domain/usecase/SellPlanetUseCase.kt` 수정 — 수령액=currentValue×(1-SELL_FEE_RATE), addCoins 호출, EventLog PLANET_SOLD 기록
- [X] T042 [US5] `[src]/presentation/screen/planet/PlanetScreen.kt`의 `SellConfirmDialog` 수정 — 시세/수수료/수령액 3행, LOSS 상태 경고 문구 추가

**Checkpoint**: 수수료 차감 후 정확한 코인 수령 확인

---

## Phase 7: User Story 4 - 시세 변동 및 이벤트 (Priority: P2)

**Goal**: 주기적 시세 변동 + 행성 유형별 이벤트 급등락

**Independent Test**: MarketEventWorker 1회 강제 실행 → 행성 시세 변동 + EventLog 기록 확인

- [X] T043 [US4] `[src]/domain/repository/MarketEventRepository.kt` 인터페이스 생성
- [X] T044 [US4] `[src]/data/repository/MarketEventRepositoryImpl.kt` 구현
- [X] T045 [US4] `[src]/domain/usecase/ApplyMarketEventUseCase.kt` 생성 — changeRate 적용, MIN/MAX_VALUE_RATE 클램핑, LOSS 전환 체크, EventLog 기록
- [X] T046 [US4] `[src]/domain/usecase/GetMarketEventsUseCase.kt` 생성
- [X] T047 [US4] `[src]/data/worker/MarketEventWorker.kt` 생성 — eventRate 기반 이벤트 발생, risk 기반 소폭 변동, LOSS 자동 복구 체크
- [X] T048 [US4] `[src]/di/WorkerModule.kt` 생성
- [X] T049 [US4] `[src]/MyApplication.kt`에 MarketEventWorker 30분 주기 WorkManager 등록

**Checkpoint**: 시세 변동·이벤트 적용·LOSS 자동 복구 독립 동작 확인

---

## Phase 8: User Story 6 - 이벤트 로그 조회 (Priority: P2)

**Goal**: FeedScreen에서 시장 이벤트·거래 이력 시간 역순 조회

**Independent Test**: 매입·매도·이벤트 발생 후 FeedScreen에서 해당 항목 표시 확인

- [X] T050 [US6] `[src]/domain/usecase/GetEventLogsUseCase.kt` 생성 — limit/offset 페이지네이션 지원
- [X] T051 [US6] `[src]/presentation/viewmodel/FeedViewModel.kt` 생성 — GetEventLogsUseCase 주입, 로그 목록 StateFlow, loadNextPage()
- [X] T052 [US6] `[src]/presentation/screen/feed/FeedScreen.kt` 구현 — 이벤트 로그 LazyColumn, 유형별 아이콘·색상, 페이지네이션

**Checkpoint**: 로그 조회·페이지네이션 독립 동작 확인

---

## Phase 9: User Story 7 - 중요 이벤트 알림 수신 (Priority: P3)

**Goal**: 앱 외부에서도 급등락·상태 악화·코인 부족 알림 수신

**Independent Test**: 이벤트 발생 → Firestore 문서 생성 → Cloud Function → 기기 알림 → 탭 → 해당 화면 진입 확인

- [X] T053 [US7] `[src]/firebase/NotificationFirestoreService.kt` 생성 — FCM 토큰 조회, Firestore notifications/{token}/pending/{autoId} 기록
- [X] T054 [US7] `[src]/data/worker/PlanetMaintenanceWorker.kt` 생성 — CollectProfitUseCase 호출, 상태 전환 알림, LOW_COINS 알림
- [X] T055 [US7] `[src]/MyApplication.kt`에 PlanetMaintenanceWorker 15분 주기 WorkManager 등록
- [X] T056 [US7] `[src]/data/worker/MarketEventWorker.kt`에 ALERT_CHANGE_THRESHOLD 이상 이벤트 시 NotificationFirestoreService 호출
- [X] T057 [US7] `functions/index.js` 작성 — onDocumentCreated 트리거, FCM 전송, 문서 삭제
- [X] T058 [US7] `[src]/MyApplication.kt`에 알림 채널 등록 — channel_market, channel_maintenance, channel_coins
- [X] T059 [US7] `[src]/MainActivity.kt`에 FCM 딥링크 처리 — doge://planet/{id}, doge://feed, doge://asset 네비게이션

**Checkpoint**: 이벤트 → 기기 알림 → 탭 → 앱 화면 직접 진입 확인

---

## Phase 10: User Story 8 - 자산 현황 조회 (Priority: P3)

**Goal**: 보유 코인·행성 수·총 시세·누적 순수익 한눈에 확인

**Independent Test**: AssetScreen 진입 → 코인·행성 수·총 시세 올바르게 표시 확인

- [X] T060 [US8] `[src]/presentation/viewmodel/AssetViewModel.kt` 생성 — 코인·행성 수·총 시세·총 수익 StateFlow
- [X] T061 [US8] `[src]/presentation/screen/asset/AssetScreen.kt` 구현 — 4개 수치 픽셀 카드, SpaceDark 배경

**Checkpoint**: 자산 화면 수치 정확도 확인

---

## Phase 11: Polish — 픽셀 아트 디자인 시스템 전면 적용

**목적**: 전체 화면에 PF Stardust ExtraBold + 우주 팔레트 통일 적용

- [X] T062 [P] `[src]/presentation/component/PixelComponents.kt` 생성 — PixelBorderCard, PixelButton, PixelStatChip 공통 Composable
- [X] T063 [P] `[src]/ui/theme/Theme.kt` — 우주 팔레트 colorScheme, PF Stardust ExtraBold typography 적용 완료
- [X] T064 ExploreScreen·PlanetScreen·PlanetDetailScreen에 우주 테마 적용 완료 (모든 화면 SpaceNavy 카드, GoldAccent 강조)
- [X] T065 AssetScreen·FeedScreen에 우주 테마 적용 완료

---

## Dependencies & Execution Order

### Phase 의존성

- **Phase 1 (Setup)**: 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 후 → 전체 블로킹
- **Phase 3~10 (User Stories)**: Phase 2 완료 후 순서대로 진행
- **Phase 11 (Polish)**: 원하는 스토리 완료 후 언제든 적용 가능

### 유저 스토리 의존성

| 유저 스토리 | 선행 조건 |
|------------|----------|
| US1 (탐험·매입) | Foundational |
| US2 (방치 수익) | US1 |
| US3 (상태 관리) | US2 |
| US5 (매도 수수료) | Foundational (US1과 병렬 가능) |
| US4 (시세 변동) | US3 |
| US6 (이벤트 로그) | US3, US4, US5 |
| US7 (FCM 알림) | US3, US4 |
| US8 (자산 현황) | US1 |

---

## 잔여 수동 작업

- **T004**: PF Stardust ExtraBold 폰트 파일을 `app/src/main/res/font/pf_stardust_extrabold.ttf`에 직접 추가 필요
- **T005**: Firebase Console에서 프로젝트 설정 후 `google-services.json` 추가 필요, `local.properties`에 `FIREBASE_STORAGE_BASE_URL` 값 설정 필요
- **Cloud Functions**: `functions/` 디렉토리에서 `npm install && firebase deploy --only functions` 실행 필요
