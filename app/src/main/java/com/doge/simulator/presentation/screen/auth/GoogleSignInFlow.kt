package com.doge.simulator.presentation.screen.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlin.coroutines.cancellation.CancellationException

// 구글 로그인 시도 순서와 실패 판정. 일부 사용자(방송하는 지인 그룹)에게서 버튼을 눌러도 계정 선택
// 하단 시트가 뜨지 않고 아무 반응이 없는 문제가 있었다 — 시트가 뜨기도 전에 시스템이 '취소'를
// 돌려주는데, 예전 코드는 취소를 "사용자가 닫음"으로 보고 조용히 무시했기 때문.
//
// 1차: 계정 선택 하단 시트(GetGoogleIdOption)
//   - 성공 → 토큰
//   - 취소 + 로그인 창이 실제로 떴음 → 사용자가 닫은 것, 조용히 끝
//   - 그 외(창 없이 취소, 계정 없음, 기타 오류) → 2차로
// 2차: 전체 화면 구글 로그인(GetSignInWithGoogleOption) — 하단 시트를 막는 무언가가 있어도 다른 경로라 통과할 수 있음
//   - 성공 → 토큰
//   - 취소 + 창이 떴음 → 사용자가 닫은 것, 조용히 끝
//   - 그 외 → 실패. 1·2차 오류 코드를 같이 화면에 보여 원인을 캡처로 받을 수 있게
//
// "사용자가 닫았는지"는 시간으로 추측하지 않고 로그인 창이 실제로 떴는지(우리 화면이 일시정지됐는지)로 판단한다.
// 느린 기기에선 창 없이 취소되는 데도 몇 초가 걸릴 수 있어 시간 기준은 오판한다
class GoogleSignInFlow(
    private val uiMonitor: UiShownMonitor,
    private val clock: () -> Long,
    private val bottomSheet: suspend () -> String,
    private val fullScreen: suspend () -> String
) {
    // 로그인 창(다른 화면)이 시도 도중 우리 화면을 가렸는지 추적
    interface UiShownMonitor {
        fun begin()
        fun end(): Boolean
    }

    sealed interface Outcome {
        data class Token(val idToken: String) : Outcome
        object UserCancelled : Outcome
        data class Failed(val code: String) : Outcome
    }

    // 토큰을 꺼내지 못한 경우(예상 밖 자격 증명 형식 등) attempt 람다가 던진다. 클래스 이름은 출시 빌드에서
    // 난독화되므로 오류 코드는 항상 이 code 문자열로 남긴다
    class CredentialParseException(val code: String) : Exception(code)

    private sealed interface AttemptResult {
        data class Token(val idToken: String) : AttemptResult
        data class Failure(val userCancelled: Boolean, val code: String) : AttemptResult
    }

    suspend fun run(): Outcome {
        val first = attempt("1차", bottomSheet)
        if (first is AttemptResult.Token) return Outcome.Token(first.idToken)
        first as AttemptResult.Failure
        if (first.userCancelled) return Outcome.UserCancelled

        val second = attempt("2차", fullScreen)
        if (second is AttemptResult.Token) return Outcome.Token(second.idToken)
        second as AttemptResult.Failure
        if (second.userCancelled) return Outcome.UserCancelled
        return Outcome.Failed("${first.code} | ${second.code}")
    }

    private suspend fun attempt(label: String, block: suspend () -> String): AttemptResult {
        val startedAt = clock()
        uiMonitor.begin()
        val result = try {
            Result.success(block())
        } catch (e: CancellationException) {
            // 화면을 벗어나 코루틴이 취소된 것 — 실패로 바꾸면 안 되고 그대로 전파(관찰자는 떼고)
            uiMonitor.end()
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        // 관찰 종료는 여기 한 곳에서만 — 모든 비전파 경로가 지나간다
        val uiShown = uiMonitor.end()
        val ms = clock() - startedAt

        result.getOrNull()?.let { return AttemptResult.Token(it) }
        return when (val e = result.exceptionOrNull()) {
            // GetCredentialCancellationException은 GetCredentialException의 하위라 먼저 검사
            is GetCredentialCancellationException -> AttemptResult.Failure(
                userCancelled = uiShown,
                code = "$label ${shortType(e.type)} ${if (uiShown) "창있음" else "창없음"} ${ms}ms"
            )
            is GetCredentialException ->
                AttemptResult.Failure(false, "$label ${shortType(e.type)} ${ms}ms${e.message.asDetail()}")
            is CredentialParseException -> AttemptResult.Failure(false, "$label ${e.code}")
            else -> AttemptResult.Failure(false, "$label UNEXPECTED${e?.message.asDetail()}")
        }
    }

    private fun shortType(type: String): String =
        type.substringAfterLast('.').removePrefix("TYPE_")

    private fun String?.asDetail(): String =
        if (isNullOrBlank()) "" else " (${take(60)})"
}
