# 알림 계약서: FCM + Firebase Cloud Functions

**작성일**: 2026-05-13

---

## 개요

앱(WorkManager) → Firestore 문서 쓰기 → Cloud Function 트리거 → FCM 알림 전송 → 기기 수신

---

## Firestore 알림 문서 형식

**컬렉션 경로**: `notifications/{fcmToken}/pending/{autoId}`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | String | ✓ | MAINTENANCE_REQUIRED \| MARKET_ALERT \| LOW_COINS |
| `title` | String | ✓ | 알림 제목 |
| `body` | String | ✓ | 알림 본문 |
| `deepLink` | String | ✓ | 탭 시 이동할 화면 경로 |
| `createdAt` | Timestamp | ✓ | 생성 시각 |

**deepLink 경로 규칙**:
- 행성 상세: `doge://planet/{planetId}`
- 피드(이벤트 로그): `doge://feed`
- 자산 화면: `doge://asset`

---

## 알림 유형별 내용

### MAINTENANCE_REQUIRED

```json
{
  "type": "MAINTENANCE_REQUIRED",
  "title": "⚠️ 행성 유지보수 필요",
  "body": "{행성명}이(가) 유지보수가 필요한 상태입니다. 생산량이 50% 감소 중!",
  "deepLink": "doge://planet/{planetId}"
}
```

### MARKET_ALERT (급등락, 변동폭 ≥ 20%)

```json
{
  "type": "MARKET_ALERT",
  "title": "📈 시장 이벤트 발생" | "📉 시장 이벤트 발생",
  "body": "{행성 유형} 시세가 {변동률}% {상승|하락}했습니다!",
  "deepLink": "doge://feed"
}
```

### LOW_COINS (보유 코인 < 최소 유지비)

```json
{
  "type": "LOW_COINS",
  "title": "💸 코인 부족 경고",
  "body": "코인이 부족하여 행성 유지비를 납부할 수 없습니다.",
  "deepLink": "doge://asset"
}
```

---

## Cloud Function 처리 흐름

```javascript
// functions/index.js
exports.sendPlanetNotification = onDocumentCreated(
  "notifications/{token}/pending/{docId}",
  async (event) => {
    const data = event.data.data();
    const token = event.params.token;

    await admin.messaging().send({
      token,
      notification: { title: data.title, body: data.body },
      data: { deepLink: data.deepLink },
      android: { priority: "high" }
    });

    await event.data.ref.delete(); // 처리 후 문서 삭제
  }
);
```

---

## 앱 수신 처리 (MainActivity)

```kotlin
// FCM 데이터 메시지 수신 시 deepLink 파싱
val deepLink = remoteMessage.data["deepLink"]
// NavController로 해당 화면 이동
```

---

## 알림 채널 (Android 8.0+)

| 채널 ID | 이름 | 중요도 |
|---------|------|--------|
| `channel_market` | 시장 이벤트 | HIGH |
| `channel_maintenance` | 행성 유지보수 | HIGH |
| `channel_coins` | 코인 경고 | DEFAULT |