package com.doge.simulator.domain.model

// 탐사 일지 "이야기" 탭과 막힘 안내 — 지금까지 본 이야기를 장별로 다시 엮는다.
// 탐사 기록이 아니라 진행 상태(지금 장 + 그 장에서 본 기록 수)로 만든다: 클라우드 세이브는 최근 기록만 옮겨서
// (GameSnapshot.HISTORY_CAP) 기기를 바꾼 플레이어는 앞 장 기록이 없을 수 있는데, 장 이야기는 순서대로만
// 나오므로 진행 상태만 알면 본 문장을 그대로 되살릴 수 있다. 막힘 문장·정착지 일상 기록은 이야기 줄기가 아니라 뺀다
object StoryReader {

    data class Chapter(
        val number: Int,
        val title: String,
        val lines: List<String>,
        // 장 마무리 문장, 5장은 1부 완주 문장, 10장은 2부 완주 문장 — 아직 못 봤으면 null
        val ending: String?
    ) {
        val part: Int get() = if (number <= StoryProgress.PART1_LAST_CHAPTER) 1 else 2
        val isPartEnding: Boolean get() = number == StoryProgress.PART1_LAST_CHAPTER || number == StoryProgress.LAST_CHAPTER
    }

    // 1부 완주 직후엔 진행 상태가 아직 5장이지만, 다음 기록부터 6장이다 — 화면에선 6장에 있는 것으로 본다
    fun currentChapter(progress: StoryProgress): Int =
        if (progress.currentChapter == StoryProgress.PART1_LAST_CHAPTER && progress.part1Completed)
            StoryProgress.PART2_FIRST_CHAPTER else progress.currentChapter

    // [seenInCurrentChapter] = 지금 장에서 본 일반 기록 수(장 마무리·완주 기록 제외)
    fun chapters(progress: StoryProgress, seenInCurrentChapter: Int): List<Chapter> {
        val current = currentChapter(progress)
        return (1..current).mapNotNull { number ->
            val pool = StoryContent.recordTitlePool[number].orEmpty()
            val lines = if (number < current) pool else pool.take(seenInCurrentChapter.coerceAtMost(pool.size))
            val ending = when {
                number == StoryProgress.PART1_LAST_CHAPTER -> StoryContent.storyEndingTitle.takeIf { progress.part1Completed }
                number == StoryProgress.LAST_CHAPTER -> StoryContent.part2EndingTitle.takeIf { progress.part2Completed }
                number < current -> StoryContent.chapterEndingTitles[number]
                else -> null
            }
            if (lines.isEmpty() && ending == null) null
            else Chapter(number, StoryProgress.chapterTitle(number), lines, ending)
        }
    }

    // 지금 장 이야기를 다 봤는데 다음 장 조건을 못 채웠을 때만 안내를 돌려준다. 이야기가 흐르는 동안엔 null —
    // 안내가 스토리보다 눈에 띄면 안 되고, 정말 막혔을 때만 할 일이 보이면 된다
    fun stalledHint(progress: StoryProgress, seenInCurrentChapter: Int): StoryContent.NextStoryHint? {
        if (progress.part2Completed) return null
        val current = currentChapter(progress)
        val chapterProgress = progress.copy(currentChapter = current)
        val poolSize = StoryContent.recordTitlePool[current]?.size ?: return null
        if (seenInCurrentChapter < poolSize || chapterProgress.isCurrentChapterExitMet) return null
        return StoryContent.nextStoryHints[current]
    }
}
