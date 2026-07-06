# WorkManager 계약서

**작성일**: 2026-05-13

---

## PlanetMaintenanceWorker

**역할**: 유지비 차감, 행성 상태 체크, 알림 트리거

**주기**: 15분 (WorkManager 최소 단위)

**실행 조건**: 네트워크 불필요, 배터리 비절약 모드 아님

**처리 순서**:
1. 보유 행성 전체 조회
2. `CollectProfitUseCase` 호출 (생산 수익 - 유지비 계산)
3. 코인 부족으로 유지비 미납 행성 → `UpdatePlanetStatusUseCase` (MAINTENANCE_REQUIRED)
4. 상태 변화 발생 시 Firestore 알림 문서 기록
5. 보유 코인 < 전체 최소 유지비 합산 → LOW_COINS 알림

**출력**: SUCCESS (항상. 실패해도 다음 주기에 재시도)

---

## MarketEventWorker

**역할**: 시장 이벤트 생성, 시세 변동 적용, 급등락 알림

**주기**: 30분

**처리 순서**:
1. 행성 유형별 랜덤 이벤트 결정 (eventRate 가중치 기반)
2. 이벤트 발생 시 `ApplyMarketEventUseCase` 호출
3. 변동폭 ≥ 20% 시 Firestore 알림 문서 기록
4. LOSS 상태 자동 복구 체크 (currentValue ≥ buyPrice)
5. 정기 시세 변동 (이벤트 없는 행성도 risk 기반 소폭 변동)

**출력**: SUCCESS

---

## WorkManager 초기화 (MyApplication)

```kotlin
val maintenanceRequest = PeriodicWorkRequestBuilder<PlanetMaintenanceWorker>(
    15, TimeUnit.MINUTES
).build()

val marketRequest = PeriodicWorkRequestBuilder<MarketEventWorker>(
    30, TimeUnit.MINUTES
).build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "planet_maintenance",
    ExistingPeriodicWorkPolicy.KEEP,
    maintenanceRequest
)

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "market_events",
    ExistingPeriodicWorkPolicy.KEEP,
    marketRequest
)
```