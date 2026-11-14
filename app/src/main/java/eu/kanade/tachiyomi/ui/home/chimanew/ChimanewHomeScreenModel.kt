// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.Navigator
import chimahon.novel.interactor.GetNextNovelChapters
import chimahon.novel.interactor.UpdateNovel
import chimahon.novel.source.LocalNovelFiles
import chimahon.novel.ui.detail.NovelDetailScreen
import chimahon.novel.ui.reader.NovelReaderActivity
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.presentation.more.settings.screen.chimanew.HomeRowsCustomizerScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.home.chimanew.data.AnilistCoverRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.CommunityRating
import eu.kanade.tachiyomi.ui.home.chimanew.data.DiscoveryItem
import eu.kanade.tachiyomi.ui.home.chimanew.data.DiscoveryRepository
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetAnimeCategories
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.AnimeCategory
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.history.interactor.GetAnimeHistory
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.GetNextEpisodes
import tachiyomi.domain.history.interactor.RemoveAnimeHistory
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.AnimeHistoryRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.novel.model.Novel
import tachiyomi.domain.novel.model.NovelCategory
import tachiyomi.domain.novel.model.NovelHistoryWithRelations
import tachiyomi.domain.novel.model.toSNNovel
import tachiyomi.domain.novel.repository.NovelCategoryRepository
import tachiyomi.domain.novel.repository.NovelChapterRepository
import tachiyomi.domain.novel.repository.NovelHistoryRepository
import tachiyomi.domain.novel.repository.NovelRepository
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.ConcurrentHashMap

const val MEDIA_TYPE_MANGA = 0
const val MEDIA_TYPE_ANIME = 1
const val MEDIA_TYPE_NOVEL = 2

@Immutable
data class HomeMangaItem(
    val mangaId: Long,
    val title: String,
    val coverData: Any, // MangaCover, AnimeCover, or String URL
    val highResBannerUrl: String? = null,
    val highResCoverUrl: String? = null,
    val communityRatings: List<CommunityRating> = emptyList(),
    val lastChapterNumber: Double? = null,
    val lastChapterId: Long? = null,
    val totalChapters: Long = 0,
    val readChapters: Long = 0,
    val unreadCount: Long = 0,
    val isChapterRead: Boolean = false,
    val lastPageRead: Long = 0,
    val chapterTotalPages: Long = 0,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val inLibrary: Boolean = false,
    val primaryTrackStatus: Long? = null,
    val mediaType: Int = MEDIA_TYPE_MANGA,
    val isDiscovery: Boolean = false,
    val historyId: Long? = null,
)

val HomeMangaItem.coverUrl: String?
    get() = when (val c = coverData) {
        is MangaCover -> c.ogUrl ?: c.url
        is AnimeCover -> c.ogUrl ?: c.url
        is String -> c.takeIf { it.isNotBlank() }
        else -> null
    }

val HomeMangaItem.vibrantColor: Color?
    get() = when (val c = coverData) {
        is MangaCover -> c.vibrantCoverColor?.let { Color(it) } ?: c.dominantCoverColors?.first?.let { Color(it) }
        is AnimeCover -> c.vibrantCoverColor?.let { Color(it) } ?: c.dominantCoverColors?.first?.let { Color(it) }
        else -> null
    }

@Immutable
data class HomeShelf(
    val id: String,
    val title: String,
    val items: List<HomeMangaItem>,
    val isResumeOnTap: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChimanewHomeScreenModel(
    private val preferences: ChimanewPreferences = Injekt.get(),
    // Manga
    private val getHistory: GetHistory = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val trackRepository: TrackRepository = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val anilistCoverRepository: AnilistCoverRepository = Injekt.get(),
    private val artworkWaterfallRepository: ArtworkWaterfallRepository = Injekt.get(),
    private val discoveryRepository: DiscoveryRepository = Injekt.get(),
    private val removeHistory: RemoveHistory = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    // Anime
    private val getAnimeHistory: GetAnimeHistory = Injekt.get(),
    private val getLibraryAnime: GetLibraryAnime = Injekt.get(),
    private val getAnimeCategories: GetAnimeCategories = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getAnimeTracks: GetAnimeTracks = Injekt.get(),
    private val removeAnimeHistory: RemoveAnimeHistory = Injekt.get(),
    private val animeHistoryRepository: AnimeHistoryRepository = Injekt.get(),
    // Novels
    private val novelRepository: NovelRepository = Injekt.get(),
    private val novelHistoryRepository: NovelHistoryRepository = Injekt.get(),
    private val novelCategoryRepository: NovelCategoryRepository = Injekt.get(),
    private val novelChapterRepository: NovelChapterRepository = Injekt.get(),
) : StateScreenModel<ChimanewHomeScreenModel.State>(
    cachedMediaStates[preferences.homeActiveMedia().get()] ?: State(activeMedia = preferences.homeActiveMedia().get())
) {

    companion object {
        private val cachedMediaStates = ConcurrentHashMap<Int, State>()
        val artworkRefreshTrigger = MutableStateFlow(0L)
        val bannerUpdateEvent = MutableSharedFlow<BannerUpdateEvent>(extraBufferCapacity = 8)

        data class BannerUpdateEvent(val id: Long, val bannerUrl: String?, val mediaType: Int)

        fun notifyBannerUpdated(id: Long, bannerUrl: String?, mediaType: Int) {
            bannerUpdateEvent.tryEmit(BannerUpdateEvent(id, bannerUrl, mediaType))
        }

        fun triggerRefresh() {
            cachedMediaStates.clear()
            artworkRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    private val attemptedHeroTitles = ConcurrentHashMap.newKeySet<String>()
    private val persistentHeroIdsByMedia = ConcurrentHashMap<Int, List<Long>>()

    private fun selectHeroItems(candidates: List<HomeMangaItem>, mediaType: Int = mutableState.value.activeMedia, maxCount: Int = 7): List<HomeMangaItem> {
        if (candidates.isEmpty()) return emptyList()
        val saved = persistentHeroIdsByMedia[mediaType]
        if (!saved.isNullOrEmpty()) {
            val byId = candidates.associateBy { it.mangaId }
            val matched = saved.mapNotNull { byId[it] }
            if (matched.isNotEmpty()) return matched
        }
        val selected = candidates.shuffled().take(maxCount)
        persistentHeroIdsByMedia[mediaType] = selected.map { it.mangaId }
        return selected
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val activeMedia: Int = MEDIA_TYPE_MANGA,
        val heroItems: List<HomeMangaItem> = emptyList(),
        val heroStyle: Int = 0,
        val heroArtworkSources: Set<String> = setOf("banner", "cover", "default"),
        val shelves: List<HomeShelf> = emptyList(),
        val showProgressBars: Boolean = true,
        val showUnreadBadges: Boolean = true,
        val continueReadingTapAction: Int = 1,
        val shelfTapAction: Int = 0,
    )

    fun switchMedia(mediaType: Int) {
        if (mutableState.value.activeMedia == mediaType) return
        logcat(LogPriority.INFO) { "[NUVIO_HOME] [MediaSwitcher] Switching active media to $mediaType" }
        preferences.homeActiveMedia().set(mediaType)
        val cached = cachedMediaStates[mediaType]
        if (cached != null) {
            mutableState.update { cached.copy(activeMedia = mediaType, isLoading = false) }
        } else {
            mutableState.update { it.copy(activeMedia = mediaType, isLoading = true) }
        }
    }

    fun refreshHero() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }
            persistentHeroIdsByMedia.remove(mutableState.value.activeMedia)
            discoveryRepository.invalidateCache()
            triggerRefresh()
            delay(800L)
            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    fun removeFromHistory(item: HomeMangaItem, all: Boolean) {
        screenModelScope.launchIO {
            when (item.mediaType) {
                MEDIA_TYPE_ANIME -> {
                    if (all) {
                        removeAnimeHistory.awaitAnime(listOf(item.mangaId))
                    } else {
                        val targetHistoryId = item.historyId ?: runCatching {
                            animeHistoryRepository.getHistoryByAnimeId(item.mangaId).let { list ->
                                if (item.lastChapterId != null) {
                                    list.firstOrNull { it.episodeId == item.lastChapterId }?.id
                                } else {
                                    list.firstOrNull()?.id
                                }
                            }
                        }.getOrNull()
                        if (targetHistoryId != null) {
                            removeAnimeHistory.await(listOf(targetHistoryId))
                        }
                    }
                }
                MEDIA_TYPE_NOVEL -> {
                    if (all) {
                        val chapterIds = runCatching {
                            novelChapterRepository.getChaptersByNovelId(item.mangaId).map { it.id }
                        }.getOrNull().orEmpty()
                        if (chapterIds.isNotEmpty()) {
                            novelHistoryRepository.resetHistoryByChapterIds(chapterIds)
                        }
                    } else {
                        val chapterId = item.lastChapterId
                        if (chapterId != null) {
                            novelHistoryRepository.resetHistoryByChapterIds(listOf(chapterId))
                        }
                    }
                }
                else -> {
                    // Manga
                    if (all) {
                        removeHistory.awaitManga(listOf(item.mangaId))
                    } else {
                        val targetHistoryId = item.historyId ?: runCatching {
                            historyRepository.getHistoryByMangaId(item.mangaId).let { list ->
                                if (item.lastChapterId != null) {
                                    list.firstOrNull { it.chapterId == item.lastChapterId }?.id
                                } else {
                                    list.firstOrNull()?.id
                                }
                            }
                        }.getOrNull()
                        if (targetHistoryId != null) {
                            removeHistory.await(listOf(targetHistoryId))
                        }
                    }
                }
            }
        }
    }

    private fun updateItemBannerInPlace(id: Long, bannerUrl: String?, mediaType: Int) {
        val effectiveUrl = bannerUrl?.takeIf { it.isNotBlank() }
            ?: when (mediaType) {
                MEDIA_TYPE_ANIME -> preferences.animeCustomHeroBannerUrl(id).get().takeIf { it.isNotBlank() }
                    ?: preferences.animeCustomBannerUrl(id).get().takeIf { it.isNotBlank() }
                MEDIA_TYPE_NOVEL -> preferences.novelCustomHeroBannerUrl(id).get().takeIf { it.isNotBlank() }
                    ?: preferences.novelCustomBannerUrl(id).get().takeIf { it.isNotBlank() }
                else -> preferences.mangaCustomHeroBannerUrl(id).get().takeIf { it.isNotBlank() }
                    ?: preferences.mangaCustomBannerUrl(id).get().takeIf { it.isNotBlank() }
            }

        mutableState.update { currentState ->
            if (currentState.activeMedia != mediaType) return@update currentState

            val updatedHeroItems = currentState.heroItems.map { item ->
                if (item.mangaId == id) {
                    val fallbackBanner = effectiveUrl ?: artworkWaterfallRepository.getCached(item.title, mangaId = id, mediaType = mediaType)?.bannerUrl
                    item.copy(highResBannerUrl = fallbackBanner)
                } else item
            }

            val updatedShelves = currentState.shelves.map { shelf ->
                shelf.copy(
                    items = shelf.items.map { item ->
                        if (item.mangaId == id) {
                            val fallbackBanner = effectiveUrl ?: artworkWaterfallRepository.getCached(item.title, mangaId = id, mediaType = mediaType)?.bannerUrl
                            item.copy(highResBannerUrl = fallbackBanner)
                        } else item
                    }
                )
            }

            currentState.copy(
                heroItems = updatedHeroItems,
                shelves = updatedShelves,
            )
        }
    }

    init {
        screenModelScope.launchIO {
            bannerUpdateEvent.collect { event ->
                updateItemBannerInPlace(event.id, event.bannerUrl, event.mediaType)
            }
        }
        screenModelScope.launchIO {
            preferences.categoryArtworkRulesUpdated().changes().collectLatest {
                artworkRefreshTrigger.value = System.currentTimeMillis()
            }
        }

        screenModelScope.launchIO {
            preferences.homeActiveMedia().changes()
                .distinctUntilChanged()
                .flatMapLatest { mediaType ->
                    logcat(LogPriority.INFO) { "[NUVIO_HOME] [MediaSwitcher] Stream bound for mediaType=$mediaType" }
                    when (mediaType) {
                        MEDIA_TYPE_ANIME -> buildAnimeFlow()
                        MEDIA_TYPE_NOVEL -> buildNovelFlow()
                        else -> buildMangaFlow()
                    }
                }
                .collectLatest { newState ->
                    cachedMediaStates[newState.activeMedia] = newState
                    mutableState.update { newState }
                    logcat(LogPriority.INFO) {
                        "[NUVIO_HOME] State emitted for mediaType=${newState.activeMedia}: hero=${newState.heroItems.size}, shelves=${newState.shelves.size}"
                    }

                    // Background pre-fetch waterfall for hero items to guarantee fast artwork rendering
                    if (newState.heroItems.isNotEmpty()) {
                        newState.heroItems.take(5).forEach { item ->
                            artworkWaterfallRepository.resolveArtwork(
                                rawTitle = item.title,
                                mangaId = item.mangaId,
                                mediaType = newState.activeMedia,
                            )
                        }
                    }
                }
        }
    }

    // =========================================================================
    // MANGA FLOW
    // =========================================================================
    private fun buildMangaFlow(): Flow<State> = flow {
        val dataFlow = combine(
            getHistory.subscribe("", unfinishedManga = null, unfinishedChapter = null, nonLibraryEntries = null),
            getLibraryManga.subscribe(),
            getCategories.subscribe(),
            trackRepository.getTracksAsFlow(),
            artworkRefreshTrigger,
        ) { h, l, c, t, _ -> MangaData(h, l, c, t) }.throttleLatest(1000L)

        combine(
            dataFlow,
            preferences.showHeroCarousel().changes(),
            preferences.heroSource().changes(),
            preferences.heroStyle().changes(),
            preferences.heroArtworkSources().changes(),
            preferences.heroCategories().changes(),
            preferences.heroTrackerStatuses().changes(),
            preferences.homeShelfOrder().changes(),
            preferences.showProgressBars().changes(),
            preferences.showUnreadBadges().changes(),
            preferences.continueReadingTapAction().changes(),
            preferences.shelfTapAction().changes(),
        ) { args ->
            val data = args[0] as MangaData
            val showHero = args[1] as Boolean
            val heroSource = args[2] as Int
            val heroStyle = args[3] as Int
            val heroArtworkSources = args[4] as Set<String>
            val heroCategories = args[5] as Set<String>
            val heroTrackerStatuses = args[6] as Set<String>
            val homeShelfOrder = args[7] as String
            val showProgressBars = args[8] as Boolean
            val showUnreadBadges = args[9] as Boolean
            val continueReadingTapAction = args[10] as Int
            val shelfTapAction = args[11] as Int

            val distinctHistory = data.historyList.distinctBy { it.mangaId }
            val tracksByManga = data.tracks.groupBy { it.mangaId }
            val libraryByMangaId = data.libraryList.associateBy { it.manga.id }

            fun createItem(
                mangaId: Long,
                title: String,
                coverData: MangaCover,
                highResBannerUrl: String? = null,
                highResCoverUrl: String? = null,
                lastChapterNumber: Double? = null,
                lastChapterId: Long? = null,
                totalChapters: Long = 0,
                readChapters: Long = 0,
                unreadCount: Long = 0,
                isChapterRead: Boolean = false,
                lastPageRead: Long = 0,
                genres: List<String> = emptyList(),
                description: String? = null,
                inLibrary: Boolean = false,
                primaryTrackStatus: Long? = null,
                historyId: Long? = null,
            ): HomeMangaItem {
                val cachedArt = artworkWaterfallRepository.getCached(title, mangaId = mangaId, mediaType = MEDIA_TYPE_MANGA)
                val customHeroBanner = preferences.mangaCustomHeroBannerUrl(mangaId).get().takeIf { it.isNotBlank() }
                return HomeMangaItem(
                    mangaId = mangaId,
                    title = title,
                    coverData = coverData,
                    highResBannerUrl = customHeroBanner ?: highResBannerUrl ?: cachedArt?.bannerUrl,
                    highResCoverUrl = highResCoverUrl ?: cachedArt?.posterUrl,
                    communityRatings = cachedArt?.ratings ?: emptyList(),
                    lastChapterNumber = lastChapterNumber,
                    lastChapterId = lastChapterId,
                    totalChapters = totalChapters,
                    readChapters = readChapters,
                    unreadCount = unreadCount,
                    isChapterRead = isChapterRead,
                    lastPageRead = lastPageRead,
                    genres = genres,
                    description = description,
                    inLibrary = inLibrary,
                    primaryTrackStatus = primaryTrackStatus,
                    mediaType = MEDIA_TYPE_MANGA,
                    historyId = historyId,
                )
            }

            // Continue reading list
            val continueReadingItems = distinctHistory.map { h ->
                val libManga = libraryByMangaId[h.mangaId]
                createItem(
                    mangaId = h.mangaId,
                    historyId = h.id,
                    title = h.title,
                    coverData = h.coverData,
                    lastChapterNumber = h.chapterNumber.takeIf { it >= 0 },
                    lastChapterId = h.chapterId,
                    totalChapters = h.totalCountCalculated,
                    readChapters = h.readCountCalculated,
                    unreadCount = h.unreadCount,
                    isChapterRead = h.read,
                    lastPageRead = h.lastPageRead,
                    genres = libManga?.manga?.ogGenre ?: emptyList(),
                    description = libManga?.manga?.ogDescription,
                    inLibrary = libManga != null,
                )
            }

            // Tracking lists
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

                val item = createItem(
                    mangaId = lib.manga.id,
                    title = lib.manga.title,
                    coverData = lib.manga.asMangaCover(),
                    totalChapters = lib.totalChapters,
                    readChapters = lib.readCount,
                    unreadCount = lib.unreadCount,
                    genres = lib.manga.ogGenre ?: emptyList(),
                    description = lib.manga.ogDescription,
                    inLibrary = true,
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

            // Discovery Manga and Manhwa
            val trendingMangaRaw = discoveryRepository.getTrendingManga()
            val trendingManhwaRaw = discoveryRepository.getTrendingManhwa()

            val trendingMangaItems = trendingMangaRaw.map { d ->
                val matched = data.libraryList.firstOrNull { it.manga.title.equals(d.title, ignoreCase = true) }
                d.toHomeMangaItem(matched?.manga?.id, MEDIA_TYPE_MANGA)
            }
            val trendingManhwaItems = trendingManhwaRaw.map { d ->
                val matched = data.libraryList.firstOrNull { it.manga.title.equals(d.title, ignoreCase = true) }
                d.toHomeMangaItem(matched?.manga?.id, MEDIA_TYPE_MANGA)
            }

            // Hero Items
            val heroItems = if (!showHero) {
                emptyList()
            } else when (heroSource) {
                1 -> {
                    // Categories
                    val selectedCatIds = heroCategories.mapNotNull { it.toLongOrNull() }.toSet()
                    val pool = if (selectedCatIds.isNotEmpty()) {
                        data.libraryList.filter { lib -> lib.categories.any { it in selectedCatIds } }
                    } else {
                        data.libraryList.filter { it.manga.favorite }
                    }
                    val items = pool.map { lib ->
                        createItem(
                            mangaId = lib.manga.id,
                            title = lib.manga.title,
                            coverData = lib.manga.asMangaCover(),
                            totalChapters = lib.totalChapters,
                            readChapters = lib.readCount,
                            unreadCount = lib.unreadCount,
                            genres = lib.manga.ogGenre ?: emptyList(),
                            description = lib.manga.ogDescription,
                            inLibrary = true,
                        )
                    }
                    selectHeroItems(items, mediaType = MEDIA_TYPE_MANGA)
                }
                2 -> {
                    // Trackers
                    val chosen = heroTrackerStatuses
                    val pool = mutableListOf<HomeMangaItem>()
                    if ("reading" in chosen) pool.addAll(readingList)
                    if ("plan_to_read" in chosen) pool.addAll(planToReadList)
                    if ("on_hold" in chosen) pool.addAll(onHoldList)
                    if ("completed" in chosen) pool.addAll(completedList)
                    selectHeroItems(pool.distinctBy { it.mangaId }, mediaType = MEDIA_TYPE_MANGA)
                }
                3 -> selectHeroItems(trendingMangaItems, mediaType = MEDIA_TYPE_MANGA)
                4 -> selectHeroItems(trendingManhwaItems, mediaType = MEDIA_TYPE_MANGA)
                else -> {
                    // History with library fallback
                    if (continueReadingItems.isNotEmpty()) {
                        selectHeroItems(continueReadingItems, mediaType = MEDIA_TYPE_MANGA)
                    } else {
                        val libItems = data.libraryList.map { lib ->
                            createItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                totalChapters = lib.totalChapters,
                                readChapters = lib.readCount,
                                unreadCount = lib.unreadCount,
                                genres = lib.manga.ogGenre ?: emptyList(),
                                description = lib.manga.ogDescription,
                                inLibrary = true,
                            )
                        }
                        selectHeroItems(libItems, mediaType = MEDIA_TYPE_MANGA)
                    }
                }
            }

            // Assemble Shelves
            val availableShelves = mutableMapOf<String, List<HomeShelf>>()
            if (continueReadingItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_CONTINUE_READING] = listOf(
                    HomeShelf(
                        id = "shelf_continue_reading",
                        title = "Continue Reading",
                        items = continueReadingItems,
                        isResumeOnTap = continueReadingTapAction == 1,
                    ),
                )
            }
            if (readingList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_READING] = listOf(HomeShelf("track_reading", "Reading", readingList))
            if (planToReadList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_PLAN_TO_READ] = listOf(HomeShelf("track_plan_to_read", "Plan to Read", planToReadList))
            if (onHoldList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_ON_HOLD] = listOf(HomeShelf("track_on_hold", "On Hold", onHoldList))
            if (completedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_COMPLETED] = listOf(HomeShelf("track_completed", "Completed", completedList))
            if (droppedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_DROPPED] = listOf(HomeShelf("track_dropped", "Dropped", droppedList))
            if (notTrackedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_NOT_TRACKED] = listOf(HomeShelf("track_not_tracked", "Not Tracked in Library", notTrackedList))

            // Categories
            for (cat in data.categories) {
                val catItems = data.libraryList.filter { cat.id in it.categories }
                if (catItems.isNotEmpty()) {
                    val shelf = HomeShelf(
                        id = "cat_${cat.id}",
                        title = cat.name.ifBlank { "Library" },
                        items = catItems.map { lib ->
                            createItem(
                                mangaId = lib.manga.id,
                                title = lib.manga.title,
                                coverData = lib.manga.asMangaCover(),
                                totalChapters = lib.totalChapters,
                                readChapters = lib.readCount,
                                unreadCount = lib.unreadCount,
                                genres = lib.manga.ogGenre ?: emptyList(),
                                description = lib.manga.ogDescription,
                                inLibrary = true,
                            )
                        },
                    )
                    availableShelves["${HomeRowsCustomizerScreen.CATEGORY_PREFIX}${cat.id}"] = listOf(shelf)
                }
            }

            // Recently Updated
            val updatedItems = data.libraryList
                .sortedByDescending { it.latestUpload.coerceAtLeast(it.chapterFetchedAt) }
                .map { lib ->
                    createItem(
                        mangaId = lib.manga.id,
                        title = lib.manga.title,
                        coverData = lib.manga.asMangaCover(),
                        totalChapters = lib.totalChapters,
                        readChapters = lib.readCount,
                        unreadCount = lib.unreadCount,
                        genres = lib.manga.ogGenre ?: emptyList(),
                        description = lib.manga.ogDescription,
                        inLibrary = true,
                    )
                }
            if (updatedItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_RECENTLY_ADDED] = listOf(HomeShelf("shelf_recently_updated", "Recently Updated", updatedItems))
            }

            // Discovery
            if (trendingMangaItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_TRENDING_MANGA] = listOf(
                    HomeShelf("shelf_trending_manga", "Trending Manga (AniList Discovery)", trendingMangaItems)
                )
            }
            if (trendingManhwaItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_TRENDING_MANHWA] = listOf(
                    HomeShelf("shelf_trending_manhwa", "Trending Manhwa (AniList Discovery)", trendingManhwaItems)
                )
            }

            val orderedCustomEntries = HomeRowsCustomizerScreen.parseOrder(
                raw = homeShelfOrder,
                categories = data.categories.map { it.id to it.name },
                mediaType = MEDIA_TYPE_MANGA,
            )
            val finalShelves = mutableListOf<HomeShelf>()
            for ((shelfId, isEnabled) in orderedCustomEntries) {
                if (isEnabled) {
                    availableShelves[shelfId]?.let { finalShelves.addAll(it) }
                }
            }

            State(
                isLoading = false,
                isRefreshing = mutableState.value.isRefreshing,
                activeMedia = MEDIA_TYPE_MANGA,
                heroItems = heroItems,
                heroStyle = heroStyle,
                heroArtworkSources = heroArtworkSources,
                shelves = finalShelves,
                showProgressBars = showProgressBars,
                showUnreadBadges = showUnreadBadges,
                continueReadingTapAction = continueReadingTapAction,
                shelfTapAction = shelfTapAction,
            )
        }.collect { emit(it) }
    }

    // =========================================================================
    // ANIME FLOW
    // =========================================================================
    private fun buildAnimeFlow(): Flow<State> = flow {
        val dataFlow = combine(
            getAnimeHistory.subscribe(""),
            getLibraryAnime.subscribe(),
            getAnimeCategories.subscribe(),
            getAnimeTracks.subscribe(),
            artworkRefreshTrigger,
        ) { h, l, c, t, _ -> AnimeData(h, l, c, t) }.throttleLatest(1000L)

        combine(
            dataFlow,
            preferences.showHeroCarouselAnime().changes(),
            preferences.heroSourceAnime().changes(),
            preferences.heroStyleAnime().changes(),
            preferences.heroArtworkSources().changes(),
            preferences.heroCategoriesAnime().changes(),
            preferences.heroTrackerStatusesAnime().changes(),
            preferences.homeShelfOrderAnime().changes(),
            preferences.showProgressBars().changes(),
            preferences.showUnreadBadges().changes(),
            preferences.continueReadingTapAction().changes(),
            preferences.shelfTapAction().changes(),
        ) { args ->
            val data = args[0] as AnimeData
            val showHero = args[1] as Boolean
            val heroSource = args[2] as Int
            val heroStyle = args[3] as Int
            val heroArtworkSources = args[4] as Set<String>
            val heroCategories = args[5] as Set<String>
            val heroTrackerStatuses = args[6] as Set<String>
            val homeShelfOrder = args[7] as String
            val showProgressBars = args[8] as Boolean
            val showUnreadBadges = args[9] as Boolean
            val continueReadingTapAction = args[10] as Int
            val shelfTapAction = args[11] as Int

            val distinctHistory = data.historyList.distinctBy { it.animeId }
            val tracksByAnime = data.tracks.groupBy { it.animeId }
            val libraryByAnimeId = data.libraryList.associateBy { it.anime.id }

            fun createAnimeItem(
                animeId: Long,
                title: String,
                coverData: AnimeCover,
                lastEpisodeNumber: Double? = null,
                lastEpisodeId: Long? = null,
                totalEpisodes: Long = 0,
                seenEpisodes: Long = 0,
                unseenCount: Long = 0,
                isEpisodeSeen: Boolean = false,
                genres: List<String> = emptyList(),
                description: String? = null,
                inLibrary: Boolean = false,
                historyId: Long? = null,
            ): HomeMangaItem {
                val cachedArt = artworkWaterfallRepository.getCached(title, mangaId = animeId, mediaType = MEDIA_TYPE_ANIME)
                val customHeroBanner = preferences.animeCustomHeroBannerUrl(animeId).get().takeIf { it.isNotBlank() }
                return HomeMangaItem(
                    mangaId = animeId,
                    title = title,
                    coverData = coverData,
                    highResBannerUrl = customHeroBanner ?: cachedArt?.bannerUrl,
                    highResCoverUrl = cachedArt?.posterUrl,
                    communityRatings = cachedArt?.ratings ?: emptyList(),
                    lastChapterNumber = lastEpisodeNumber,
                    lastChapterId = lastEpisodeId,
                    totalChapters = totalEpisodes,
                    readChapters = seenEpisodes,
                    unreadCount = unseenCount,
                    isChapterRead = isEpisodeSeen,
                    genres = genres,
                    description = description,
                    inLibrary = inLibrary,
                    mediaType = MEDIA_TYPE_ANIME,
                    historyId = historyId,
                )
            }

            // Continue watching
            val continueWatchingItems = distinctHistory.map { h ->
                val lib = libraryByAnimeId[h.animeId]
                createAnimeItem(
                    animeId = h.animeId,
                    historyId = h.id,
                    title = h.title,
                    coverData = h.coverData,
                    lastEpisodeNumber = h.episodeNumber.takeIf { it >= 0 },
                    lastEpisodeId = h.episodeId,
                    totalEpisodes = lib?.totalEpisodes ?: 0L,
                    seenEpisodes = lib?.seenCount ?: 0L,
                    unseenCount = lib?.unseenCount ?: 0L,
                    isEpisodeSeen = h.seen,
                    genres = lib?.anime?.genre ?: emptyList(),
                    description = lib?.anime?.description,
                    inLibrary = lib != null,
                )
            }

            // Tracking lists
            val watchingList = mutableListOf<HomeMangaItem>()
            val planToWatchList = mutableListOf<HomeMangaItem>()
            val onHoldList = mutableListOf<HomeMangaItem>()
            val completedList = mutableListOf<HomeMangaItem>()
            val droppedList = mutableListOf<HomeMangaItem>()
            val notTrackedList = mutableListOf<HomeMangaItem>()

            for (lib in data.libraryList) {
                val animeTracks = tracksByAnime[lib.anime.id]
                val activeTrack = animeTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                    ?: animeTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                    ?: animeTracks?.firstOrNull()

                val item = createAnimeItem(
                    animeId = lib.anime.id,
                    title = lib.anime.title,
                    coverData = lib.anime.asAnimeCover(),
                    totalEpisodes = lib.totalEpisodes,
                    seenEpisodes = lib.seenCount,
                    unseenCount = lib.unseenCount,
                    genres = lib.anime.genre ?: emptyList(),
                    description = lib.anime.description,
                    inLibrary = true,
                )

                if (activeTrack == null) {
                    notTrackedList.add(item)
                } else {
                    when (TrackStatus.parseTrackerStatus(trackerManager, activeTrack.trackerId, activeTrack.status)) {
                        TrackStatus.READING, TrackStatus.REPEATING -> watchingList.add(item)
                        TrackStatus.PLAN_TO_READ -> planToWatchList.add(item)
                        TrackStatus.PAUSED -> onHoldList.add(item)
                        TrackStatus.COMPLETED -> completedList.add(item)
                        TrackStatus.DROPPED -> droppedList.add(item)
                        else -> notTrackedList.add(item)
                    }
                }
            }

            // Discovery Anime
            val trendingAnimeRaw = discoveryRepository.getTrendingAnime()
            val trendingAnimeItems = trendingAnimeRaw.map { d ->
                val matched = data.libraryList.firstOrNull { it.anime.title.equals(d.title, ignoreCase = true) }
                d.toHomeMangaItem(matched?.anime?.id, MEDIA_TYPE_ANIME)
            }

            // Hero Items
            val heroItems = if (!showHero) {
                emptyList()
            } else when (heroSource) {
                1 -> {
                    // Categories
                    val selectedCatIds = heroCategories.mapNotNull { it.toLongOrNull() }.toSet()
                    val pool = if (selectedCatIds.isNotEmpty()) {
                        data.libraryList.filter { lib -> lib.categories.any { it in selectedCatIds } }
                    } else {
                        data.libraryList.filter { it.anime.favorite }
                    }
                    val items = pool.map { lib ->
                        createAnimeItem(
                            animeId = lib.anime.id,
                            title = lib.anime.title,
                            coverData = lib.anime.asAnimeCover(),
                            totalEpisodes = lib.totalEpisodes,
                            seenEpisodes = lib.seenCount,
                            unseenCount = lib.unseenCount,
                            genres = lib.anime.genre ?: emptyList(),
                            description = lib.anime.description,
                            inLibrary = true,
                        )
                    }
                    selectHeroItems(items, mediaType = MEDIA_TYPE_ANIME)
                }
                2 -> {
                    // Trackers
                    val chosen = heroTrackerStatuses
                    val pool = mutableListOf<HomeMangaItem>()
                    if ("reading" in chosen) pool.addAll(watchingList)
                    if ("plan_to_read" in chosen) pool.addAll(planToWatchList)
                    if ("on_hold" in chosen) pool.addAll(onHoldList)
                    if ("completed" in chosen) pool.addAll(completedList)
                    selectHeroItems(pool.distinctBy { it.mangaId }, mediaType = MEDIA_TYPE_ANIME)
                }
                3 -> selectHeroItems(trendingAnimeItems, mediaType = MEDIA_TYPE_ANIME)
                else -> {
                    // History with library fallback
                    if (continueWatchingItems.isNotEmpty()) {
                        selectHeroItems(continueWatchingItems, mediaType = MEDIA_TYPE_ANIME)
                    } else {
                        val libItems = data.libraryList.map { lib ->
                            createAnimeItem(
                                animeId = lib.anime.id,
                                title = lib.anime.title,
                                coverData = lib.anime.asAnimeCover(),
                                totalEpisodes = lib.totalEpisodes,
                                seenEpisodes = lib.seenCount,
                                unseenCount = lib.unseenCount,
                                genres = lib.anime.genre ?: emptyList(),
                                description = lib.anime.description,
                                inLibrary = true,
                            )
                        }
                        selectHeroItems(libItems, mediaType = MEDIA_TYPE_ANIME)
                    }
                }
            }

            // Assemble Shelves
            val availableShelves = mutableMapOf<String, List<HomeShelf>>()
            if (continueWatchingItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_CONTINUE_WATCHING] = listOf(
                    HomeShelf(
                        id = "shelf_continue_watching",
                        title = "Continue Watching",
                        items = continueWatchingItems,
                        isResumeOnTap = continueReadingTapAction == 1,
                    ),
                )
            }
            if (watchingList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_WATCHING] = listOf(HomeShelf("track_watching", "Watching", watchingList))
            if (planToWatchList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_PLAN_TO_WATCH] = listOf(HomeShelf("track_plan_to_watch", "Plan to Watch", planToWatchList))
            if (onHoldList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_ON_HOLD] = listOf(HomeShelf("track_on_hold", "On Hold", onHoldList))
            if (completedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_COMPLETED] = listOf(HomeShelf("track_completed", "Completed", completedList))
            if (droppedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_DROPPED] = listOf(HomeShelf("track_dropped", "Dropped", droppedList))
            if (notTrackedList.isNotEmpty()) availableShelves[HomeRowsCustomizerScreen.SHELF_NOT_TRACKED] = listOf(HomeShelf("track_not_tracked", "Not Tracked in Library", notTrackedList))

            // Categories
            for (cat in data.categories) {
                val catItems = data.libraryList.filter { cat.id in it.categories }
                if (catItems.isNotEmpty()) {
                    val shelf = HomeShelf(
                        id = "cat_${cat.id}",
                        title = cat.name.ifBlank { "Anime" },
                        items = catItems.map { lib ->
                            createAnimeItem(
                                animeId = lib.anime.id,
                                title = lib.anime.title,
                                coverData = lib.anime.asAnimeCover(),
                                totalEpisodes = lib.totalEpisodes,
                                seenEpisodes = lib.seenCount,
                                unseenCount = lib.unseenCount,
                                genres = lib.anime.genre ?: emptyList(),
                                description = lib.anime.description,
                                inLibrary = true,
                            )
                        },
                    )
                    availableShelves["${HomeRowsCustomizerScreen.CATEGORY_PREFIX}${cat.id}"] = listOf(shelf)
                }
            }

            // Recently Added
            val addedItems = data.libraryList
                .sortedByDescending { it.anime.dateAdded }
                .map { lib ->
                    createAnimeItem(
                        animeId = lib.anime.id,
                        title = lib.anime.title,
                        coverData = lib.anime.asAnimeCover(),
                        totalEpisodes = lib.totalEpisodes,
                        seenEpisodes = lib.seenCount,
                        unseenCount = lib.unseenCount,
                        genres = lib.anime.genre ?: emptyList(),
                        description = lib.anime.description,
                        inLibrary = true,
                    )
                }
            if (addedItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_RECENTLY_ADDED] = listOf(HomeShelf("shelf_recently_added_anime", "Recently Added", addedItems))
            }

            // Discovery
            if (trendingAnimeItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_TRENDING_ANIME] = listOf(
                    HomeShelf("shelf_trending_anime", "Trending Anime (AniList Discovery)", trendingAnimeItems)
                )
            }

            val orderedCustomEntries = HomeRowsCustomizerScreen.parseOrder(
                raw = homeShelfOrder,
                categories = data.categories.map { it.id to it.name },
                mediaType = MEDIA_TYPE_ANIME,
            )
            val finalShelves = mutableListOf<HomeShelf>()
            for ((shelfId, isEnabled) in orderedCustomEntries) {
                if (isEnabled) {
                    availableShelves[shelfId]?.let { finalShelves.addAll(it) }
                }
            }

            State(
                isLoading = false,
                isRefreshing = mutableState.value.isRefreshing,
                activeMedia = MEDIA_TYPE_ANIME,
                heroItems = heroItems,
                heroStyle = heroStyle,
                heroArtworkSources = heroArtworkSources,
                shelves = finalShelves,
                showProgressBars = showProgressBars,
                showUnreadBadges = showUnreadBadges,
                continueReadingTapAction = continueReadingTapAction,
                shelfTapAction = shelfTapAction,
            )
        }.collect { emit(it) }
    }

    // =========================================================================
    // NOVEL FLOW
    // =========================================================================
    private fun buildNovelFlow(): Flow<State> = flow {
        val dataFlow = combine(
            novelHistoryRepository.subscribeToHistoryWithRelations(""),
            novelRepository.getFavoritesAsFlow(),
            novelCategoryRepository.subscribe(),
            artworkRefreshTrigger,
        ) { h, f, c, _ -> NovelData(h, f, c) }.throttleLatest(1000L)

        combine(
            dataFlow,
            preferences.showHeroCarouselNovel().changes(),
            preferences.heroSourceNovel().changes(),
            preferences.heroStyleNovel().changes(),
            preferences.heroArtworkSources().changes(),
            preferences.heroCategoriesNovel().changes(),
            preferences.homeShelfOrderNovel().changes(),
            preferences.showProgressBars().changes(),
            preferences.showUnreadBadges().changes(),
            preferences.continueReadingTapAction().changes(),
            preferences.shelfTapAction().changes(),
        ) { args ->
            val data = args[0] as NovelData
            val showHero = args[1] as Boolean
            val heroSource = args[2] as Int
            val heroStyle = args[3] as Int
            val heroArtworkSources = args[4] as Set<String>
            val heroCategories = args[5] as Set<String>
            val homeShelfOrder = args[6] as String
            val showProgressBars = args[7] as Boolean
            val showUnreadBadges = args[8] as Boolean
            val continueReadingTapAction = args[9] as Int
            val shelfTapAction = args[10] as Int

            val distinctHistory = data.historyList.distinctBy { it.novelId }
            val favoritesByNovelId = data.favoritesList.associateBy { it.id }

            fun parseNovelGenres(genre: String?): List<String> {
                return genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            }

            fun createNovelItem(
                novelId: Long,
                title: String,
                thumbnailUrl: String?,
                lastChapterNumber: Double? = null,
                lastChapterId: Long? = null,
                genres: List<String> = emptyList(),
                description: String? = null,
                inLibrary: Boolean = false,
                historyId: Long? = null,
            ): HomeMangaItem {
                val cachedArt = artworkWaterfallRepository.getCached(title, mangaId = novelId, mediaType = MEDIA_TYPE_NOVEL)
                val customHeroBanner = preferences.novelCustomHeroBannerUrl(novelId).get().takeIf { it.isNotBlank() }
                return HomeMangaItem(
                    mangaId = novelId,
                    title = title,
                    coverData = thumbnailUrl ?: "",
                    highResBannerUrl = customHeroBanner ?: cachedArt?.bannerUrl,
                    highResCoverUrl = cachedArt?.posterUrl ?: thumbnailUrl,
                    communityRatings = cachedArt?.ratings ?: emptyList(),
                    lastChapterNumber = lastChapterNumber,
                    lastChapterId = lastChapterId,
                    genres = genres,
                    description = description,
                    inLibrary = inLibrary,
                    mediaType = MEDIA_TYPE_NOVEL,
                    historyId = historyId,
                )
            }

            // Continue reading
            val continueReadingItems = distinctHistory.map { h ->
                val fav = favoritesByNovelId[h.novelId]
                createNovelItem(
                    novelId = h.novelId,
                    historyId = h.id,
                    title = h.novelTitle,
                    thumbnailUrl = h.novelThumbnailUrl,
                    lastChapterNumber = h.chapterNumber.toDouble().takeIf { it >= 0 },
                    lastChapterId = h.chapterId,
                    genres = parseNovelGenres(fav?.genre),
                    description = fav?.description,
                    inLibrary = fav != null,
                )
            }

            // Library items
            val libraryItems = data.favoritesList.map { novel ->
                createNovelItem(
                    novelId = novel.id,
                    title = novel.title,
                    thumbnailUrl = novel.thumbnailUrl,
                    genres = parseNovelGenres(novel.genre),
                    description = novel.description,
                    inLibrary = true,
                )
            }

            // Discovery Novels
            val trendingNovelsRaw = discoveryRepository.getTrendingNovels()
            val trendingNovelsItems = trendingNovelsRaw.map { d ->
                val matched = data.favoritesList.firstOrNull { it.title.equals(d.title, ignoreCase = true) }
                d.toHomeMangaItem(matched?.id, MEDIA_TYPE_NOVEL)
            }

            // Hero Items
            val heroItems = if (!showHero) {
                emptyList()
            } else when (heroSource) {
                1 -> {
                    // Categories
                    val selectedCatIds = heroCategories.mapNotNull { it.toLongOrNull() }.toSet()
                    val pool = if (selectedCatIds.isNotEmpty()) {
                        val categoryMap = novelCategoryRepository.getCategoryIdsByNovelIds(data.favoritesList.map { it.id })
                        data.favoritesList.filter { novel ->
                            val ids = categoryMap[novel.id].orEmpty()
                            ids.any { it in selectedCatIds }
                        }
                    } else {
                        data.favoritesList
                    }
                    val items = pool.map { novel ->
                        createNovelItem(
                            novelId = novel.id,
                            title = novel.title,
                            thumbnailUrl = novel.thumbnailUrl,
                            genres = parseNovelGenres(novel.genre),
                            description = novel.description,
                            inLibrary = true,
                        )
                    }
                    selectHeroItems(items, mediaType = MEDIA_TYPE_NOVEL)
                }
                2 -> selectHeroItems(continueReadingItems, mediaType = MEDIA_TYPE_NOVEL)
                3 -> selectHeroItems(trendingNovelsItems, mediaType = MEDIA_TYPE_NOVEL)
                else -> {
                    if (continueReadingItems.isNotEmpty()) {
                        selectHeroItems(continueReadingItems, mediaType = MEDIA_TYPE_NOVEL)
                    } else {
                        selectHeroItems(libraryItems, mediaType = MEDIA_TYPE_NOVEL)
                    }
                }
            }

            // Assemble Shelves
            val availableShelves = mutableMapOf<String, List<HomeShelf>>()
            if (continueReadingItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_CONTINUE_READING] = listOf(
                    HomeShelf(
                        id = "shelf_continue_reading_novel",
                        title = "Continue Reading",
                        items = continueReadingItems,
                        isResumeOnTap = continueReadingTapAction == 1,
                    ),
                )
            }
            if (libraryItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_READING] = listOf(
                    HomeShelf("shelf_library_novels", "Reading", libraryItems)
                )
            }

            // Categories
            for (cat in data.categories) {
                val categoryMap = novelCategoryRepository.getCategoryIdsByNovelIds(data.favoritesList.map { it.id })
                val catItems = data.favoritesList.filter { novel ->
                    cat.id in categoryMap[novel.id].orEmpty()
                }
                if (catItems.isNotEmpty()) {
                    val shelf = HomeShelf(
                        id = "cat_${cat.id}",
                        title = cat.name.ifBlank { "Novels" },
                        items = catItems.map { novel ->
                            createNovelItem(
                                novelId = novel.id,
                                title = novel.title,
                                thumbnailUrl = novel.thumbnailUrl,
                                genres = parseNovelGenres(novel.genre),
                                description = novel.description,
                                inLibrary = true,
                            )
                        },
                    )
                    availableShelves["${HomeRowsCustomizerScreen.CATEGORY_PREFIX}${cat.id}"] = listOf(shelf)
                }
            }

            // Recently Added
            val addedItems = data.favoritesList.map { novel ->
                createNovelItem(
                    novelId = novel.id,
                    title = novel.title,
                    thumbnailUrl = novel.thumbnailUrl,
                    genres = parseNovelGenres(novel.genre),
                    description = novel.description,
                    inLibrary = true,
                )
            }
            if (addedItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_RECENTLY_ADDED] = listOf(
                    HomeShelf("shelf_recently_added_novels", "Recently Added", addedItems)
                )
            }

            // Discovery
            if (trendingNovelsItems.isNotEmpty()) {
                availableShelves[HomeRowsCustomizerScreen.SHELF_TRENDING_NOVELS] = listOf(
                    HomeShelf("shelf_trending_novels", "Trending Light Novels (AniList Discovery)", trendingNovelsItems)
                )
            }

            val orderedCustomEntries = HomeRowsCustomizerScreen.parseOrder(
                raw = homeShelfOrder,
                categories = data.categories.map { it.id to it.name },
                mediaType = MEDIA_TYPE_NOVEL,
            )
            val finalShelves = mutableListOf<HomeShelf>()
            for ((shelfId, isEnabled) in orderedCustomEntries) {
                if (isEnabled) {
                    availableShelves[shelfId]?.let { finalShelves.addAll(it) }
                }
            }

            State(
                isLoading = false,
                isRefreshing = mutableState.value.isRefreshing,
                activeMedia = MEDIA_TYPE_NOVEL,
                heroItems = heroItems,
                heroStyle = heroStyle,
                heroArtworkSources = heroArtworkSources,
                shelves = finalShelves,
                showProgressBars = showProgressBars,
                showUnreadBadges = showUnreadBadges,
                continueReadingTapAction = continueReadingTapAction,
                shelfTapAction = shelfTapAction,
            )
        }.collect { emit(it) }
    }

    // Navigation and Resume Helpers
    suspend fun getResumeChapter(mangaId: Long, lastChapterId: Long?): Chapter? {
        return withIOContext {
            val nextChapters = getNextChapters.await(mangaId, onlyUnread = true)
            nextChapters.firstOrNull() ?: lastChapterId?.let { getNextChapters.await(mangaId, it).firstOrNull() }
        }
    }

    suspend fun getResumeEpisode(animeId: Long, lastEpisodeId: Long?): Episode? {
        return withIOContext {
            val episodes = getNextEpisodes.await(animeId, onlyUnseen = true)
            episodes.firstOrNull() ?: lastEpisodeId?.let { getNextEpisodes.await(animeId, it, onlyUnseen = false).firstOrNull() }
        }
    }

    suspend fun getDbNovel(novelId: Long): Novel? = withIOContext {
        runCatching { novelRepository.getNovelById(novelId) }.getOrNull()
    }

    fun resumeNovel(context: Context, novelId: Long, onFallback: (Screen) -> Unit) {
        screenModelScope.launchIO {
            val novel = getDbNovel(novelId)
            if (novel == null) return@launchIO
            val localFolder = novel.localFolder
            if (localFolder != null) {
                val dir = LocalNovelFiles.ensureReadableDir(context, localFolder)
                if (dir != null) {
                    withContext(Dispatchers.Main) {
                        NovelReaderActivity.launch(context, dir, novelId)
                    }
                    return@launchIO
                }
            }
            withContext(Dispatchers.Main) {
                onFallback(NovelDetailScreen.fromDbNovel(novel))
            }
        }
    }

    fun openNovelDetails(novelId: Long, onNavigate: (Screen) -> Unit) {
        screenModelScope.launchIO {
            val novel = getDbNovel(novelId) ?: return@launchIO
            withContext(Dispatchers.Main) {
                onNavigate(NovelDetailScreen.fromDbNovel(novel))
            }
        }
    }

    fun toggleLibraryBookmark(mangaId: Long) {
        screenModelScope.launchIO {
            try {
                val manga = getManga.await(mangaId) ?: return@launchIO
                val updateManga = Injekt.get<eu.kanade.domain.manga.interactor.UpdateManga>()
                updateManga.awaitUpdateFavorite(mangaId, !manga.favorite)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to toggle library bookmark for manga $mangaId" }
            }
        }
    }

    fun updateTrackStatus(mangaId: Long, trackerId: Long, status: Long) {
        screenModelScope.launchIO {
            try {
                val track = trackRepository.getTracksByMangaId(mangaId).firstOrNull { it.trackerId == trackerId } ?: return@launchIO
                val updatedTrack = track.copy(status = status)
                trackRepository.insert(updatedTrack)
                val tracker = trackerManager.get(trackerId)
                if (tracker != null && tracker.isLoggedIn) {
                    tracker.update(updatedTrack.toDbTrack())
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to update track status for manga $mangaId" }
            }
        }
    }
}

private fun DiscoveryItem.toHomeMangaItem(libraryId: Long?, mediaType: Int): HomeMangaItem {
    return HomeMangaItem(
        mangaId = libraryId ?: -remoteId,
        title = title,
        coverData = coverUrl ?: "",
        highResBannerUrl = bannerUrl,
        highResCoverUrl = coverUrl,
        genres = genres,
        description = description,
        communityRatings = averageScore?.let {
            listOf(CommunityRating(score10 = it / 10f, score100 = it, source = "AniList"))
        } ?: emptyList(),
        inLibrary = libraryId != null,
        mediaType = mediaType,
        isDiscovery = libraryId == null,
    )
}

private data class MangaData(
    val historyList: List<HistoryWithRelations>,
    val libraryList: List<LibraryManga>,
    val categories: List<Category>,
    val tracks: List<Track>,
)

private data class AnimeData(
    val historyList: List<AnimeHistoryWithRelations>,
    val libraryList: List<LibraryAnime>,
    val categories: List<AnimeCategory>,
    val tracks: List<AnimeTrack>,
)

private data class NovelData(
    val historyList: List<NovelHistoryWithRelations>,
    val favoritesList: List<Novel>,
    val categories: List<NovelCategory>,
)

private fun <T> Flow<T>.throttleLatest(periodMillis: Long): Flow<T> = channelFlow {
    var lastEmitTime = 0L
    val mutex = Mutex()
    var latestValue: T? = null
    var emitJob: Job? = null

    collect { value ->
        mutex.withLock {
            latestValue = value
            val currentTime = SystemClock.elapsedRealtime()
            val elapsed = currentTime - lastEmitTime

            if (elapsed >= periodMillis || lastEmitTime == 0L) {
                emitJob?.cancel()
                lastEmitTime = currentTime
                send(value)
            } else if (emitJob?.isActive != true) {
                emitJob = launch {
                    delay(periodMillis - elapsed)
                    mutex.withLock {
                        latestValue?.let {
                            lastEmitTime = SystemClock.elapsedRealtime()
                            send(it)
                        }
                    }
                }
            }
        }
    }
}
