# Doge 🚀

우주인을 고용하고 우주선을 강화해 탐사팀을 파견하고, 발견한 행성과 자원으로 회사를 키워나가는 **우주 탐사 타이쿤** 안드로이드 게임입니다.

핵심 사이클: **우주인 고용 + 우주선 구매 → 탐사팀 구성·파견 → 자원 획득 + 행성 발견 → 행성/우주선/연구소 강화 → 재투자**

## 주요 기능

- **탐사(Explore)**: 우주선과 우주인을 파견해 미지의 구역을 탐사하고 행성·자원을 발견합니다. 태양계부터 미지의 공간까지 10개 티어로 구성되며, 상위 티어는 보유 행성 등급/개수 조건을 충족해야 해금됩니다.
- **행성(Planet)**: 탐사로 발견한 행성은 시간에 따라 코인을 생산합니다. 코인+자원으로 강화(최대 20레벨)해 생산량을 늘릴 수 있고, 매도 시 매입가+강화 투자액 기준으로 수수료(5%)를 차감한 금액을 받습니다.
- **정거장(HQ)**: 우주인 고용·훈련(관측소), 우주선 강화(격납고), 연구소 업그레이드를 관리하는 본부입니다.
  - **우주인**: 모집 센터에서 후보 풀(3시간 주기 갱신)을 통해 영입하고, 기본/고급 훈련으로 숙련도를 올립니다. 전문 분야가 탐사 성공률·자원 획득량에 영향을 줍니다.
  - **우주선**: 초기 정찰선에서 시작해 최대 6등급까지 강화하며 승무원 수·속도·화물량·성공률이 향상됩니다.
  - **연구소**: 자원을 소모해 레벨을 올려 탐사 발견 확률 등 전역 보너스를 얻습니다.
- **자산(Asset)**: 보유 코인, 행성 수, 행성 가치 합산, 누적 순수익을 한눈에 확인하고, 보유 자원을 코인으로 판매할 수 있습니다.
- **탐사 기록 & 랭킹**: 완료된 원정 결과(획득 자원, 행성 발견 여부 등)를 기록으로 확인하고, 리더보드로 다른 플레이어와 순위를 비교할 수 있습니다.
- **로그인**: Google 계정(Credential Manager) 로그인 및 Firebase Auth 연동.
- **알림**: 훈련 완료·탐사 완료를 기기 알림(WorkManager 로컬 알림)으로 안내합니다.

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose (Material 3), Navigation Compose, Coil(이미지/GIF)
- **아키텍처**: Clean Architecture (`domain` / `data` / `presentation`), Hilt(DI), Kotlin Coroutines + Flow
- **로컬 저장소**: Room (SQLite)
- **백그라운드 처리**: WorkManager
- **백엔드/인프라**: Firebase (Auth, Firestore, Storage, Cloud Messaging) + Cloud Functions(Node.js)

## 프로젝트 구조

```
app/src/main/java/com/doge/simulator/
├── data/            # Room DAO/Entity/Mapper, Repository 구현, WorkManager 워커
├── di/              # Hilt 모듈
├── domain/          # 모델, Repository 인터페이스, UseCase, 게임 밸런스 상수(GameConstants)
├── firebase/        # FCM/Firestore 연동
├── presentation/    # 화면(explore/planet/hq/asset/rank/auth), ViewModel, 내비게이션
└── ui/theme/        # Compose 테마, 컬러, 타이포그래피

functions/           # Firebase Cloud Functions (알림 발송)
specs/               # spec-kit 기반 기능 명세·계획 문서
```

## 시작하기

### 요구 사항

- Android Studio (최신 안정 버전)
- JDK 11
- Firebase 프로젝트 (`google-services.json`을 `app/` 아래에 배치)

### 로컬 설정

`local.properties`에 아래 값을 추가합니다.

```properties
GOOGLE_WEB_CLIENT_ID=<Firebase/Google Cloud 콘솔에서 발급받은 웹 클라이언트 ID>
FIREBASE_STORAGE_BASE_URL=<Firebase Storage 다운로드 base URL>
```

### 빌드 & 실행

```bash
./gradlew assembleDebug     # 디버그 APK 빌드
./gradlew installDebug      # 연결된 기기/에뮬레이터에 설치
```

### Cloud Functions

```bash
cd functions
npm install
npm run serve   # 로컬 에뮬레이터
npm run deploy  # 배포
```

## 문서

기능 명세, 구현 계획 등 상세 설계 문서는 [`specs/002-space-exploration-tycoon/`](specs/002-space-exploration-tycoon/) 에서 확인할 수 있습니다.
