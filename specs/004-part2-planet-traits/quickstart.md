# Quickstart: 2부 확장 검증

## 빌드·테스트

Bash에서 실행할 때는 JAVA_HOME을 Android Studio의 jbr로 잡는다. 결과는 출력을 grep하지 말고 **종료 코드**로 판단한다.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:compileDebugKotlin -q; echo "exit=$?"
./gradlew :app:testDebugUnitTest -q; echo "exit=$?"
```

개별 테스트 (구현 후):

```bash
./gradlew :app:testDebugUnitTest --tests "*PlanetTraitTableTest" --tests "*PlanetTraitRollerTest" \
  --tests "*PlanetTraitEffectsTest" --tests "*GameConstantsTierLadderTest" --tests "*ExpeditionEconomyTest" \
  --tests "*GenerateExpeditionReportUseCaseTest" --tests "*Part2ProgressSimTest" --tests "*SnapshotLoaderTest"
```

수치 재현 (balance.md 근거):

```bash
cd specs/004-part2-planet-traits/sim
node verify.js          # 적합도 밴드·부작용·티어별 발견 결과
node bad_traits.js      # 나쁜 특성 행성별 시세 값
node progress_sim.js    # 2부 진행 속도
```

## 실기기 확인 (사용자가 직접)

실제 기기의 앱 DB는 adb로 고치지 않는다. 아래는 사용자가 플레이하면서 확인할 항목이다.

| # | 시나리오 | 기대 결과 | 관련 |
|---|---|---|---|
| 1 | 1부를 완주한 세이브로 업데이트 후 실행 | 탐사 탭에서 관제탑 2부 안내 5줄이 한 번 뜨고, 팀 구성 시트에서 T11이 강조됨 | FR-001, SC-007 |
| 2 | 1부 미완주 세이브 (T10까지 열림) | T11~T15 칩이 잠긴 채 보이고, T11과 배너에 "1부 완주" 조건 | US2-5 |
| 3 | T11 탐사에서 행성 발견 | 발견 창에 특성·강도·적합도·설명·숫자·특성 시세가 보임 | US1-1 |
| 4 | 발견 창을 띄운 채 앱 종료 → 재실행 | 같은 행성·같은 특성이 다시 뜸(다시 굴리지 않음) | research R1 |
| 5 | 특성 행성 구매 → 상세 | 악재 확률·드롭·강화 성공률이 특성 반영 값 | FR-008 |
| 6 | 슬롯이 가득 찬 상태에서 특성 행성 "코인으로 받기" | 다음 해제 목표 배너의 특성 행성 수 +1 | FR-002 |
| 7 | 발견 창 그냥 닫기 | 배너 숫자 변화 없음 | FR-002 |
| 8 | 특성 행성 매도 | 열린 티어 유지 | US2-3 |
| 9 | 탐사 일지 | 옛 완주 기록이 "1부 완주"로 표시, 6장 기록 시작 | FR-016 |
| 10 | 클라우드 저장 → 재설치 → 복원 | 특성·카운터·장 진행 유지 | FR-017 |

## 배포 전 체크

- versionCode 14 이상. versionName은 2부 규모라 1.6.0을 제안(사용자 결정)
- `main` 병합: v1.5.3 버전 커밋(2e18c59)이 이 브랜치에 없으므로 먼저 병합한다.
- 새 광고 단위 없음. 있으면 release buildType에도 넣는다.
- DB 23 / 스냅샷 스키마 5 → 1.5.x 앱은 1.6.0 클라우드 세이브를 복원하지 않고 건너뜀(로컬 유지). 릴리스 노트에 "업데이트 후 저장" 안내가 필요한지 검토한다.
