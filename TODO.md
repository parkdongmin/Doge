# 할 일

## 이모지 → 커스텀 아이콘 교체 (진행 중)

완료: 연구소 4분야(🔭🌌👥🚀), 탐사 카테고리/우주인 전문분야(자원 아이콘 재사용), 잠금/코인/에너지/광고(🔒💰🪙⚡📺), 행성/트로피/메달(🪐🏆🥇🥈🥉)

다음 배치 후보 (우선순위순은 아님, 필요할 때 순서 정해서 진행):

- [ ] 📖 도감 등록됨 / 🆕 신규 미등록 — `ExploreScreen.kt` (`StatChipMini`에 이미 iconRes 파라미터 있음, 이미지만 만들면 바로 연결 가능)
- [ ] 📡 진행 중인 탐사 / 📬 미확인 보고서 — `ExpeditionHistoryScreen.kt` 섹션 헤더
- [ ] 👥 팀 인원 표시 — `ExpeditionHistoryScreen.kt` (연구소 인사관리 아이콘 `ic_research_human_management` 재사용 검토)
- [ ] ⚠ 경고 — `PlanetDetailScreen.kt`(위험구간 강화), `ChooseStoryEventUseCase.kt`(페널티 선택지)
- [ ] 📊📈 자산 통계 아이콘 — `AssetScreen.kt`
- [ ] 🟡🟢 상태 표시 점 — `AssetScreen.kt` (단순 Compose Canvas 원으로 대체해도 충분할 수 있어 이미지 제작이 꼭 필요한지 먼저 판단)
- [ ] ✓ 선택 체크마크 — `ExploreScreen.kt` (Material Icons 벡터로 대체 가능, 커스텀 아트 불필요할 수 있음)
- [ ] 🚀 잔여 사용처 — "탐사 파견" 버튼, "N팀 탐사중" 라벨 등 (기존 `spaceship_2.png` 재사용 검토)
- [ ] 🎉💥😞🔄✨ 강화/탐사 결과 토스트 메시지 — `PlanetViewModel.kt`, `ExploreScreen.kt` (동적 문자열에 임베드된 장식용이라 우선순위 낮음)

## 출시 전 확인 필요 (이전 점검에서 발견)

- [ ] AdMob release 빌드 유닛 ID를 테스트 ID에서 실제 ID로 교체 (`app/build.gradle.kts`)
- [ ] PF Stardust ExtraBold 폰트 파일 실제 추가 (`res/font/`)
- [ ] `google-services.json` 배치 + Firebase Console 설정
- [ ] `local.properties`에 `FIREBASE_STORAGE_BASE_URL` 실값
- [ ] Cloud Functions 배포 (`functions/` → `npm install && firebase deploy --only functions`)
