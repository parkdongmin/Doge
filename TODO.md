# 할 일

## 기능·로직 점검 및 수정 (완료, 8개 커밋)

여러 라운드에 걸쳐 게임 로직 전반을 점검하고 발견된 버그를 수정했다. 출시 준비(광고·서명·배포)는
기능·디자인이 마무리된 뒤로 미루고, 이번엔 기능적 정확성에만 집중했다.

### 경제 로직 — 코인/자원 이중 차감 (e49b612, 06a2c46, 3524f70)
- 강화(행성·우주선·연구소)·훈련·영입·구매 각 UseCase가 "잔액 확인 후 별도로 차감"하던 방식(TOCTOU)을
  전부 원자적 `deductCoins`/`consume`으로 교체 — 연타 시 이중 차감·음수 잔액 가능성 제거
- `ResourceRepository.consume/add`를 DB 레벨 원자 쿼리로 교체 (자원 수량 음수화·동시 지급 유실 방지)
- 우주선·연구소 강화는 코인/자원은 원자적으로 막았지만 등급·레벨 반영 자체는 안 막혀 있어서,
  연타 시 비용은 두 번 나가고 등급은 한 단계만 오르는 문제가 남아있었음 — `expectedGrade`/
  `expectedLevel` DB 가드 추가, 충돌 시 비용 환불 + `Conflict` 결과 반환
- 행성 매도(`SellPlanetUseCase`)도 동일 패턴으로 연타 시 코인 중복 지급되던 것 수정
- 행성 구매·슬롯풀 코인 전환도 `ExploreViewModel`에 재진입 방지 플래그(`isResolvingDiscovery`) 추가

### 상태 동기화·타이밍 (e49b612, a094b68, 06a2c46, 3524f70)
- 강화 실패 후 "광고 보고 되돌리기" 버튼이 토스트 타이머(3초)에 묶여 다이얼로그가 열려있어도
  사라지던 것 — 새 강화 시도 시에만 무효화되도록 분리
- `ChooseStoryEventUseCase`: 코인 부족으로 페널티가 실제로 안 깎였는데도 메시지엔 항상
  "-N코인"이 뜨던 것 — `deductCoinsClamped`로 실제 차감액만 표시
- 스토리 이벤트 선택 자체도 DB 가드가 없어 연타 시 보상/페널티가 중복 적용될 수 있던 것 —
  `claimEventChoice`로 먼저 원자적 확정 후에만 보상 적용
- 리더보드가 구매 시점에 고정된 채 갱신 안 되는 `Planet.currentValue`(죽은 필드)를 써서
  강화 투자액이 순위 계산에서 누락되던 것 — `buyPrice + upgradeInvestment`로 통일
- 훈련 완료가 포그라운드 폴링·백그라운드 워커 양쪽에서 동시에 처리될 수 있어 숙련도 계산식이
  바뀌면 이중 적용될 여지가 있던 것 — `status='TRAINING'` DB 가드 추가
- 스킵대기 광고 시청 중 훈련이 이미 끝났는데 광고 콜백이 무조건 TRAINING으로 되살리던 것 —
  전용 `extendTraining`(가드 포함) 메서드로 교체

### 방치 수익 계산 (d80ff90)
- 60초 방치수익 루프와 앱 복귀 시 자동 수집이 거의 동시에 실행되면 같은 경과 구간이
  중복 지급될 수 있던 것 — `CollectProfitUseCase`에 Singleton Mutex + 락 안 DB 재조회로 수정
- 정산에 쓰던 `effectiveProduction`이 분당 정수 표시용으로 미리 내림된 값이라 경과 시간에
  곱하면 소수점 손실이 누적돼 최대 약 7% 덜 지급되던 것 — 내림 안 하는 `preciseProduction` 추가
- `AssetScreen`의 실시간 코인 표시가 액션 직후 순간적으로 어긋날 수 있던 중복 로직을
  `rememberLiveCoinDisplay` 재사용으로 정리
- 탐사 자원 보상이 티어와 무관해 고티어가 저티어보다 시간당 효율이 크게 떨어지던 것 —
  코인 보상과 같은 방식(소요 시간 비례 + 티어당 완만한 배율)으로 스케일링

### UI/네비게이션 (904af42)
- 딥링크로 특정 화면에 진입한 뒤 기기를 회전하면 Activity가 재생성되며 같은 딥링크
  내비게이션이 재실행되던 것 — 읽은 즉시 Intent에서 제거(consume)하도록 수정
- 탭 딥링크(explore/planet/hq/asset)가 하단 탭 바와 달리 `popUpTo`/`restoreState` 없이
  백스택에 중복 쌓이던 것, 서브 화면(탐사기록/행성상세/랭킹/HQ 3분야) 진입에 연타 가드
  (`launchSingleTop`)가 없던 것 수정

### WorkManager (3524f70)
- `TrainingCompleteWorker`·`ExplorationCompleteWorker`·`PlanetMaintenanceWorker`에
  예외 발생 시 `Result.retry()` 추가 (이전엔 일시적 DB 오류로 조용히 영구 실패해 해당
  워커가 담당하는 알림이 다시는 안 뜰 수 있었음)
- 초기화·취소/재예약·빈 리스트 엣지케이스는 이미 정상이었음을 확인

### 인증/랭킹 (46ed63b)
- 구글 로그인 취소 시 "취소는 무시"라는 주석과 달리 실제로는 매번 에러 문구가 뜨던 것 —
  `GetCredentialCancellationException` 별도 처리 추가
- `GoogleIdTokenCredential.createFrom()`이 던질 수 있는 `GetCredentialException`이 아닌
  예외(파싱 오류 등)를 못 잡아 크래시 가능하던 경로에 포괄 catch 추가
- 랭킹 화면이 상위 50위 밖 유저에게 항상 "51위"라는 실제와 무관한 숫자를 보여주던 것 —
  집계 쿼리가 없어 정확한 순위를 알 수 없으므로 "50위 밖"으로 정직하게 표시

### 확인만 하고 손 안 댄 것
- `ExploreViewModel.dispatch()`가 탐사를 DB에 먼저 저장하고 워커를 나중에 예약해 그 사이
  프로세스가 죽으면 워커 없이 남을 수 있음 — 5초 주기 포그라운드 폴링이 자가 복구, 영향은
  알림이 최대 5초 늦는 정도라 손대지 않음
- `AuthViewModel.signOut()`이 로컬 게임 데이터(코인·행성)를 안 지우는 죽은 코드 — 현재
  아무도 호출 안 해서 활성 버그는 아니지만, 나중에 로그아웃 UI를 추가할 때 지뢰가 될 수 있음
- `doge://feed` 딥링크가 매니페스트 주석·명세 문서에만 있고 실제로 아무도 안 보내는 죽은 참조 —
  확인만 하고 미수정
- `AssetScreen`의 판매 다이얼로그가 열려있는 동안 자원 수량이 바뀌어도 다이얼로그 표시값이
  갱신 안 되는 경우가 있으나, 백엔드가 이미 원자적 `consume()`으로 보호돼 있어 실질 위험 없음

## 이모지 → 커스텀 아이콘 교체 (완료, 03fb8af 커밋·푸시됨)

연구소 4분야(🔭🌌👥🚀), 탐사 카테고리/우주인 전문분야(자원 아이콘 재사용), 잠금/코인/에너지/광고(🔒💰🪙⚡📺), 행성/트로피/메달(🪐🏆🥇🥈🥉), 도감 등록/신규(📖🆕), 진행중 탐사/미확인 보고서(📡📬), 팀 인원(👥), 강화 위험구간 경고(`PlanetDetailScreen.kt`), 자산 통계(📊📈), 선택 체크마크(✓), 홈 버튼(🏠), 강화/탐사 결과 토스트(🎉💥😞🔄✨) — 아이콘 19종 추가 완료.

추가로 이번 라운드에서 정리:
- 시스템 알림 타이틀 3곳(훈련 완료·행성 수익·탐사 완료)의 장식용 이모지 제거, `TrainingCompleteWorker`의 알림 아이콘이 딥링크 대상(HQ)과 안 맞던 것도 수정
- `ChooseStoryEventUseCase.kt`의 스토리 로그 문자열 "⚠" 제거 → `ExpeditionHistoryScreen`에서 `ic_ui_danger` 아이콘으로 렌더링, 위험/경고 아이콘을 `ic_ui_danger`로 통일(`ic_ui_warning` 삭제)
- `ExpeditionCategory`·`AstronautSpecialty`·`ResearchField`의 미사용 이모지 필드, `AssetScreen`의 `ResourceRow` 미사용 이모지 폴백 분기 등 죽은 코드 정리
- 상태 표시 점(🟡🟢)은 이모지 없이 `StatusGreen`/`StatusRed` 색상으로 처리되어 있음을 확인, 별도 작업 불필요

남은 이모지 없음(Compose UI, 알림, 로그 문자열, 리소스 전체 재확인 완료).

## 출시 전 확인 필요

기능·디자인이 마무리된 뒤 진행. 아래 4개는 재점검 결과 이미 로컬 설정 완료된 상태(문서만 안 갱신돼 있었음):

- [x] AdMob release 빌드 유닛 ID — `local.properties`에 실제 ID 설정 확인됨 (`ADMOB_APP_ID` 등)
- [x] PF Stardust ExtraBold 폰트 파일 — `res/font/pf_stardust_extrabold.ttf` 존재 확인
- [x] `google-services.json` — `app/`에 존재 확인
- [x] `local.properties`에 `FIREBASE_STORAGE_BASE_URL` 실값 — 설정 확인됨
- [ ] Cloud Functions 배포 여부 미확인 (`functions/` → `npm install && firebase deploy --only functions`) — 로컬에 firebase CLI가 없어 배포 이력을 직접 확인 못 함, Firebase 콘솔에서 확인 필요
- [ ] Play Store 출시용 서명 설정(keystore + `signingConfigs`) — release 빌드에 아직 없음
- [ ] release `isMinifyEnabled` — 현재 `false`, R8/난독화 적용 여부 결정 필요
