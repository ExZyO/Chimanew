// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.presentation.more.settings.screen.chimanew.HomeRowsCustomizerScreen
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import eu.kanade.tachiyomi.ui.home.chimanew.data.AnilistCoverRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.AnilistMediaArtwork
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.UnifiedArtwork
import kotlinx.coroutines.delay
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
import eu.kanade.tachiyomi.ui.home.chimanew.data.CommunityRating
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class HomeMangaItem(
    val mangaId: Long,
    val title: String,
    val coverData: MangaCover,
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
)

@Immutable
data class HomeShelf(
    val id: String,
    val title: String,
    val items: List<HomeMangaItem>,
    val isResumeOnTap: Boolean = false,
)

@Immutable
private data class DataBundle(
    val historyList: List<HistoryWithRelations>,
    val libraryList: List<LibraryManga>,
    val categories: List<Category>,
    val tracks: List<Track>,
)

@Immutable
private data class CoreHeroPrefs(
    val showHero: Boolean,
    val heroSource: Int,
    val heroStyle: Int,
    val heroRequireSelectedArtwork: Boolean,
    val heroStrictArtworkRequirements: Set<String>,
)

@Immutable
private data class HeroPrefs(
    val showHero: Boolean,
    val heroSource: Int,
    val heroStyle: Int,
    val heroRequireSelectedArtwork: Boolean,
    val heroStrictArtworkRequirements: Set<String>,
    val useAnilistArtwork: Boolean,
    val heroArtworkSources: Set<String>,
    val heroTrackerStatuses: Set<String>,
    val artworkWaterfallOrder: String,
    val artworkBannerOrder: String,
    val artworkCoverOrder: String,
)

@Immutable
private data class ShelvesPrefs(
    val showContinue: Boolean,
    val showUpdated: Boolean,
    val showCategoryShelves: Boolean,
    val showTrackingShelves: Boolean,
    val homeShelfOrder: String,
)

@Immutable
private data class CardAndHeroPrefs(
    val showProgressBars: Boolean,
    val showUnreadBadges: Boolean,
    val continueReadingTapAction: Int,
    val shelfTapAction: Int,
    val heroCategories: Set<String>,
)

@Immutable
private data class NuvioHomePrefs(
    val showHero: Boolean,
    val heroSource: Int,
    val heroStyle: Int,
    val heroRequireSelectedArtwork: Boolean,
    val heroStrictArtworkRequirements: Set<String>,
    val useAnilistArtwork: Boolean,
    val heroArtworkSources: Set<String>,
    val heroCategories: Set<String>,
    val heroTrackerStatuses: Set<String>,
    val artworkWaterfallOrder: String,
    val artworkBannerOrder: String,
    val artworkCoverOrder: String,
    val showContinue: Boolean,
    val showUpdated: Boolean,
    val showCategoryShelves: Boolean,
    val showTrackingShelves: Boolean,
    val homeShelfOrder: String,
    val showProgressBars: Boolean,
    val showUnreadBadges: Boolean,
    val continueReadingTapAction: Int,
    val shelfTapAction: Int,
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
    private val anilistCoverRepository: AnilistCoverRepository = Injekt.get(),
    private val artworkWaterfallRepository: ArtworkWaterfallRepository = Injekt.get(),
) : StateScreenModel<ChimanewHomeScreenModel.State>(State()) {

    private val artworkRefreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(0L)
    // EaZy Nuvio+ / Chimanew: prevent infinite re-query loops for hero art
    private val attemptedHeroTitles = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var lastArtworkConfigKey: String? = null

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val heroItems: List<HomeMangaItem> = emptyList(),
        val heroStyle: Int = 0,
        val heroArtworkSources: Set<String> = setOf("banner", "cover", "default"),
        val shelves: List<HomeShelf> = emptyList(),
        val showProgressBars: Boolean = true,
        val showUnreadBadges: Boolean = true,
        val continueReadingTapAction: Int = 1,
        val shelfTapAction: Int = 0,
    )

    init {
        screenModelScope.launchIO {
            val dataFlow = combine(
                getHistory.subscribe("", unfinishedManga = null, unfinishedChapter = null, nonLibraryEntries = null),
                getLibraryManga.subscribe(),
                getCategories.subscribe(),
                trackRepository.getTracksAsFlow(),
                artworkRefreshTrigger,
            ) { historyList: List<HistoryWithRelations>, libraryList: List<LibraryManga>, categories: List<Category>, tracks: List<Track>, _ ->
                DataBundle(historyList, libraryList, categories, tracks)
            }

            val heroPrefsFlow = combine(
                combine(
                    preferences.showHeroCarousel().changes(),
                    preferences.heroSource().changes(),
                    preferences.heroStyle().changes(),
                    preferences.heroRequireSelectedArtwork().changes(),
                    preferences.heroStrictArtworkRequirements().changes(),
                ) { show, source, style, requireArt, strictReqs -> CoreHeroPrefs(show, source, style, requireArt, strictReqs) },
                preferences.useAnilistArtwork().changes(),
                preferences.heroArtworkSources().changes(),
                preferences.heroTrackerStatuses().changes(),
                combine(
                    preferences.artworkWaterfallOrder().changes(),
                    preferences.artworkBannerOrder().changes(),
                    preferences.artworkCoverOrder().changes(),
                ) { waterfall, banner, cover -> Triple(waterfall, banner, cover) },
            ) { core, useAnilist, artworkSources, trackerStatuses, orders ->
                HeroPrefs(
                    showHero = core.showHero,
                    heroSource = core.heroSource,
                    heroStyle = core.heroStyle,
                    heroRequireSelectedArtwork = core.heroRequireSelectedArtwork,
                    heroStrictArtworkRequirements = core.heroStrictArtworkRequirements,
                    useAnilistArtwork = useAnilist,
                    heroArtworkSources = artworkSources,
                    heroTrackerStatuses = trackerStatuses,
                    artworkWaterfallOrder = orders.first,
                    artworkBannerOrder = orders.second,
                    artworkCoverOrder = orders.third,
                )
            }

            val shelvesPrefsFlow = combine(
                preferences.showContinueReading().changes(),
                preferences.showRecentlyUpdated().changes(),
                preferences.showCategoryShelves().changes(),
                preferences.showTrackingShelves().changes(),
                preferences.homeShelfOrder().changes(),
            ) { showContinue: Boolean, showUpdated: Boolean, showCategoryShelves: Boolean, showTrackingShelves: Boolean, homeShelfOrder: String ->
                ShelvesPrefs(showContinue, showUpdated, showCategoryShelves, showTrackingShelves, homeShelfOrder)
            }

            val cardPrefsFlow = combine(
                preferences.showProgressBars().changes(),
                preferences.showUnreadBadges().changes(),
                preferences.continueReadingTapAction().changes(),
                preferences.shelfTapAction().changes(),
                preferences.heroCategories().changes(),
            ) { showProgress: Boolean, showUnread: Boolean, continueTap: Int, shelfTap: Int, heroCats: Set<String> ->
                CardAndHeroPrefs(showProgress, showUnread, continueTap, shelfTap, heroCats)
            }

            val prefsFlow = combine(heroPrefsFlow, shelvesPrefsFlow, cardPrefsFlow) { heroPrefs, shelvesPrefs, cardPrefs ->
                NuvioHomePrefs(
                    showHero = heroPrefs.showHero,
                    heroSource = heroPrefs.heroSource,
                    heroStyle = heroPrefs.heroStyle,
                    heroRequireSelectedArtwork = heroPrefs.heroRequireSelectedArtwork,
                    heroStrictArtworkRequirements = heroPrefs.heroStrictArtworkRequirements,
                    useAnilistArtwork = heroPrefs.useAnilistArtwork,
                    heroArtworkSources = heroPrefs.heroArtworkSources,
                    heroCategories = cardPrefs.heroCategories,
                    heroTrackerStatuses = heroPrefs.heroTrackerStatuses,
                    artworkWaterfallOrder = heroPrefs.artworkWaterfallOrder,
                    artworkBannerOrder = heroPrefs.artworkBannerOrder,
                    artworkCoverOrder = heroPrefs.artworkCoverOrder,
                    showContinue = shelvesPrefs.showContinue,
                    showUpdated = shelvesPrefs.showUpdated,
                    showCategoryShelves = shelvesPrefs.showCategoryShelves,
                    showTrackingShelves = shelvesPrefs.showTrackingShelves,
                    homeShelfOrder = shelvesPrefs.homeShelfOrder,
                    showProgressBars = cardPrefs.showProgressBars,
                    showUnreadBadges = cardPrefs.showUnreadBadges,
                    continueReadingTapAction = cardPrefs.continueReadingTapAction,
                    shelfTapAction = cardPrefs.shelfTapAction,
                )
            }

            combine(dataFlow, prefsFlow) { data, prefs ->
                val currentConfigKey = "${prefs.artworkWaterfallOrder}|${prefs.artworkBannerOrder}|${prefs.artworkCoverOrder}|${prefs.heroArtworkSources}|${prefs.heroRequireSelectedArtwork}|${prefs.heroStrictArtworkRequirements}"
                if (lastArtworkConfigKey != null && lastArtworkConfigKey != currentConfigKey) {
                    attemptedHeroTitles.clear()
                }
                lastArtworkConfigKey = currentConfigKey

                ChimanewDiagnostics.log(DiagnosticCategory.HERO, "Settings") {
                    "Hero Config: strict=${prefs.heroRequireSelectedArtwork}, reqs=${prefs.heroStrictArtworkRequirements}, sources=${prefs.heroArtworkSources}, style=${prefs.heroStyle}"
                }
                ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Settings") {
                    val bannerOrder = eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository.parsePriorityOrder(
                        preferences.artworkBannerOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
                        listOf("NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "KITSU"),
                    )
                    val coverOrder = eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository.parsePriorityOrder(
                        preferences.artworkCoverOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
                        listOf("MANGADEX", "BOOKWALKER", "BANGUMI", "ANILIST", "NAVER", "KUAIKAN", "KITSU", "DEFAULT"),
                    )
                    "Artwork Priority: Banners=$bannerOrder, Covers=$coverOrder"
                }

                val distinctHistory = data.historyList.distinctBy { it.mangaId }
                val tracksByManga = data.tracks.groupBy { it.mangaId }

                // Gather AniList and MAL IDs for batch artwork query
                val aniListRemoteIds = if (prefs.useAnilistArtwork) {
                    data.tracks.filter { it.trackerId == TrackerManager.ANILIST }.map { it.remoteId }
                } else {
                    emptyList()
                }

                val malRemoteIds = if (prefs.useAnilistArtwork) {
                    data.tracks.filter { it.trackerId == trackerManager.myAnimeList.id }.map { it.remoteId }
                } else {
                    emptyList()
                }

                if (aniListRemoteIds.isNotEmpty()) {
                    anilistCoverRepository.fetchBatch(aniListRemoteIds)
                }

                if (malRemoteIds.isNotEmpty()) {
                    anilistCoverRepository.fetchBatchByMalIds(malRemoteIds)
                }

                val libraryByMangaId = data.libraryList.associateBy { it.manga.id }

                fun getArtwork(mangaId: Long, title: String): UnifiedArtwork? {
                    if (!prefs.useAnilistArtwork) return null
                    val defaultCoverUrl = libraryByMangaId[mangaId]?.manga?.thumbnailUrl
                    var cachedUnified = artworkWaterfallRepository.getCached(title, defaultCoverUrl = defaultCoverUrl)
                    if (cachedUnified == null) {
                        val libManga = libraryByMangaId[mangaId]
                        val og = libManga?.manga?.ogTitle
                        if (!og.isNullOrBlank() && og != title) {
                            cachedUnified = artworkWaterfallRepository.getCached(og, defaultCoverUrl = defaultCoverUrl)
                        }
                    }
                    if (cachedUnified != null) return cachedUnified

                    val mangaTracks = tracksByManga[mangaId]
                    val aniListTrack = mangaTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                    val malTrack = mangaTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                    val cachedAniList = if (aniListTrack != null && aniListTrack.remoteId > 0) {
                        anilistCoverRepository.getCached(aniListTrack.remoteId)
                    } else if (malTrack != null && malTrack.remoteId > 0) {
                        anilistCoverRepository.getCachedByMalId(malTrack.remoteId)
                    } else {
                        anilistCoverRepository.getCachedByTitle(title)
                    }
                    if (cachedAniList != null) {
                        val alArtwork = UnifiedArtwork(
                            bannerUrl = cachedAniList.bannerUrl,
                            posterUrl = cachedAniList.extraLargeCoverUrl,
                            allAvailableBanners = listOfNotNull(cachedAniList.bannerUrl),
                            allAvailableCovers = listOfNotNull(
                                cachedAniList.extraLargeCoverUrl?.let {
                                    eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkCoverOption(url = it, source = "AniList", subtitle = "HD Poster")
                                },
                            ),
                            bannerOptions = listOfNotNull(
                                cachedAniList.bannerUrl?.let { eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkBannerOption(it, "ANILIST") },
                            ),
                            ratings = listOfNotNull(
                                run {
                                    val s10 = cachedAniList.score10 ?: cachedAniList.score100?.let { it / 10f }
                                    val s100 = cachedAniList.score100 ?: cachedAniList.score10?.let { (it * 10).toInt() }
                                    if (s10 != null && s100 != null) {
                                        CommunityRating(
                                            score10 = s10,
                                            score100 = s100,
                                            source = "AniList",
                                        )
                                    } else null
                                },
                            ),
                        )
                        val effective = artworkWaterfallRepository.getEffectiveArtwork(alArtwork, defaultCoverUrl = defaultCoverUrl)
                        artworkWaterfallRepository.putCached(title, alArtwork)
                        return effective
                    }
                    return null
                }

                fun createHomeItem(
                    mangaId: Long,
                    title: String,
                    coverData: MangaCover,
                    totalChapters: Long = 0,
                    readChapters: Long = 0,
                    unreadCount: Long = 0,
                    lastChapterNumber: Double? = null,
                    lastChapterId: Long? = null,
                    isChapterRead: Boolean = false,
                    lastPageRead: Long = 0,
                    chapterTotalPages: Long = 0,
                    genres: List<String> = emptyList(),
                    description: String? = null,
                ): HomeMangaItem {
                    val artwork = getArtwork(mangaId, title)
                    val inLib = libraryByMangaId.containsKey(mangaId)
                    val mangaTracks = tracksByManga[mangaId]
                    val primaryStatus = mangaTracks?.firstOrNull()?.status
                    return HomeMangaItem(
                        mangaId = mangaId,
                        title = title,
                        coverData = coverData,
                        highResBannerUrl = artwork?.bannerUrl,
                        highResCoverUrl = artwork?.posterUrl,
                        communityRatings = artwork?.ratings ?: emptyList(),
                        lastChapterNumber = lastChapterNumber,
                        lastChapterId = lastChapterId,
                        totalChapters = totalChapters,
                        readChapters = readChapters,
                        unreadCount = unreadCount,
                        isChapterRead = isChapterRead,
                        lastPageRead = lastPageRead,
                        chapterTotalPages = chapterTotalPages,
                        genres = genres,
                        description = description,
                        inLibrary = inLib,
                        primaryTrackStatus = primaryStatus,
                    )
                }

                val continueReadingItems = if (prefs.showContinue) {
                    distinctHistory.map { h ->
                        val libManga = libraryByMangaId[h.mangaId]
                        createHomeItem(
                            mangaId = h.mangaId,
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
                        )
                    }
                } else {
                    emptyList()
                }

                // Pre-calculate tracker lists so both Hero and Shelves can use them
                val readingList = mutableListOf<HomeMangaItem>()
                val planToReadList = mutableListOf<HomeMangaItem>()
                val onHoldList = mutableListOf<HomeMangaItem>()
                val completedList = mutableListOf<HomeMangaItem>()
                val droppedList = mutableListOf<HomeMangaItem>()
                val notTrackedList = mutableListOf<HomeMangaItem>()

                if ((prefs.showTrackingShelves || prefs.heroSource == 2) && data.libraryList.isNotEmpty()) {
                    for (lib in data.libraryList) {
                        val mangaTracks = tracksByManga[lib.manga.id]
                        val activeTrack = mangaTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                            ?: mangaTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                            ?: mangaTracks?.firstOrNull()

                        val item = createHomeItem(
                            mangaId = lib.manga.id,
                            title = lib.manga.title,
                            coverData = lib.manga.asMangaCover(),
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
                    ChimanewDiagnostics.log(DiagnosticCategory.SHELVES, "Trackers") {
                        "Grouped library trackers: Reading=${readingList.size}, PlanToRead=${planToReadList.size}, OnHold=${onHoldList.size}, Completed=${completedList.size}, Dropped=${droppedList.size}, NotTracked=${notTrackedList.size}"
                    }
                }

                fun hasRequiredHeroArtwork(mangaId: Long, title: String, heroStyle: Int): Boolean {
                    val artwork = getArtwork(mangaId, title)
                    val hasBanner = artwork != null && !artwork.bannerUrl.isNullOrBlank()
                    val hasCover = (artwork != null && !artwork.posterUrl.isNullOrBlank()) || ("default" in prefs.heroArtworkSources)

                    val enforceBanner = "banner" in prefs.heroStrictArtworkRequirements
                    val enforceCover = "cover" in prefs.heroStrictArtworkRequirements

                    val bannerPass = if (enforceBanner) hasBanner else true
                    val coverPass = if (enforceCover && heroStyle != 1) hasCover else true

                    val pass = if (!enforceBanner && !enforceCover) {
                        hasBanner || hasCover
                    } else {
                        bannerPass && coverPass
                    }

                    ChimanewDiagnostics.log(DiagnosticCategory.HERO, "Candidate") {
                        val status = if (pass) "[PASS]" else "[FAIL]"
                        "$status '$title' (id=$mangaId): hasBanner=$hasBanner, hasCover=$hasCover, reqs=${prefs.heroStrictArtworkRequirements}"
                    }

                    return pass
                }

                val heroItems = if (!prefs.showHero) {
                    emptyList()
                } else if (prefs.heroSource == 1) {
                    // Selected Categories
                    val selectedCatIds = prefs.heroCategories.mapNotNull { it.toLongOrNull() }.toSet()
                    val candidateList = if (selectedCatIds.isNotEmpty()) {
                        data.libraryList.filter { lib -> lib.categories.any { it in selectedCatIds } }
                    } else {
                        data.libraryList.filter { it.manga.favorite }
                    }
                    val filteredCandidates = if (prefs.heroRequireSelectedArtwork) {
                        candidateList.filter { lib ->
                            val mTitle = lib.manga.ogTitle ?: lib.manga.title
                            hasRequiredHeroArtwork(lib.manga.id, mTitle, prefs.heroStyle)
                        }
                    } else {
                        candidateList
                    }
                    filteredCandidates
                        .shuffled()
                        .take(7)
                        .map { lib ->
                            createHomeItem(
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
                } else if (prefs.heroSource == 2) {
                    // Selected Tracker Statuses
                    val chosenStatuses = prefs.heroTrackerStatuses
                    val trackerCandidates = mutableListOf<HomeMangaItem>()
                    if ("reading" in chosenStatuses) trackerCandidates.addAll(readingList)
                    if ("plan_to_read" in chosenStatuses) trackerCandidates.addAll(planToReadList)
                    if ("on_hold" in chosenStatuses) trackerCandidates.addAll(onHoldList)
                    if ("completed" in chosenStatuses) trackerCandidates.addAll(completedList)
                    if ("dropped" in chosenStatuses) trackerCandidates.addAll(droppedList)

                    val candidateList = if (trackerCandidates.isNotEmpty()) {
                        trackerCandidates.distinctBy { it.mangaId }
                    } else {
                        readingList.ifEmpty {
                            data.libraryList.map { lib ->
                                createHomeItem(
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
                        }
                    }
                    val filteredCandidates = if (prefs.heroRequireSelectedArtwork) {
                        candidateList.filter { item ->
                            hasRequiredHeroArtwork(item.mangaId, item.title, prefs.heroStyle)
                        }
                    } else {
                        candidateList
                    }
                    filteredCandidates.shuffled().take(7)
                } else {
                    // Default: in-progress history with library fallback
                    val eligibleHistory = if (prefs.heroRequireSelectedArtwork) {
                        distinctHistory.filter { h ->
                            hasRequiredHeroArtwork(h.mangaId, h.title, prefs.heroStyle)
                        }
                    } else {
                        distinctHistory
                    }
                    val heroFromHistory = eligibleHistory.take(7).map { h ->
                        val libManga = libraryByMangaId[h.mangaId]
                        createHomeItem(
                            mangaId = h.mangaId,
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
                        )
                    }
                    if (heroFromHistory.size < 5 && data.libraryList.isNotEmpty()) {
                        val additional = data.libraryList
                            .filter { lib ->
                                lib.id !in heroFromHistory.map { item -> item.mangaId } &&
                                (!prefs.heroRequireSelectedArtwork || hasRequiredHeroArtwork(lib.manga.id, lib.manga.ogTitle ?: lib.manga.title, prefs.heroStyle))
                            }
                            .sortedByDescending { it.unreadCount.coerceAtLeast(0L) }
                            .take(7 - heroFromHistory.size)
                            .map { lib ->
                                createHomeItem(
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

                if (prefs.useAnilistArtwork) {
                    val activeBannerOrder = artworkWaterfallRepository.getActiveBannerOrder()
                    val activeCoverOrder = artworkWaterfallRepository.getActiveCoverOrder()

                    val missingHeroArt = heroItems.filter { item ->
                        val cached = artworkWaterfallRepository.getCached(item.title)
                        val needsBanner = activeBannerOrder.isNotEmpty() && (cached == null || cached.bannerUrl.isNullOrBlank())
                        val needsCover = activeCoverOrder.isNotEmpty() && (cached == null || cached.posterUrl.isNullOrBlank())
                        (cached == null || needsBanner || needsCover) && attemptedHeroTitles.add(item.title)
                    }
                    val missingLibraryArt = data.libraryList.filter { lib ->
                        val mTitle = lib.manga.ogTitle ?: lib.manga.title
                        val cached = artworkWaterfallRepository.getCached(mTitle)
                        val needsBanner = activeBannerOrder.isNotEmpty() && (cached == null || cached.bannerUrl.isNullOrBlank())
                        val needsCover = activeCoverOrder.isNotEmpty() && (cached == null || cached.posterUrl.isNullOrBlank())
                        (cached == null || needsBanner || needsCover) && attemptedHeroTitles.add(mTitle)
                    }
                    if (missingHeroArt.isNotEmpty() || missingLibraryArt.isNotEmpty()) {
                        screenModelScope.launchIO {
                            var anyUpdated = false
                            for (item in missingHeroArt) {
                                val mangaTracks = tracksByManga[item.mangaId]
                                val aniListTrack = mangaTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                                val malTrack = mangaTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                                val kitsuTrack = mangaTracks?.firstOrNull { it.trackerId == trackerManager.kitsu.id }
                                val art = artworkWaterfallRepository.resolveArtwork(
                                    rawTitle = item.title,
                                    mangaId = item.mangaId,
                                    defaultCoverUrl = item.coverData.ogUrl,
                                    anilistRemoteId = aniListTrack?.remoteId?.takeIf { it > 0 },
                                    kitsuRemoteId = kitsuTrack?.remoteId?.takeIf { it > 0 },
                                    malRemoteId = malTrack?.remoteId?.takeIf { it > 0 },
                                )
                                if (!art.bannerUrl.isNullOrBlank() || !art.posterUrl.isNullOrBlank()) {
                                    anyUpdated = true
                                }
                                delay(120L)
                            }
                            if (anyUpdated) {
                                artworkRefreshTrigger.value = System.currentTimeMillis()
                            }

                            // Proactively warm library covers in background
                            for (lib in missingLibraryArt) {
                                val mTitle = lib.manga.ogTitle ?: lib.manga.title
                                val mTracks = tracksByManga[lib.manga.id]
                                val alTrack = mTracks?.firstOrNull { it.trackerId == TrackerManager.ANILIST }
                                val malTrack = mTracks?.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
                                val kitsuTrack = mTracks?.firstOrNull { it.trackerId == trackerManager.kitsu.id }
                                artworkWaterfallRepository.resolveArtwork(
                                    rawTitle = mTitle,
                                    mangaId = lib.manga.id,
                                    defaultCoverUrl = lib.manga.thumbnailUrl,
                                    anilistRemoteId = alTrack?.remoteId?.takeIf { it > 0 },
                                    kitsuRemoteId = kitsuTrack?.remoteId?.takeIf { it > 0 },
                                    malRemoteId = malTrack?.remoteId?.takeIf { it > 0 },
                                )
                                delay(80L)
                            }
                        }
                    }
                }

                val updatedItems = if (prefs.showUpdated) {
                    data.libraryList
                        .sortedByDescending { it.latestUpload.coerceAtLeast(it.chapterFetchedAt) }
                        .take(15)
                        .map { lib ->
                            createHomeItem(
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
                    data.categories.mapNotNull { cat ->
                        val itemsForCat = data.libraryList.filter { cat.id in it.categories }
                        if (itemsForCat.isEmpty()) {
                            null
                        } else {
                            HomeShelf(
                                id = "cat_${cat.id}",
                                title = cat.name.ifBlank { "Library" },
                                items = itemsForCat.map { lib ->
                                    createHomeItem(
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

                // Assemble shelves strictly according to user custom order & visibility
                val availableShelvesMap = mutableMapOf<String, List<HomeShelf>>()

                // 1. Continue Reading
                if (continueReadingItems.isNotEmpty() && prefs.showContinue) {
                    availableShelvesMap[HomeRowsCustomizerScreen.SHELF_CONTINUE_READING] = listOf(
                        HomeShelf(
                            id = "shelf_continue_reading",
                            title = "Continue Reading",
                            items = continueReadingItems,
                            isResumeOnTap = prefs.continueReadingTapAction == 1,
                        ),
                    )
                }

                // 2. Tracking shelves
                if (prefs.showTrackingShelves && data.libraryList.isNotEmpty()) {
                    if (readingList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_READING] = listOf(
                            HomeShelf(id = "track_reading", title = "Reading", items = readingList),
                        )
                    }
                    if (planToReadList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_PLAN_TO_READ] = listOf(
                            HomeShelf(id = "track_plan_to_read", title = "Plan to Read", items = planToReadList),
                        )
                    }
                    if (onHoldList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_ON_HOLD] = listOf(
                            HomeShelf(id = "track_on_hold", title = "On Hold", items = onHoldList),
                        )
                    }
                    if (completedList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_COMPLETED] = listOf(
                            HomeShelf(id = "track_completed", title = "Completed", items = completedList),
                        )
                    }
                    if (droppedList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_DROPPED] = listOf(
                            HomeShelf(id = "track_dropped", title = "Dropped", items = droppedList),
                        )
                    }
                    if (notTrackedList.isNotEmpty()) {
                        availableShelvesMap[HomeRowsCustomizerScreen.SHELF_NOT_TRACKED] = listOf(
                            HomeShelf(id = "track_not_tracked", title = "Not Tracked in Library", items = notTrackedList),
                        )
                    }
                }

                // 3. Category shelves
                if (prefs.showCategoryShelves && categoryShelves.isNotEmpty()) {
                    availableShelvesMap[HomeRowsCustomizerScreen.SHELF_CATEGORIES] = categoryShelves
                    for (cat in data.categories) {
                        val itemsForCat = data.libraryList.filter { cat.id in it.categories }
                        if (itemsForCat.isNotEmpty()) {
                            val shelf = HomeShelf(
                                id = "cat_${cat.id}",
                                title = cat.name.ifBlank { "Library" },
                                items = itemsForCat.map { lib ->
                                    createHomeItem(
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
                            availableShelvesMap["${HomeRowsCustomizerScreen.CATEGORY_PREFIX}${cat.id}"] = listOf(shelf)
                        }
                    }
                }

                // 4. Recently updated
                if (updatedItems.isNotEmpty() && prefs.showUpdated) {
                    availableShelvesMap[HomeRowsCustomizerScreen.SHELF_RECENTLY_ADDED] = listOf(
                        HomeShelf(
                            id = "shelf_recently_updated",
                            title = "Recently Updated",
                            items = updatedItems,
                        ),
                    )
                }

                val orderedCustomEntries = HomeRowsCustomizerScreen.parseOrder(prefs.homeShelfOrder, data.categories)
                val finalShelves = mutableListOf<HomeShelf>()
                for ((shelfId, isEnabled) in orderedCustomEntries) {
                    if (isEnabled) {
                        availableShelvesMap[shelfId]?.let { finalShelves.addAll(it) }
                    }
                }

                State(
                    isLoading = false,
                    heroItems = heroItems,
                    heroStyle = prefs.heroStyle,
                    heroArtworkSources = prefs.heroArtworkSources,
                    shelves = finalShelves,
                    showProgressBars = prefs.showProgressBars,
                    showUnreadBadges = prefs.showUnreadBadges,
                    continueReadingTapAction = prefs.continueReadingTapAction,
                    shelfTapAction = prefs.shelfTapAction,
                )
            }.collectLatest { newState ->
                mutableState.update { newState }

                // EaZy Nuvio+ / Chimanew: Pre-resolve waterfall for hero items to ensure 100% visual continuity with Details
                if (newState.heroItems.isNotEmpty()) {
                    newState.heroItems.take(7).forEach { item ->
                        artworkWaterfallRepository.resolveArtwork(
                            rawTitle = item.title,
                            mangaId = item.mangaId,
                        )
                    }
                }
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

    fun updateMangaTrackStatus(mangaId: Long, trackStatus: TrackStatus) {
        screenModelScope.launchIO {
            try {
                val mangaTracks = trackRepository.getTracksByMangaId(mangaId)
                if (mangaTracks.isNotEmpty()) {
                    mangaTracks.forEach { track ->
                        val tracker = trackerManager.get(track.trackerId)
                        if (tracker != null && tracker.isLoggedIn) {
                            val newStatus = when (track.trackerId) {
                                trackerManager.aniList.id -> when (trackStatus) {
                                    TrackStatus.READING -> 1L
                                    TrackStatus.PLAN_TO_READ -> 2L
                                    TrackStatus.COMPLETED -> 3L
                                    TrackStatus.PAUSED -> 4L
                                    TrackStatus.DROPPED -> 5L
                                    TrackStatus.REPEATING -> 6L
                                    else -> 1L
                                }
                                trackerManager.myAnimeList.id -> when (trackStatus) {
                                    TrackStatus.READING -> 1L
                                    TrackStatus.COMPLETED -> 2L
                                    TrackStatus.PAUSED -> 3L
                                    TrackStatus.DROPPED -> 4L
                                    TrackStatus.PLAN_TO_READ -> 6L
                                    TrackStatus.REPEATING -> 7L
                                    else -> 1L
                                }
                                trackerManager.kitsu.id -> when (trackStatus) {
                                    TrackStatus.READING -> 1L
                                    TrackStatus.PLAN_TO_READ -> 2L
                                    TrackStatus.COMPLETED -> 3L
                                    TrackStatus.PAUSED -> 4L
                                    TrackStatus.DROPPED -> 5L
                                    else -> 1L
                                }
                                else -> 1L
                            }
                            val updatedTrack = track.copy(status = newStatus)
                            try {
                                tracker.setRemoteStatus(updatedTrack.toDbTrack(), newStatus)
                            } catch (_: Exception) {}
                            trackRepository.insert(updatedTrack)
                        }
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to update track status for manga $mangaId" }
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
}
