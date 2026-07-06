# 리서치 보고서: 행성 방치형 투자 시뮬레이션 게임

**작성일**: 2026-05-13
**관련 명세서**: [spec.md](spec.md)

---

## 결정 1: 유지비와 수익 수집 통합 처리

**결정**: 유지비 차감을 `CollectProfitUseCase`에 통합한다.

**근거**: 수익과 유지비는 동일한 경과 시간 기반으로 계산되므로 한 트랜잭션에서 처리해야 원자성 보장.

**수식**: `순수익 = (production × 경과분) - (maintenanceCost × 경과분)`

**대안 검토**:
- 별도 UseCase로 분리 → 두 번의 DB 쓰기, 불일치 위험, 기각
- WorkManager에서 직접 처리 → 도메인 로직 워커 유출, Clean Architecture 위반, 기각

---

## 결정 2: 백그라운드 처리 — WorkManager

**결정**: `PlanetMaintenanceWorker`(15분 주기), `MarketEventWorker`(30분 주기)로 분리.

**대안 검토**:
- `Handler.postDelayed` → 앱 종료 시 중단, 방치형 부적합, 기각
- `ForegroundService` → 배터리 소모 과다, 기각

---

## 결정 3: 알림 — FCM + Firebase Cloud Functions

**결정**: WorkManager가 중요 이벤트 감지 시 Firestore에 알림 문서 기록 → Cloud Function `onDocumentCreated` 트리거 → FCM으로 기기에 알림 전송.

**흐름**:
```
WorkManager 이벤트 감지
  → Firestore notifications/{fcmToken}/pending 문서 쓰기
    → Cloud Function onDocumentCreated 트리거
      → FCM HTTP v1 API 호출
        → 기기 알림 수신
```

**알림 트리거 조건**:
- 행성 상태 MAINTENANCE_REQUIRED 전환 시
- 급등락 이벤트 (시세 변동폭 ≥ 20%) 발생 시
- 보유 코인이 최소 유지비 미만으로 하락 시

**딥링크**: PendingIntent로 PlanetDetailScreen 또는 FeedScreen 직접 진입

**Firebase 구성 필요 항목**:
- `google-services.json` → `app/` 디렉토리
- Firebase Console: FCM, Firestore, Storage 활성화
- Cloud Functions (Node.js) 배포

**추가 Gradle 의존성**:
```gradle
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
```

**대안 검토**:
- 로컬 알림만 사용 → FCM 신뢰성·딥링크 없음, 기각
- 앱에서 FCM REST API 직접 호출 → 서버 키 APK 노출, 보안 위험, 기각

---

## 결정 4: 행성 이미지 — Firebase Storage + Coil (variantId 기반 URL 자동 생성)

**결정**: Firebase Storage에 이미지 사전 업로드. **파일명 = variantId** 규칙으로 URL을 코드에서 자동 생성. `PlanetVariant.imageUrl`은 이 URL로 채워지며, 앱은 Coil로 비동기 로드.

**variantId 네이밍**: 기존 `generateVariants` 함수가 이미 `TERRAN_WET-01`, `TERRAN_WET-02` 형식으로 생성하고 있어 추가 변경 불필요.

**Firebase Storage 경로 규칙**:
```
gs://{PROJECT_ID}.appspot.com/
└── planets/
    ├── TERRAN_WET-01.webp
    ├── TERRAN_WET-02.webp
    ├── TERRAN_DRY-01.webp
    ├── ...
    ├── GALAXY-01.webp
    └── STAR-01.webp
```

**URL 자동 생성 로직** (`generateVariants` 수정):
```kotlin
private fun generateVariants(prefix: String, count: Int): List<PlanetVariant> {
    return (1..count).map { index ->
        val variantId = "$prefix-${index.toString().padStart(2, '0')}"
        PlanetVariant(
            variantId = variantId,
            imageUrl = "${BuildConfig.FIREBASE_STORAGE_BASE_URL}/planets%2F$variantId.webp?alt=media"
        )
    }
}
```
→ `FIREBASE_STORAGE_BASE_URL`은 `local.properties` 또는 `BuildConfig`로 주입, APK에 URL 패턴만 포함.

**이미지 규격 권장**:
- 포맷: WebP (PNG 대비 25~35% 용량 절감)
- Explore 카드: 512×512px
- Planet 목록 썸네일: 256×256px

**캐싱**: Coil 기본 디스크 캐시 — 최초 1회 다운로드 후 로컬 캐시 제공

**추가 Gradle 의존성**:
```gradle
implementation("io.coil-kt:coil-compose:2.x")
```

**대안 검토**:
- 이미지 로컬 번들 (res/drawable) → APK 크기 과다, 이미지 변경에 앱 업데이트 필요, 기각
- Firestore에 URL 저장 → variantId로 URL 패턴이 이미 결정되므로 오버엔지니어링, 기각
- URL 하드코딩 (각 variant별로) → variant 수가 총 161개로 유지보수 불가, 기각

---

## 결정 5: 행성 상태 전환 규칙

**결정**: `UpdatePlanetStatusUseCase`에서 집중 처리.

| 조건 | 전환 상태 |
|------|-----------|
| 코인 부족으로 유지비 미납 누적 OR 악재 이벤트 누적 피해 | MAINTENANCE_REQUIRED |
| 현재 시세 < 매입가 | LOSS |
| 수리 비용 납부 완료 | NORMAL |
| 시세 회복 (현재 시세 ≥ 매입가) | LOSS → NORMAL 자동 복구 |

**생산량 패널티**:
- NORMAL: 100%
- MAINTENANCE_REQUIRED: 50%
- LOSS: 변화 없음 (시세만 영향)

---

## 결정 6: 시세 변동 모델

**정기 변동** (MarketEventWorker 30분 주기):
```
변동폭 = currentValue × (risk / 100) × Random(-1.0, 1.0) × 0.1
새 시세 = currentValue + 변동폭
하한 = buyPrice × 0.3  // 최대 70% 하락
상한 = buyPrice × 3.0  // 최대 200% 상승
```

**이벤트 변동** (eventRate 기반 랜덤):
- 호재: 시세 +10% ~ +50%
- 악재: 시세 -10% ~ -40%

---

## 결정 7: 매도 수수료율

**결정**: 고정 5% (`SELL_FEE_RATE = 0.05` 상수 분리)

**수식**: `수령액 = currentValue × 0.95`

---

## 결정 8: 수리 비용

**결정**: `buyPrice × 0.20` (`REPAIR_COST_RATE = 0.20` 상수 분리)

---

## 결정 9: 이벤트 로그 저장

**결정**: Room `event_log_table`, 최대 200건. 삽입 시 초과분 자동 삭제.

**EventLogType**: PLANET_BOUGHT, PLANET_SOLD, MARKET_POSITIVE, MARKET_NEGATIVE, STATUS_CHANGED, MAINTENANCE_COLLECTED, PLANET_REPAIRED

---

## 결정 10: 오프라인 수익 누적 상한

**결정**: 24시간. `elapsedMinutes = min(elapsedMinutes, 1440)`

---

## 미결 항목 (밸런싱 단계에서 결정)

- 초기 지급 코인 수량
- 행성 유형별 유지비 구체적 수치
- 희귀도별 탐험 확률 가중치
- Firebase 프로젝트 ID (`google-services.json`)
- Cloud Functions 배포 환경 (Node.js 버전)
- 행성 이미지 실제 디자인 및 업로드 일정