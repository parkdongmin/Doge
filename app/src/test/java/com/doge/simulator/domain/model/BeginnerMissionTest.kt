package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeginnerMissionTest {

    // 갓 시작한 새 게임(스타터 자산: 정찰선 1·대원 1·Lv1 행성 1)
    private val fresh = MissionGameState(
        expeditionCount = 0, readReportCount = 0, maxPlanetLevel = 1, researchUpgrades = 0,
        astronautCount = 1, spaceshipCount = 1, orbitPlayed = false, planetUpgradeTried = false
    )

    private fun progress(step: Int) = MissionProgress(step, orbitPlayed = false, planetUpgradeTried = false)

    @Test
    fun `대상 아님(-1)이나 다 끝낸 경우엔 임무가 없다`() {
        assertNull(beginnerMissionStatus(progress(-1), fresh))
        assertNull(beginnerMissionStatus(progress(BeginnerMission.entries.size), fresh))
    }

    @Test
    fun `새 게임은 아무 임무도 처음부터 완료돼 있지 않다`() {
        BeginnerMission.entries.forEachIndexed { i, mission ->
            val status = beginnerMissionStatus(progress(i), fresh)!!
            assertFalse("${mission.name}이(가) 시작하자마자 완료면 안 된다", status.isComplete)
        }
    }

    @Test
    fun `임무보다 먼저 해둔 것은 그 차례에 진행도와 함께 완료로 뜬다`() {
        // 1번 임무 때 우주선을 먼저 사둠 → 마지막(우주선) 임무 차례에 2/2척으로 완료
        val boughtEarly = fresh.copy(spaceshipCount = 2)
        val status = beginnerMissionStatus(progress(BeginnerMission.BUY_SPACESHIP.ordinal), boughtEarly)!!
        assertTrue(status.isComplete)
        assertEquals(2, status.current)
        assertEquals(2, status.mission.target)
    }

    @Test
    fun `행성 강화는 실패해도 시도만으로 완료`() {
        val step = progress(BeginnerMission.UPGRADE_PLANET.ordinal)
        assertFalse(beginnerMissionStatus(step, fresh)!!.isComplete)
        assertTrue(beginnerMissionStatus(step, fresh.copy(planetUpgradeTried = true))!!.isComplete)
        assertTrue(beginnerMissionStatus(step, fresh.copy(maxPlanetLevel = 2))!!.isComplete)
    }

    @Test
    fun `진행도는 목표를 넘겨 표시하지 않는다`() {
        val status = beginnerMissionStatus(progress(0), fresh.copy(expeditionCount = 5))!!
        assertEquals(1, status.current)
    }

    @Test
    fun `자원이 드는 임무는 바로 앞 임무 보상으로 비용을 채울 수 있다`() {
        // 행성 강화(Lv1→2, 커먼 스타터) 자원 비용 ⊆ 탐사 보내기 보상
        val (_, upgradeCost) = GameConstants.planetUpgradeCost(1, RarityTier.COMMON)
        upgradeCost.forEach { (type, amount) ->
            val reward = BeginnerMission.SEND_EXPEDITION.rewardResources[type] ?: 0L
            assertTrue("강화 재료 $type $amount 를 보상이 못 채움", reward >= amount)
        }
        // 연구 1단계 자원 비용 ⊆ 행성 강화 보상
        val (_, researchCost) = GameConstants.researchUpgradeCost(1)
        researchCost.forEach { (type, amount) ->
            val reward = BeginnerMission.UPGRADE_PLANET.rewardResources[type] ?: 0L
            assertTrue("연구 재료 $type $amount 를 보상이 못 채움", reward >= amount)
        }
    }

    @Test
    fun `코인 보상은 그 임무에 쓰는 코인 이상`() {
        assertTrue(BeginnerMission.UPGRADE_PLANET.rewardCoins >= GameConstants.planetUpgradeCost(1, RarityTier.COMMON).first)
        assertTrue(BeginnerMission.RESEARCH.rewardCoins >= GameConstants.researchUpgradeCost(1).first)
        assertTrue(BeginnerMission.PLAY_ORBIT.rewardCoins >= GameConstants.ORBIT_BET_AMOUNTS.min())
        assertTrue(BeginnerMission.BUY_SPACESHIP.rewardCoins >= GameConstants.SCOUT_SHIP_BASE_COST)
    }
}
