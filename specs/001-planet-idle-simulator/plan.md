# 구현 계획서: 행성 방치형 투자 시뮬레이션 게임

**브랜치**: `main` | **작성일**: 2026-05-13 | **명세서**: [spec.md](spec.md) | **리서치**: [research.md](research.md)

## 요약

우주 행성을 매입·유지·매도하는 방치형 투자 시뮬레이션 앱. 기존 Clean Architecture(Hilt + Room + Compose) 위에 **유지비 시스템**, **행성 상태 관리(3단계)**, **시세 변동 엔진**, **이벤트 로그**, **FCM 알림(Cloud Functions)**, **Firebase Storage 이미지(variantId 기반 URL)**를 추가 구현한다.

**디자인 방향**: 도트(픽셀 아트) 스타일 — 픽셀 폰트, 레트로 색감, 계단식 그림자, 격자 패턴 UI

핵심 투자 사이클: **매입 → 유지비 납부 + 자원 생산 → 시세 변동 → 매도(수수료 5%) → 재투자**

---

## 기술 컨텍스트

**언어/버전**: Kotlin (Android)

**주요 의존성**:
- 기존: Jetpack Compose, Hilt, Room v2, Kotlin Coroutines + Flow, Navigation Compose
- 신규: WorkManager, Firebase Messaging, Firebase Firestore, Firebase Storage, Coil

**저장소**: Room SQLite (로컬) + Firebase Firestore (알림 트리거용)

**이미지**: Firebase Storage (variantId 기반 URL 자동 생성, 픽셀 아트 WebP)

**알림**: FCM + Firebase Cloud Functions (Node.js)

**폰트**: 픽셀/비트맵 계열 폰트 (Press Start 2P 또는 Silkscreen, Google Fonts)

**테스트**: JUnit4 + Hilt Testing (미구성, 태스크 단계 추가)

**대상 플랫폼**: Android (모바일)

**성능 목표**: 탐험 결과 ≤ 3초 / 오프라인 수익 반영 ≤ 5초 / 행성 50개 계산 ≤ 2초

**제약**: 오프라인 누적 상한 24시간 / 이벤트 로그 최대 200건 / 인앱 결제 없음

---

## 헌법 검사 (Constitution Check)

| 게이트 | 상태 | 근거 |
|--------|------|------|
| 레이어 분리 (domain/data/presentation) | 통과 | 기존 구조 유지, 신규 기능 동일 패턴 |
| 단방향 의존성 | 통과 | Firebase/WorkManager는 data 레이어에서만 사용 |
| 단일 책임 UseCase | 통과 | 기능별 UseCase 분리 |
| 데이터 불변성 (Entity → Model 매핑) | 통과 | Mapper 패턴 유지 |
| 오프라인 우선 | 통과 | Room이 단일 진실 공급원, Firebase는 알림·이미지 보조 |

---

## 프로젝트 구조

### 문서 (이 기능)

```
specs/001-planet-idle-simulator/
├── plan.md              ← 이 파일
├── research.md          ← Phase 0 산출물
├── data-model.md        ← Phase 1 산출물
├── contracts/
│   ├── notification-contract.md
│   └── workmanager-contract.md
└── tasks.md             ← /speckit-tasks 산출물 (미생성)
```

### 소스 코드 (기존 + 신규)

```
app/src/main/java/com/doge/simulator/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── PlanetDao.kt              기존 — 상태 쿼리 추가
│   │   │   ├── UserDao.kt                기존
│   │   │   ├── EventLogDao.kt            신규
│   │   │   └── MarketEventDao.kt         신규
│   │   ├── entity/
│   │   │   ├── PlanetEntity.kt           기존 — maintenanceCost, status 필드 추가
│   │   │   ├── UserEntity.kt             기존
│   │   │   ├── EventLogEntity.kt         신규
│   │   │   └── MarketEventEntity.kt      신규
│   │   ├── mapper/
│   │   │   ├── PlanetMapper.kt           기존 — 신규 필드 반영
│   │   │   ├── EventLogMapper.kt         신규
│   │   │   └── MarketEventMapper.kt      신규
│   │   └── PlanetDatabase.kt             기존 — v3 마이그레이션
│   ├── repository/
│   │   ├── PlanetRepositoryImpl.kt       기존 — updateStatus, repair 추가
│   │   ├── UserRepositoryImpl.kt         기존
│   │   ├── EventLogRepositoryImpl.kt     신규
│   │   └── MarketEventRepositoryImpl.kt  신규
│   └── worker/
│       ├── PlanetMaintenanceWorker.kt    신규 (15분 주기)
│       └── MarketEventWorker.kt          신규 (30분 주기)
├── di/
│   ├── AppModule.kt                      기존
│   ├── DatabaseModule.kt                 기존 — 신규 바인딩 추가
│   └── WorkerModule.kt                   신규
├── domain/
│   ├── model/
│   │   ├── Planet.kt                     기존 — maintenanceCost, status 추가
│   │   ├── PlanetStatus.kt               신규 (NORMAL, MAINTENANCE_REQUIRED, LOSS)
│   │   ├── PlanetType.kt                 기존
│   │   ├── PlanetMetaData.kt             기존 — baseMaintenanceCost 추가
│   │   ├── PlanetMetaDataTable.kt        기존 — URL 패턴 수정, maintenanceCost 추가
│   │   ├── PlanetVariant.kt              기존
│   │   ├── RarityTier.kt                 기존
│   │   ├── EventLog.kt                   신규
│   │   ├── EventLogType.kt               신규
│   │   └── MarketEvent.kt                신규
│   ├── repository/
│   │   ├── PlanetRepository.kt           기존 — updateStatus, repairPlanet 추가
│   │   ├── UserRepository.kt             기존
│   │   ├── EventLogRepository.kt         신규
│   │   └── MarketEventRepository.kt      신규
│   └── usecase/
│       ├── BuyPlanetUseCase.kt           기존
│       ├── SellPlanetUseCase.kt          기존 — 수수료 5% 차감, 매도 로그 기록
│       ├── CollectProfitUseCase.kt       기존 — 유지비 차감 통합
│       ├── GeneratePlanetsUseCase.kt     기존
│       ├── GetOwnedPlanetsUseCase.kt     기존
│       ├── RepairPlanetUseCase.kt        신규
│       ├── UpdatePlanetStatusUseCase.kt  신규
│       ├── ApplyMarketEventUseCase.kt    신규
│       ├── GetEventLogsUseCase.kt        신규
│       └── GetMarketEventsUseCase.kt     신규
├── firebase/
│   └── NotificationFirestoreService.kt  신규 — Firestore 알림 문서 기록
└── presentation/
    ├── screen/
    │   ├── explore/
    │   │   ├── ExploreScreen.kt          기존 — 이미지, 예상 순수익 표시 추가
    │   │   └── ExploreUiState.kt         기존
    │   ├── planet/
    │   │   ├── PlanetScreen.kt           기존 — 상태 뱃지, 수리 버튼, 이미지 추가
    │   │   └── PlanetDetailScreen.kt     신규
    │   ├── asset/
    │   │   └── AssetScreen.kt            기존 placeholder → 구현
    │   └── feed/
    │       └── FeedScreen.kt             기존 placeholder → 이벤트 로그로 구현
    ├── theme/
    │   ├── Color.kt                      기존 — 픽셀 아트 팔레트로 확장
    │   ├── Type.kt                       기존 — 픽셀 폰트(Press Start 2P) 적용
    │   └── Theme.kt                      기존
    └── viewmodel/
        ├── ExploreViewModel.kt           기존
        ├── PlanetViewModel.kt            기존 — 상태/수리 추가
        ├── AssetViewModel.kt             신규
        └── FeedViewModel.kt              신규
```

---

## 구현 단계

### Phase 1: 데이터 모델 확장 (기반)

1. `PlanetStatus` enum 추가 (NORMAL, MAINTENANCE_REQUIRED, LOSS)
2. `Planet` 모델에 `maintenanceCost: Int`, `status: PlanetStatus` 필드 추가
3. `PlanetMetaData`에 `baseMaintenanceCost: Int` 추가
4. `PlanetMetaDataTable` — `generateVariants` Firebase Storage URL 패턴으로 수정, 각 타입에 `maintenanceCost` 값 설정
5. `PlanetEntity`에 동일 필드 추가, `PlanetMapper` 업데이트
6. Room DB v3 마이그레이션 작성 (planet_table 컬럼 추가)
7. `EventLog`, `EventLogType` 도메인 모델 추가
8. `MarketEvent` 도메인 모델 추가
9. `EventLogEntity`, `MarketEventEntity` Room 엔티티 추가
10. `EventLogDao`, `MarketEventDao` 추가, DB v3 마이그레이션에 신규 테이블 포함

### Phase 2: Repository 및 UseCase 확장

11. `PlanetRepository` 인터페이스에 `updateStatus`, `repairPlanet` 추가 및 `PlanetRepositoryImpl` 구현
12. `EventLogRepository` + Impl, `MarketEventRepository` + Impl
13. `DatabaseModule` 신규 바인딩 추가
14. `SellPlanetUseCase` — 수수료 5% 차감, 매도 이벤트 로그 기록
15. `CollectProfitUseCase` — 유지비 차감 통합, 코인 부족 시 상태 전환 트리거
16. `RepairPlanetUseCase` — 수리 비용(buyPrice × 20%) 차감, NORMAL 복구, 로그 기록
17. `UpdatePlanetStatusUseCase` — 상태 전환 규칙 적용
18. `ApplyMarketEventUseCase` — 이벤트 효과 시세 반영, 로그 기록
19. `GetEventLogsUseCase`, `GetMarketEventsUseCase`

### Phase 3: Firebase 연동

20. `google-services.json` 추가 및 Firebase Gradle 플러그인 설정
21. `firebase-messaging-ktx`, `firebase-firestore-ktx`, `firebase-storage-ktx`, `coil-compose` 의존성 추가
22. `NotificationFirestoreService` — FCM 토큰 조회, Firestore `notifications/{token}/pending` 문서 기록
23. Firebase Cloud Functions 작성 (Node.js) — `onDocumentCreated` 트리거 → FCM 발송
24. FCM 딥링크 처리 (`MainActivity` Intent 처리)
25. `PlanetVariant.imageUrl` Coil `AsyncImage`로 교체 (픽셀 아트 이미지 표시)

### Phase 4: 백그라운드 워커

26. `PlanetMaintenanceWorker` (15분 주기) — `CollectProfitUseCase` 호출, 상태 체크, 알림 트리거
27. `MarketEventWorker` (30분 주기) — 랜덤 이벤트 생성, `ApplyMarketEventUseCase` 호출, 급등락 알림 트리거
28. `WorkerModule` DI 바인딩, `MyApplication`에서 WorkManager 초기화

### Phase 5: 디자인 시스템 (픽셀 아트)

29. `Type.kt` — Press Start 2P 또는 Silkscreen 픽셀 폰트 적용 (Google Fonts)
30. `Color.kt` — 레트로 픽셀 아트 팔레트 확장 (제한된 색상 수, 선명한 레트로 계열)
31. 공통 픽셀 스타일 컴포넌트 정의 — 계단식 그림자(`offset shadow`), 픽셀 테두리 카드, 격자 배경 패턴

### Phase 6: 화면 구현

32. `ExploreScreen` — 픽셀 아트 행성 이미지(Coil), 예상 순수익(생산량 - 유지비) 표시
33. `PlanetScreen` — 상태 뱃지(NORMAL: 초록 / MAINTENANCE: 노랑 / LOSS: 빨강), 수리 버튼, 이미지 썸네일
34. `PlanetDetailScreen` — 신규: 대형 행성 이미지, 스탯, 실시간 누적 수익, 현재 시세, 수리/매도 버튼
35. `PlanetViewModel` — 수리 기능, 상태 구독 추가
36. `AssetScreen` — 보유 코인, 행성 수, 총 시세, 누적 순수익 (픽셀 스타일 대시보드)
37. `AssetViewModel`
38. `FeedScreen` — 이벤트 로그 목록 (시간 역순, 이벤트 유형별 아이콘·색상, 페이지 단위)
39. `FeedViewModel`
40. `SellConfirmDialog` 개선 — 현재 시세 / 수수료(5%) / 수령 예정 금액 3단 표시, 손실 경고

---

## 결정 사항 요약

| 항목 | 결정 | 근거 |
|------|------|------|
| 디자인 스타일 | 픽셀 아트(도트게임) | 사용자 지정 방향 |
| 행성 이미지 | Firebase Storage + variantId URL 자동 생성 | 161개 variant 코드 관리, APK 경량화 |
| 알림 | FCM + Cloud Functions | 서버 키 노출 없이 신뢰성 있는 푸시 |
| 유지비 처리 | CollectProfitUseCase 통합 | 원자성 보장 |
| 시세 변동 | WorkManager 30분 주기 | 백그라운드 제약 준수 |
| 수수료율 | 고정 5% | 상수 분리, 밸런싱 후 조정 가능 |
| 수리 비용 | buyPrice × 20% | 상수 분리, 밸런싱 후 조정 가능 |
| 이벤트 로그 | Room 최대 200건 | 기존 DB 활용 |
| FeedScreen | 이벤트 로그 화면으로 전환 | placeholder 재활용 |