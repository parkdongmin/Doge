package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 탐사 일지 "이야기" 탭·막힘 안내 — 진행 상태만으로 본 이야기를 되살리는지
class StoryReaderTest {

    @Test
    fun `earlier chapters are complete and the current chapter shows only what was seen`() {
        val chapters = StoryReader.chapters(StoryProgress(currentChapter = 3), seenInCurrentChapter = 2)
        assertEquals(listOf(1, 2, 3), chapters.map { it.number })
        assertEquals(StoryContent.recordTitlePool.getValue(1), chapters[0].lines)
        assertEquals(StoryContent.chapterEndingTitles.getValue(1), chapters[0].ending)
        assertEquals(StoryContent.recordTitlePool.getValue(3).take(2), chapters[2].lines)
        assertNull(chapters[2].ending)
    }

    @Test
    fun `part 1 ending closes chapter 5 and part 2 starts right after`() {
        val progress = StoryProgress(currentChapter = 5, storyCompleted = true)
        val chapters = StoryReader.chapters(progress, seenInCurrentChapter = 0)
        assertEquals(5, chapters.last().number)
        assertEquals(StoryContent.storyEndingTitle, chapters.last().ending)
        assertEquals(6, StoryReader.currentChapter(progress))
    }

    @Test
    fun `hint shows only when the chapter story is done and the condition is not met`() {
        val pool = StoryContent.recordTitlePool.getValue(7).size
        val waiting = StoryProgress(currentChapter = 7, storyCompleted = true)
        assertNull("이야기가 남아 있으면 안 보임", StoryReader.stalledHint(waiting, pool - 1))
        assertEquals(StoryContent.nextStoryHints.getValue(7), StoryReader.stalledHint(waiting, pool))
        assertNull("조건을 채웠으면 안 보임", StoryReader.stalledHint(waiting.copy(firstT11Completed = true), pool))
        assertNull("6장은 조건이 없어 안 보임", StoryReader.stalledHint(StoryProgress(currentChapter = 6, storyCompleted = true), 99))
        assertNull("2부 완주 뒤엔 안 보임", StoryReader.stalledHint(waiting.copy(currentChapter = 10, part2Completed = true), 99))
    }
}
