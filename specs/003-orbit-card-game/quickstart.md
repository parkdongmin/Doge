# Quickstart: ORBIT — Crew Lounge Card Mini-Game

## 도메인 로직만 검증 (UI 없이, 권장 순서)

기획 원문 16번 항목에 따라 UI 이전에 게임 로직 전체를 유닛 테스트로 검증한다.

```powershell
# 도메인 계층만 컴파일/테스트 (Android 인스트루먼트 테스트 불필요)
./gradlew testDebugUnitTest --tests "com.doge.simulator.domain.model.orbit.*"
./gradlew testDebugUnitTest --tests "com.doge.simulator.domain.usecase.orbit.*"
./gradlew testDebugUnitTest --tests "com.doge.simulator.domain.ai.*"
```

최소 통과 기준(권장):
- 8종 카드 각각의 이펙트 케이스(정답/오답 SCOUT_DRONE, SHIELD로 무효화되는 5종 효과, AI_CORE 강제 사용, CAPTAIN의 OUT/비OUT 분기 등)
- 덱 소진 3단계 비교(손패 Power → 사용 카드 총합 → DRAW) 각각의 케이스
- SIGNAL 3 선취 매치 종료
- B01OrbitAi가 플레이어 손패를 참조하지 않는지(리플렉션 또는 설계상 원천 차단으로 검증)

## 앱에서 수동 확인 (UI 완성 후)

1. 앱을 디버그 빌드로 실행 (`ADMOB_APP_ID` 등은 기존 `local.properties` 설정 그대로 사용 — 디버그는 항상 Google 공식 테스트 광고 ID 사용).
2. 정거장 탭 → 휴게실 진입 → B-01 대사 확인.
3. 카드 테이블 선택 → 베팅 화면에서 금액(4단계) + 위험도 티어(3단계) 선택 → GAME START.
4. 매치를 끝까지 진행해 SIGNAL 3개 선취(승리) 또는 패배까지 확인. 결과 화면의 BET/WIN 또는 BET/RESULT 표시, 재화 반영 확인.
5. 매치 도중 시스템 뒤로가기로 이탈 → 패배 처리 및 재화 미환불 확인(FR-016).
6. 휴게실에서 리워드 광고 버튼을 5회 사용 → 6번째부터 비활성화 확인. 광고 로드 실패/스킵 시 횟수가 차감되지 않는지 확인(테스트 광고 유닛으로는 실패 케이스 재현이 어려울 수 있으므로 네트워크를 끄고 재현하거나 코드 리뷰로 대체 가능).
7. 재화가 모든 베팅 옵션의 최저 금액보다 적을 때 베팅 화면에서 해당 옵션들이 비활성화되는지 확인.

## 참고

- 정확한 위험도 티어별 실수 빈도/보상 배율 수치, 전면광고 노출 빈도는 스펙 Assumptions에 따라 이번 구현 범위에서 확정하지 않는다(플레이스홀더 값으로 구현 후 추후 플레이테스트/밸런싱에서 조정).
