// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
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
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class HomeMangaItem(
    val mangaId: Long,
    val title: String,
    val coverData: MangaCover,
    val highResBannerUrl: String? = null,
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
private data class DataBundle(
    val historyList: List<HistoryWithRelations>,
    val libraryList: List<LibraryManga>,
    val categories: List<Category>,
    val tracks: List<Track>,
)

@Immutable
private data class NuvioHomePrefs(
    val showHero: Boolean,
    val heroSource: Int,
    val heroStyle: Int,
    val showContinue: Boolean,
    val showUpdated: Boolean,
    val showCategoryShelves: Boolean,
    val showTrackingShelves: Boolean,
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
    private val trackRepository: TrackRepository = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) : StateScreenModel<ChimanewHomeScreenModel.State>(State()) {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val heroItems: List<HomeMangaItem> = emptyList(),
        val heroStyle: Int = 0,
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
                trackRepository.getTracksAsFlow(),
            ) { historyList: List<HistoryWithRelations>, libraryList: List<LibraryManga>, categories: List<Category>, tracks: List<Track> ->
                DataBundle(historyList, libraryList, categories, tracks)
            }

            val heroPrefsFlow = combine(
                preferences.showHeroCarousel().changes(),
                preferences.heroSource().changes(),
                preferences.heroStyle().changes(),
            ) { showHero: Boolean, heroSource: Int, heroStyle: Int ->
                Triple(showHero, heroSource, heroStyle)
            }

            val shelvesPrefsFlow = combine(
                preferences.showContinueReading().changes(),
                preferences.showRecentlyUpdated().changes(),
                preferences.showCategoryShelves().changes(),
                preferences.showTrackingShelves().changes(),
            ) { showContinue: Boolean, showUpdated: Boolean, showCategoryShelves: Boolean, showTrackingShelves: Boolean ->
                listOf(showContinue, showUpdated, showCategoryShelves, showTrackingShelves)
            }

            val prefsFlow = combine(heroPrefsFlow, shelvesPrefsFlow) { (showHero, heroSource, heroStyle), shelvesPrefs ->
                NuvioHomePrefs(
                    showHero = showHero,
                    heroSource = heroSource,
                    heroStyle = heroStyle,
                    showContinue = shelvesPrefs[0],
                    showUpdated = shelvesPrefs[1],
                    showCategoryShelves = shelvesPrefs[2],
                    showTrackingShelves = shelvesPrefs[3],
                    showProgressBars = preferences.showProgressBars().get(),
                    showUnreadBadges = preferences.showUnreadBadges().get(),
                )
            }

            combine(dataFlow, prefsFlow) { data, prefs ->
                val distinctHistory = data.historyList.distinctBy { it.mangaId }
                val tracksByManga = data.tracks.groupBy { it.mangaId }

                fun getHighResBanner(mangaId: Long): String? {
                    val mangaTracks = tracksByManga[mangaId] ?: return null
                    val aniListTrack = mangaTracks.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                    return aniListTrack?.let { "https://img.anili.st/media/${it.remoteId}" }
                }

                val continueReadingItems = if (prefs.showContinue) {
                    distinctHistory.map { h ->
                        HomeMangaItem(
                            mangaId = h.mangaId,
                            title = h.title,
                            coverData = h.coverData,
                            highResBannerUrl = getHighResBanner(h.mangaId),
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
                    data.libraryList
                        .filter { it.manga.favorite }
                        .take(7)
                        .map { lib ->
                            HomeMangaItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                highResBannerUrl = getHighResBanner(lib.manga.id),
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
                            highResBannerUrl = getHighResBanner(h.mangaId),
                            lastChapterNumber = h.chapterNumber.takeIf { it >= 0 },
                            lastChapterId = h.chapterId,
                            totalChapters = h.totalCountCalculated,
                            readChapters = h.readCountCalculated,
                            unreadCount = h.unreadCount,
                        )
                    }
                    if (heroFromHistory.size < 5 && data.libraryList.isNotEmpty()) {
                        val additional = data.libraryList
                            .filter { it.id !in heroFromHistory.map { item -> item.mangaId } }
                            .take(7 - heroFromHistory.size)
                            .map { lib ->
                                HomeMangaItem(
                                    mangaId = lib.manga.id,
                                    title = lib.manga.title,
                                    coverData = lib.manga.asMangaCover(),
                                    highResBannerUrl = getHighResBanner(lib.manga.id),
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
                    data.libraryList
                        .sortedByDescending { it.latestUpload.coerceAtLeast(it.chapterFetchedAt) }
                        .take(15)
                        .map { lib ->
                            HomeMangaItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                highResBannerUrl = getHighResBanner(lib.manga.id),
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
                    data.categories.mapNotNull { cat ->
                        val itemsForCat = data.libraryList.filter { cat.id in it.categories }
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
                                        highResBannerUrl = getHighResBanner(lib.manga.id),
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

                val trackingShelves = if (prefs.showTrackingShelves && data.libraryList.isNotEmpty()) {
                    val readingList = mutableListOf<HomeMangaItem>()
                    val planToReadList = mutableListOf<HomeMangaItem>()
                    val onHoldList = mutableListOf<HomeMangaItem>()
                    val completedList = mutableListOf<HomeMangaItem>()
                    val droppedList = mutableListOf<HomeMangaItem>()
                    val notTrackedList = mutableListOf<HomeMangaItem>()

                    for (lib in data.libraryList) {
                        val mangaTracks = tracksByManga[lib.manga.id]
                        val activeTrack = mangaTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                            ?: mangaTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                            ?: mangaTracks?.firstOrNull()

                        val item = HomeMangaItem(
                            mangaId = lib.manga.id,
                            title = lib.manga.title,
                            coverData = lib.manga.asMangaCover(),
                            highResBannerUrl = getHighResBanner(lib.manga.id),
                            totalChapters = lib.totalChapters,
                            readChapters = lib.readCount,
                            unreadCount = lib.unreadCount,
                            genres = lib.manga.ogGenre ?: emptyList(),
                            description = lib.manga.ogDescription,
                        )

                        if (activeTrack == null) {
                            notTrackedList.add(item)
                        } else {
                            when (TrackStatus.parseTrackerStatus(trackerManager, activeTrack.trackerId, activeTrack.status)) {
                                TrackStatus.READING, TrackStatus.REPEATING -> readingList.add(item)
                                TrackStatus.PLAN_TO_READ -> planToReadList.add(item)
                                TrackStatus.PAUSED -> onHoldList.add(item)
                                TrackStatus.COMPLETED -> completedList.add(item)
                                TrackStatus.DROPPED -> droppedList.add(item)
                                else -> notTrackedList.add(item)
                            }
                        }
                    }

                    buildList {
                        if (readingList.isNotEmpty()) {
                            add(HomeShelf(id = "track_reading", title = "Reading", items = readingList))
                        }
                        if (planToReadList.isNotEmpty()) {
                            add(HomeShelf(id = "track_plan_to_read", title = "Plan to Read", items = planToReadList))
                        }
                        if (onHoldList.isNotEmpty()) {
                            add(HomeShelf(id = "track_on_hold", title = "On Hold", items = onHoldList))
                        }
                        if (completedList.isNotEmpty()) {
                            add(HomeShelf(id = "track_completed", title = "Completed", items = completedList))
                        }
                        if (droppedList.isNotEmpty()) {
                            add(HomeShelf(id = "track_dropped", title = "Dropped", items = droppedList))
                        }
                        if (notTrackedList.isNotEmpty()) {
                            add(HomeShelf(id = "track_not_tracked", title = "Not Tracked in Library", items = notTrackedList))
                        }
                    }
                } else {
                    emptyList()
                }

                State(
                    isLoading = false,
                    heroItems = heroItems,
                    heroStyle = prefs.heroStyle,
                    continueReading = continueReadingItems,
                    recentlyUpdated = updatedItems,
                    shelves = categoryShelves + trackingShelves,
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
