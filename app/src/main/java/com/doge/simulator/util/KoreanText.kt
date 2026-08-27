package com.doge.simulator.util

// 행성 표시명(사용자 콘텐츠 아님, 코드 내 고정 문자열)처럼 동적으로 끼워 넣는 명사 뒤에
// "이/가" 조사를 종성 유무에 맞게 자동으로 붙인다 — 한글 완성형 유니코드는 (코드 - 0xAC00)를
// 28로 나눈 나머지가 종성 인덱스라, 0이면 종성 없음(받침 없음)
fun String.withSubjectParticle(): String {
    val last = trim().lastOrNull() ?: return this
    val hasFinalConsonant = last.code in 0xAC00..0xD7A3 && (last.code - 0xAC00) % 28 != 0
    return this + if (hasFinalConsonant) "이" else "가"
}
