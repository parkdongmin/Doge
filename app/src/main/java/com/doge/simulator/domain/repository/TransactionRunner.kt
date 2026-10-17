package com.doge.simulator.domain.repository

// 여러 저장소에 걸친 쓰기를 한 DB 트랜잭션으로 묶는다 — 중간에 코루틴이 취소되거나 프로세스가 죽으면
// 전부 되돌아가, "탐사는 끝났는데 대원은 아직 탐사 중" 같은 반쪽 상태가 남지 않게 한다
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
