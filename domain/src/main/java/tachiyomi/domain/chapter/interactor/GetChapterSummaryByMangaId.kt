package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.ChapterSummary
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetChapterSummaryByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long): ChapterSummary? {
        return try {
            chapterRepository.getChapterSummaryByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }
}
