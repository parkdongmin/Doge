package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameMaskTest {

    @Test
    fun `masking keeps first and last character`() {
        assertEquals("박*민", maskDisplayName("박동민"))
        assertEquals("남**수", maskDisplayName("남궁민수"))
        assertEquals("박*", maskDisplayName("박동"))
        assertEquals("D****k", maskDisplayName("Dongmin Park"))
        assertEquals("D****k", maskDisplayName(maskDisplayName("Dongmin Park")))
        assertEquals("익명", maskDisplayName(null))
    }

    @Test
    fun `npc names are shown as is but only on the ranking side`() {
        assertEquals("NPC - B01", rankingDisplayName("NPC - B01"))
        assertEquals("박*민", rankingDisplayName("박동민"))
        // 저장 경로(maskDisplayName)는 접두어 예외가 없어 실제 유저가 NPC 행세를 할 수 없다
        assertEquals("N****수", maskDisplayName("NPC - 철수"))
    }
}
