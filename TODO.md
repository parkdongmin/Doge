# 할 일

## 이모지 → 커스텀 아이콘 교체 (거의 완료, 커밋 대기 중)

완료: 연구소 4분야(🔭🌌👥🚀), 탐사 카테고리/우주인 전문분야(자원 아이콘 재사용), 잠금/코인/에너지/광고(🔒💰🪙⚡📺), 행성/트로피/메달(🪐🏆🥇🥈🥉), 도감 등록/신규(📖🆕), 진행중 탐사/미확인 보고서(📡📬), 팀 인원(👥), 강화 위험구간 경고(⚠, `PlanetDetailScreen.kt`), 자산 통계(📊📈), 선택 체크마크(✓), 홈 버튼(🏠), 강화/탐사 결과 토스트(🎉💥😞🔄✨) — 워킹트리에 반영됨, **아직 커밋 안 됨**

남은 항목:

- [ ] ⚠ `ChooseStoryEventUseCase.kt:35` — 페널티 선택지 로그 문자열에 이모지가 그대로 남아 있음 (동적 문자열 임베드용이라 우선순위 낮음, 이전 메모 유지)
- [ ] 🟡🟢 상태 표시 점 — 이모지는 안 쓰고 `StatusGreen`/`StatusRed` 색상으로 이미 대체되어 있어 실질적으로 해결된 것으로 보임, 재확인만 필요

## 출시 전 확인 필요 (이전 점검에서 발견)

- [ ] AdMob release 빌드 유닛 ID를 테스트 ID에서 실제 ID로 교체 (`app/build.gradle.kts`)
- [ ] PF Stardust ExtraBold 폰트 파일 실제 추가 (`res/font/`)
- [ ] `google-services.json` 배치 + Firebase Console 설정
- [ ] `local.properties`에 `FIREBASE_STORAGE_BASE_URL` 실값
- [ ] Cloud Functions 배포 (`functions/` → `npm install && firebase deploy --only functions`)
