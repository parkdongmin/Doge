package com.doge.simulator.data.local.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotLoaderTest {

    @Test
    fun `minimal old snapshot loads with defaults`() {
        // roomDbVersion 하나만 있는 구버전 세이브 — 나머지 필드는 기본값으로 채워져야 한다.
        val json = """{"snapshotSchemaVersion":1,"roomDbVersion":17}"""
        val result = loadSnapshot(json)

        assertTrue(result is SnapshotLoad.Loaded)
        val snapshot = (result as SnapshotLoad.Loaded).snapshot
        assertEquals(17, snapshot.roomDbVersion)
        assertEquals(null, snapshot.user)
        assertTrue(snapshot.planets.isEmpty())
    }

    @Test
    fun `unknown fields are ignored (removed-column forward compat)`() {
        // 나중에 삭제된 컬럼이 들어있는 옛 세이브라도 무시하고 읽혀야 한다.
        val json = """{"snapshotSchemaVersion":1,"roomDbVersion":17,"legacyRemovedField":42,"planets":[]}"""
        assertTrue(loadSnapshot(json) is SnapshotLoad.Loaded)
    }

    @Test
    fun `missing schema version is treated as v1`() {
        val json = """{"roomDbVersion":17}"""
        assertTrue(loadSnapshot(json) is SnapshotLoad.Loaded)
    }

    @Test
    fun `future schema version is incompatible`() {
        // 이 앱보다 높은 버전 세이브는 다운그레이드하지 않고 건너뛴다.
        val json = """{"snapshotSchemaVersion":999,"roomDbVersion":99}"""
        assertEquals(SnapshotLoad.Incompatible, loadSnapshot(json))
    }

    @Test
    fun `garbage payload is corrupt`() {
        assertEquals(SnapshotLoad.Corrupt, loadSnapshot("not json at all"))
        assertEquals(SnapshotLoad.Corrupt, loadSnapshot(""))
    }

    @Test
    fun `old saves convert negative production into the broken state`() {
        val planet = """"type":"NO_ATMOSPHERE","production":60,"risk":6,"investment":0,"eventRate":50,
            "buyPrice":2260,"acquireTime":0,"level":1,"totalProfit":0,"lastProfitTime":0"""
        val json = """{"snapshotSchemaVersion":1,"roomDbVersion":19,"planets":[
            {"id":"neg",$planet,"productionMultiplier":-0.2,"marketAdjustment":-2712},
            {"id":"high",$planet,"productionMultiplier":2.6,"marketAdjustment":3616},
            {"id":"normal",$planet}]}"""
        val planets = ((loadSnapshot(json) as SnapshotLoad.Loaded).snapshot.planets).associateBy { it.id }

        assertEquals(1.0, planets.getValue("neg").lossMultiplier, 1e-9)
        assertEquals(0.5, planets.getValue("neg").productionMultiplier, 1e-9)
        assertEquals(-1130L, planets.getValue("neg").marketAdjustment)

        assertEquals(0.0, planets.getValue("high").lossMultiplier, 1e-9)
        assertEquals(2.0, planets.getValue("high").productionMultiplier, 1e-9)
        assertEquals(2260L, planets.getValue("high").marketAdjustment)

        assertEquals(0.0, planets.getValue("normal").lossMultiplier, 1e-9)
        assertEquals(1.0, planets.getValue("normal").productionMultiplier, 1e-9)
        assertEquals(0L, planets.getValue("normal").marketAdjustment)
    }

    @Test
    fun `v4 saves load into v5 with no traits and part 2 not started`() {
        // 2부 이전(스키마 4) 세이브 — 특성·획득 카운터·2부 진행·발견 행성 칸이 없다
        val json = """{"snapshotSchemaVersion":4,"roomDbVersion":22,
            "user":{"coins":5000,"discoveredVariantIds":"STAR-01","missionStep":-1},
            "storyProgress":{"totalRecordsCompleted":90,"currentChapter":5,"firstT10Completed":true,"storyCompleted":true},
            "planets":[{"id":"p","type":"STAR","production":1200,"risk":30,"investment":0,"eventRate":28,
                "buyPrice":211140,"acquireTime":0,"level":10,"totalProfit":0,"lastProfitTime":0}],
            "expeditions":[{"id":"e","category":"PLANET","tier":10,"astronautIds":"a","spaceshipId":"s",
                "startTime":0,"endTime":1,"status":"COMPLETED","resourcesResult":null,"discoveredPlanetType":"STAR"}]}"""
        val snapshot = (loadSnapshot(json) as SnapshotLoad.Loaded).snapshot

        assertEquals("", snapshot.planets.single().traits)
        assertEquals(0, snapshot.user!!.traitPlanetsAcquired)
        assertEquals(0, snapshot.user!!.highFitTier2Acquired)
        assertEquals(0, snapshot.user!!.highFitTier3Acquired)
        val story = snapshot.storyProgress!!
        assertTrue("1부 완주는 유지돼 T11이 열린다", story.storyCompleted)
        assertEquals(5, story.currentChapter)
        assertEquals(false, story.part2Completed)
        assertEquals(false, story.part2IntroShown)
        assertEquals(0, story.settlementLogCursor)
        assertEquals(null, snapshot.expeditions.single().discoveredPlanetJson)
    }

    @Test
    fun `same-version migrate returns input unchanged`() {
        val v = GameSnapshot.SCHEMA_VERSION
        val json = """{"snapshotSchemaVersion":$v,"roomDbVersion":17}"""
        assertTrue(loadSnapshot(json) is SnapshotLoad.Loaded)
    }
}
