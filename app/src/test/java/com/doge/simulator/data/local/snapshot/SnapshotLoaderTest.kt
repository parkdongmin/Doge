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
    fun `same-version migrate returns input unchanged`() {
        val v = GameSnapshot.SCHEMA_VERSION
        val json = """{"snapshotSchemaVersion":$v,"roomDbVersion":17}"""
        assertTrue(loadSnapshot(json) is SnapshotLoad.Loaded)
    }
}
