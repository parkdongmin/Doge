# 할 일

## 이모지 → 커스텀 아이콘 교체 (완료, 03fb8af 커밋·푸시됨)

연구소 4분야(🔭🌌👥🚀), 탐사 카테고리/우주인 전문분야(자원 아이콘 재사용), 잠금/코인/에너지/광고(🔒💰🪙⚡📺), 행성/트로피/메달(🪐🏆🥇🥈🥉), 도감 등록/신규(📖🆕), 진행중 탐사/미확인 보고서(📡📬), 팀 인원(👥), 강화 위험구간 경고(`PlanetDetailScreen.kt`), 자산 통계(📊📈), 선택 체크마크(✓), 홈 버튼(🏠), 강화/탐사 결과 토스트(🎉💥😞🔄✨) — 아이콘 19종 추가 완료.

추가로 이번 라운드에서 정리:
- 시스템 알림 타이틀 3곳(훈련 완료·행성 수익·탐사 완료)의 장식용 이모지 제거, `TrainingCompleteWorker`의 알림 아이콘이 딥링크 대상(HQ)과 안 맞던 것도 수정
- `ChooseStoryEventUseCase.kt`의 스토리 로그 문자열 "⚠" 제거 → `ExpeditionHistoryScreen`에서 `ic_ui_danger` 아이콘으로 렌더링, 위험/경고 아이콘을 `ic_ui_danger`로 통일(`ic_ui_warning` 삭제)
- `ExpeditionCategory`·`AstronautSpecialty`·`ResearchField`의 미사용 이모지 필드, `AssetScreen`의 `ResourceRow` 미사용 이모지 폴백 분기 등 죽은 코드 정리
- 상태 표시 점(🟡🟢)은 이모지 없이 `StatusGreen`/`StatusRed` 색상으로 처리되어 있음을 확인, 별도 작업 불필요

남은 이모지 없음(Compose UI, 알림, 로그 문자열, 리소스 전체 재확인 완료).

## 출시 전 확인 필요 (이전 점검에서 발견)

- [ ] AdMob release 빌드 유닛 ID를 테스트 ID에서 실제 ID로 교체 (`app/build.gradle.kts`)
- [ ] PF Stardust ExtraBold 폰트 파일 실제 추가 (`res/font/`)
- [ ] `google-services.json` 배치 + Firebase Console 설정
- [ ] `local.properties`에 `FIREBASE_STORAGE_BASE_URL` 실값
- [ ] Cloud Functions 배포 (`functions/` → `npm install && firebase deploy --only functions`)
