package com.doge.simulator.presentation.screen.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GoogleSignInFlowTest {

    // 시도마다 "로그인 창이 떴는지"를 미리 정해 두고, begin/end 짝이 맞는지도 센다
    private class FakeMonitor(vararg shown: Boolean) : GoogleSignInFlow.UiShownMonitor {
        private val queue = ArrayDeque(shown.toList())
        var begins = 0
        var ends = 0
        private var current = false
        override fun begin() { begins++; current = queue.removeFirstOrNull() ?: false }
        override fun end(): Boolean { ends++; return current }
    }

    private var bottomCalls = 0
    private var fullCalls = 0

    private fun flow(
        monitor: FakeMonitor,
        bottom: suspend () -> String,
        full: suspend () -> String = { fail("2차가 불리면 안 됨"); "" }
    ) = GoogleSignInFlow(
        uiMonitor = monitor,
        clock = { 0L },
        bottomSheet = { bottomCalls++; bottom() },
        fullScreen = { fullCalls++; full() }
    )

    @Test
    fun bottomSheetSuccess_returnsToken_withoutFallback() = runBlocking {
        val m = FakeMonitor(true)
        val out = flow(m, bottom = { "tokenA" }).run()
        assertEquals(GoogleSignInFlow.Outcome.Token("tokenA"), out)
        assertEquals(0, fullCalls)
        assertEquals(1, m.begins); assertEquals(1, m.ends)
    }

    @Test
    fun cancelAfterSheetShown_isUserCancel_silent() = runBlocking {
        val m = FakeMonitor(true)
        val out = flow(m, bottom = { throw GetCredentialCancellationException() }).run()
        assertEquals(GoogleSignInFlow.Outcome.UserCancelled, out)
        assertEquals(0, fullCalls)
        assertEquals(m.begins, m.ends)
    }

    @Test
    fun cancelWithoutSheet_fallsBackToFullScreen() = runBlocking {
        val m = FakeMonitor(false, true)
        val out = flow(m, bottom = { throw GetCredentialCancellationException() }, full = { "tokenB" }).run()
        assertEquals(GoogleSignInFlow.Outcome.Token("tokenB"), out)
        assertEquals(1, fullCalls)
        assertEquals(2, m.begins); assertEquals(2, m.ends)
    }

    @Test
    fun noCredential_fallsBack_thenUserCancelsFullScreen_silent() = runBlocking {
        val m = FakeMonitor(false, true)
        val out = flow(m, bottom = { throw NoCredentialException() }, full = { throw GetCredentialCancellationException() }).run()
        assertEquals(GoogleSignInFlow.Outcome.UserCancelled, out)
        assertEquals(m.begins, m.ends)
    }

    @Test
    fun bothFail_reportsBothCodes() = runBlocking {
        val m = FakeMonitor(false, false)
        val out = flow(m, bottom = { throw GetCredentialCancellationException() }, full = { throw NoCredentialException("no account") }).run()
        assertTrue(out is GoogleSignInFlow.Outcome.Failed)
        val code = (out as GoogleSignInFlow.Outcome.Failed).code
        assertTrue(code, code.contains("1차 USER_CANCELED 창없음"))
        assertTrue(code, code.contains("2차 NO_CREDENTIAL"))
        assertTrue(code, code.contains("no account"))
    }

    @Test
    fun secondCancelWithoutSheet_isFailureNotSilent() = runBlocking {
        val m = FakeMonitor(false, false)
        val out = flow(m, bottom = { throw GetCredentialCancellationException() }, full = { throw GetCredentialCancellationException() }).run()
        assertTrue(out is GoogleSignInFlow.Outcome.Failed)
    }

    @Test
    fun parseFailure_usesStableCode() = runBlocking {
        val m = FakeMonitor(true, true)
        val out = flow(m,
            bottom = { throw GoogleSignInFlow.CredentialParseException("TOKEN_PARSE") },
            full = { throw RuntimeException("boom") }
        ).run()
        val code = (out as GoogleSignInFlow.Outcome.Failed).code
        assertTrue(code, code.contains("1차 TOKEN_PARSE"))
        assertTrue(code, code.contains("2차 UNEXPECTED (boom)"))
    }

    @Test
    fun coroutineCancellation_propagates_andMonitorIsReleased() {
        val m = FakeMonitor(false)
        try {
            runBlocking { flow(m, bottom = { throw CancellationException("left screen") }).run() }
            fail("취소가 전파되지 않음")
        } catch (e: CancellationException) {
            // 기대한 경로
        }
        assertEquals(0, fullCalls)
        assertEquals(1, m.begins); assertEquals(1, m.ends)
    }
}
