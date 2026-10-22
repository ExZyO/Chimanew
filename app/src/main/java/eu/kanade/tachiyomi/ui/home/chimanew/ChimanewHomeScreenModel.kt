// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class HomeMangaItem(
    val mangaId: Long,
    val title: String,
    val coverData: MangaCover,
    val lastChapterNumber: Double? = null,
    val lastChapterId: Long? = null,
    val totalChapters: Long = 0,
    val readChapters: Long = 0,
    val unreadCount: Long = 0,
    val genres: List<String> = emptyList(),
    val description: String? = null,
)

@Immutable
data class HomeShelf(
    val id: String,
    val title: String,
    val items: List<HomeMangaItem>,
)

@Immutable
private data class NuvioHomePrefs(
    val showHero: Boolean,
    val heroSource: Int,
    val showContinue: Boolean,
    val showUpdated: Boolean,
    val showCategoryShelves: Boolean,
    val showProgressBars: Boolean,
    val showUnreadBadges: Boolean,
)

class ChimanewHomeScreenModel(
    private val preferences: ChimanewPreferences = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<ChimanewHomeScreenModel.State>(State()) {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val heroItems: List<HomeMangaItem> = emptyList(),
        val continueReading: List<HomeMangaItem> = emptyList(),
        val recentlyUpdated: List<HomeMangaItem> = emptyList(),
        val shelves: List<HomeShelf> = emptyList(),
        val showProgressBars: Boolean = true,
        val showUnreadBadges: Boolean = true,
    )

    init {
        screenModelScope.launchIO {
            val dataFlow = combine(
                getHistory.subscribe("", unfinishedManga = null, unfinishedChapter = null, nonLibraryEntries = null),
                getLibraryManga.subscribe(),
                getCategories.subscribe(),
            ) { historyList: List<HistoryWithRelations>, libraryList: List<LibraryManga>, categories: List<Category> ->
                Triple(historyList, libraryList, categories)
            }

            val prefsFlow = combine(
                preferences.showHeroCarousel().changes(),
                preferences.heroSource().changes(),
                preferences.showContinueReading().changes(),
                preferences.showRecentlyUpdated().changes(),
                preferences.showCategoryShelves().changes(),
            ) { showHero, heroSource, showContinue, showUpdated, showCategoryShelves ->
                NuvioHomePrefs(
                    showHero = showHero,
                    heroSource = heroSource,
                    showContinue = showContinue,
                    showUpdated = showUpdated,
                    showCategoryShelves = showCategoryShelves,
                    showProgressBars = preferences.showProgressBars().get(),
                    showUnreadBadges = preferences.showUnreadBadges().get(),
                )
            }

            combine(dataFlow, prefsFlow) { (historyList, libraryList, categories), prefs ->
                val distinctHistory = historyList.distinctBy { it.mangaId }

                val continueReadingItems = if (prefs.showContinue) {
                    distinctHistory.map { h ->
                        HomeMangaItem(
                            mangaId = h.mangaId,
                            title = h.title,
                            coverData = h.coverData,
                            lastChapterNumber = h.chapterNumber.takeIf { it >= 0 },
                            lastChapterId = h.chapterId,
                            totalChapters = h.totalCountCalculated,
                            readChapters = h.readCountCalculated,
                            unreadCount = h.unreadCount,
                        )
                    }
                } else {
                    emptyList()
                }

                val heroItems = if (!prefs.showHero) {
                    emptyList()
                } else if (prefs.heroSource == 1) {
                    // Favorites only
                    libraryList
                        .filter { it.manga.favorite }
                        .take(7)
                        .map { lib ->
                            HomeMangaItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                totalChapters = lib.totalChapters,
                                readChapters = lib.readCount,
                                unreadCount = lib.unreadCount,
                                genres = lib.manga.ogGenre ?: emptyList(),
                                description = lib.manga.ogDescription,
                            )
                        }
                } else {
                    // Default: in-progress history with library fallback
                    val heroFromHistory = distinctHistory.take(7).map { h ->
                        HomeMangaItem(
                            mangaId = h.mangaId,
                            title = h.title,
                            coverData = h.coverData,
                            lastChapterNumber = h.chapterNumber.takeIf { it >= 0 },
                            lastChapterId = h.chapterId,
                            totalChapters = h.totalCountCalculated,
                            readChapters = h.readCountCalculated,
                            unreadCount = h.unreadCount,
                        )
                    }
                    if (heroFromHistory.size < 5 && libraryList.isNotEmpty()) {
                        val additional = libraryList
                            .filter { it.id !in heroFromHistory.map { item -> item.mangaId } }
                            .take(7 - heroFromHistory.size)
                            .map { lib ->
                                HomeMangaItem(
                                    mangaId = lib.manga.id,
                                    title = lib.manga.title,
                                    coverData = lib.manga.asMangaCover(),
                                    totalChapters = lib.totalChapters,
                                    readChapters = lib.readCount,
                                    unreadCount = lib.unreadCount,
                                    genres = lib.manga.ogGenre ?: emptyList(),
                                    description = lib.manga.ogDescription,
                                )
                            }
                        heroFromHistory + additional
                    } else {
                        heroFromHistory
                    }
                }

                val updatedItems = if (prefs.showUpdated) {
                    libraryList
                        .sortedByDescending { it.latestUpload.coerceAtLeast(it.chapterFetchedAt) }
                        .take(15)
                        .map { lib ->
                            HomeMangaItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                totalChapters = lib.totalChapters,
                                readChapters = lib.readCount,
                                unreadCount = lib.unreadCount,
                                genres = lib.manga.ogGenre ?: emptyList(),
                                description = lib.manga.ogDescription,
                            )
                        }
                } else {
                    emptyList()
                }

                val categoryShelves = if (prefs.showCategoryShelves) {
                    categories.mapNotNull { cat ->
                        val itemsForCat = libraryList.filter { cat.id in it.categories }
                        if (itemsForCat.isEmpty()) {
                            null
                        } else {
                            HomeShelf(
                                id = "cat_${cat.id}",
                                title = cat.name,
                                items = itemsForCat.map { lib ->
                                    HomeMangaItem(
                                        mangaId = lib.manga.id,
                                        title = lib.manga.title,
                                        coverData = lib.manga.asMangaCover(),
                                        totalChapters = lib.totalChapters,
                                        readChapters = lib.readCount,
                                        unreadCount = lib.unreadCount,
                                        genres = lib.manga.ogGenre ?: emptyList(),
                                        description = lib.manga.ogDescription,
                                    )
                                },
                            )
                        }
                    }
                } else {
                    emptyList()
                }

                State(
                    isLoading = false,
                    heroItems = heroItems,
                    continueReading = continueReadingItems,
                    recentlyUpdated = updatedItems,
                    shelves = categoryShelves,
                    showProgressBars = prefs.showProgressBars,
                    showUnreadBadges = prefs.showUnreadBadges,
                )
            }.collectLatest { newState ->
                mutableState.update { newState }
            }
        }
    }

    suspend fun getResumeChapter(mangaId: Long, lastChapterId: Long?): Chapter? {
        return withIOContext {
            if (lastChapterId != null) {
                getNextChapters.await(mangaId, lastChapterId, onlyUnread = false).firstOrNull()
            } else {
                getNextChapters.await(mangaId, onlyUnread = true).firstOrNull()
                    ?: getNextChapters.await(mangaId, onlyUnread = false).firstOrNull()
            }
        }
    }
}
