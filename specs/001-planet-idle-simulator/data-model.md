# 데이터 모델: 행성 방치형 투자 시뮬레이션 게임

**작성일**: 2026-05-13

---

## 도메인 모델

### Planet (기존 → 확장)

```kotlin
data class Planet(
    val id: String,                      // UUID
    val type: PlanetType,

    // 행성 스탯 (고정)
    val production: Int,                 // 분당 수익 코인
    val risk: Int,                       // 시세 변동폭 계수 (0~100)
    val investment: Int,                 // 투자 지수
    val eventRate: Int,                  // 이벤트 발생 확률 (0~100)
    val maintenanceCost: Int,            // [신규] 분당 유지비 코인

    // 구매 시 결정
    val buyPrice: Int,
    val acquireTime: Long,

    // 변동 정보
    val currentValue: Int,               // 현재 시세
    val level: Int,
    val totalProfit: Long,               // 누적 순수익
    val status: PlanetStatus,            // [신규] 행성 상태

    // Idle 계산용
    val lastProfitTime: Long
)
```

**상태 전환 규칙**:
- `NORMAL` → `MAINTENANCE_REQUIRED`: 코인 부족으로 유지비 미납 누적, 또는 악재 이벤트 누적 피해
- `NORMAL` / `MAINTENANCE_REQUIRED` → `LOSS`: currentValue < buyPrice
- `MAINTENANCE_REQUIRED` → `NORMAL`: 수리 비용(buyPrice × 0.20) 납부
- `LOSS` → `NORMAL`: currentValue ≥ buyPrice 자동 복구

---

### PlanetStatus (신규)

```kotlin
enum class PlanetStatus {
    NORMAL,                  // 정상 운영, 생산량 100%
    MAINTENANCE_REQUIRED,    // 유지보수 필요, 생산량 50%
    LOSS                     // 손실 상태, 시세 < 매입가
}
```

---

### PlanetMetaData (기존 → 확장)

```kotlin
data class PlanetMetaData(
    val type: PlanetType,
    val displayName: String,
    val description: String,
    val productionMin: Int,
    val productionMax: Int,
    val riskMin: Int,
    val riskMax: Int,
    val investmentMin: Int,
    val investmentMax: Int,
    val eventRateMin: Int,
    val eventRateMax: Int,
    val maintenanceCostMin: Int,         // [신규]
    val maintenanceCostMax: Int,         // [신규]
    val rarity: RarityTier,
    val basePrice: Int,
    val variants: List<PlanetVariant>
)
```

---

### PlanetVariant (기존 — URL 패턴 변경)

```kotlin
data class PlanetVariant(
    val variantId: String,   // "TERRAN_WET-01" 형식
    val imageUrl: String     // Firebase Storage URL (variantId 기반 자동 생성)
)
```

**URL 생성 규칙**:
```
https://firebasestorage.googleapis.com/v0/b/{PROJECT_ID}.appspot.com/o/planets%2F{variantId}.webp?alt=media
```
예: `variantId = "TERRAN_WET-01"` → `.../planets%2FTERRAN_WET-01.webp?alt=media`

---

### EventLog (신규)

```kotlin
data class EventLog(
    val id: Long = 0,
    val type: EventLogType,
    val planetId: String?,           // null이면 시스템 이벤트
    val planetType: String?,         // PlanetType name
    val planetDisplayName: String?,
    val description: String,         // 사람이 읽을 수 있는 설명
    val valueBefore: Int?,           // 시세 변동 전
    val valueAfter: Int?,            // 시세 변동 후
    val coinDelta: Long?,            // 코인 변화량 (양수: 획득, 음수: 지출)
    val occurredAt: Long             // System.currentTimeMillis()
)
```

---

### EventLogType (신규)

```kotlin
enum class EventLogType {
    PLANET_BOUGHT,           // 행성 매입
    PLANET_SOLD,             // 행성 매도 (수수료 포함)
    MARKET_POSITIVE,         // 시장 호재 이벤트
    MARKET_NEGATIVE,         // 시장 악재 이벤트
    STATUS_CHANGED,          // 행성 상태 변화
    MAINTENANCE_COLLECTED,   // 유지비 차감
    PLANET_REPAIRED          // 행성 수리 완료
}
```

---

### MarketEvent (신규)

```kotlin
data class MarketEvent(
    val id: Long = 0,
    val targetPlanetType: PlanetType,   // 대상 행성 유형
    val isPositive: Boolean,            // true: 호재, false: 악재
    val changeRate: Float,              // 변동률 (0.10 ~ 0.50)
    val title: String,                  // 이벤트 제목
    val description: String,
    val occurredAt: Long
)
```

---

## Room 엔티티

### PlanetEntity (기존 → 확장, DB v3 마이그레이션)

```kotlin
@Entity(tableName = "planet_table")
data class PlanetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,
    val maintenanceCost: Int,    // [신규] DEFAULT 0
    val buyPrice: Int,
    val acquireTime: Long,
    val currentValue: Int,
    val level: Int,
    val totalProfit: Long,
    val status: String,          // [신규] PlanetStatus.name, DEFAULT "NORMAL"
    val lastProfitTime: Long
)
```

**v3 마이그레이션 SQL**:
```sql
ALTER TABLE planet_table ADD COLUMN maintenanceCost INTEGER NOT NULL DEFAULT 0;
ALTER TABLE planet_table ADD COLUMN status TEXT NOT NULL DEFAULT 'NORMAL';
```

---

### EventLogEntity (신규)

```kotlin
@Entity(tableName = "event_log_table")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val planetId: String?,
    val planetType: String?,
    val planetDisplayName: String?,
    val description: String,
    val valueBefore: Int?,
    val valueAfter: Int?,
    val coinDelta: Long?,
    val occurredAt: Long
)
```

---

### MarketEventEntity (신규)

```kotlin
@Entity(tableName = "market_event_table")
data class MarketEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetPlanetType: String,
    val isPositive: Boolean,
    val changeRate: Float,
    val title: String,
    val description: String,
    val occurredAt: Long
)
```

---

## DAO 인터페이스

### EventLogDao (신규)

```kotlin
@Dao
interface EventLogDao {
    @Insert
    suspend fun insert(log: EventLogEntity)

    @Query("SELECT * FROM event_log_table ORDER BY occurredAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getLogs(limit: Int = 50, offset: Int = 0): List<EventLogEntity>

    @Query("SELECT COUNT(*) FROM event_log_table")
    suspend fun count(): Int

    // 200건 초과 시 오래된 항목 삭제
    @Query("DELETE FROM event_log_table WHERE id IN (SELECT id FROM event_log_table ORDER BY occurredAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
```

### MarketEventDao (신규)

```kotlin
@Dao
interface MarketEventDao {
    @Insert
    suspend fun insert(event: MarketEventEntity)

    @Query("SELECT * FROM market_event_table ORDER BY occurredAt DESC LIMIT 50")
    fun getRecentEvents(): Flow<List<MarketEventEntity>>
}
```

---

## 핵심 비즈니스 규칙

| 규칙 | 값 | 상수명 |
|------|-----|--------|
| 매도 수수료율 | 5% | `SELL_FEE_RATE = 0.05` |
| 수리 비용율 | 매입가의 20% | `REPAIR_COST_RATE = 0.20` |
| 오프라인 누적 상한 | 1,440분 (24시간) | `MAX_OFFLINE_MINUTES = 1440` |
| 이벤트 로그 최대 보존 | 200건 | `MAX_EVENT_LOG_COUNT = 200` |
| 생산량 패널티 (유지보수 필요) | 50% | `MAINTENANCE_PRODUCTION_RATE = 0.5` |
| 시세 하한 | 매입가의 30% | `MIN_VALUE_RATE = 0.3` |
| 시세 상한 | 매입가의 300% | `MAX_VALUE_RATE = 3.0` |
| 급등락 알림 임계값 | 변동폭 ≥ 20% | `ALERT_CHANGE_THRESHOLD = 0.20` |

---

## Firestore 스키마 (알림 트리거용)

```
notifications/
└── {fcmToken}/
    └── pending/
        └── {documentId}           ← Cloud Function 트리거
            ├── type: String        (MAINTENANCE_REQUIRED | MARKET_ALERT | LOW_COINS)
            ├── title: String
            ├── body: String
            ├── deepLink: String    (screen 경로)
            └── createdAt: Timestamp
```

Cloud Function이 문서 생성 감지 후 FCM 발송, 처리 완료 시 문서 삭제.