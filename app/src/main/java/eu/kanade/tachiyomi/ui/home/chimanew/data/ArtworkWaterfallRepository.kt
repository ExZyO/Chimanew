// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import android.app.Application
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.track.repository.TrackRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PersistedRating(
    val score10: Float,
    val score100: Int,
    val source: String,
)

@Serializable
data class PersistedBannerOption(
    val url: String,
    val source: String,
)

@Serializable
data class PersistedCoverOption(
    val url: String,
    val source: String,
    val subtitle: String? = null,
)

@Serializable
data class PersistedArtworkEntry(
    val bannerUrl: String? = null,
    val posterUrl: String? = null,
    val allBanners: List<String> = emptyList(),
    val allCovers: List<PersistedCoverOption> = emptyList(),
    val bannerOptions: List<PersistedBannerOption> = emptyList(),
    val ratings: List<PersistedRating> = emptyList(),
    val queriedProviders: List<String> = emptyList(),
)

data class CommunityRating(
    val score10: Float,
    val score100: Int,
    val source: String,
)

data class ArtworkBannerOption(
    val url: String,
    val source: String,
)

data class ArtworkCoverOption(
    val url: String,
    val source: String,
    val subtitle: String? = null,
)

data class UnifiedArtwork(
    val bannerUrl: String? = null,
    val posterUrl: String? = null,
    val allAvailableBanners: List<String> = emptyList(),
    val allAvailableCovers: List<ArtworkCoverOption> = emptyList(),
    val bannerOptions: List<ArtworkBannerOption> = emptyList(),
    val ratings: List<CommunityRating> = emptyList(),
    val queriedProviders: Set<String> = emptySet(),
)

class ArtworkWaterfallRepository(
    private val anilistRepo: AnilistCoverRepository = Injekt.get(),
    private val kitsuRepo: KitsuCoverRepository = Injekt.get(),
    private val mangadexRepo: MangadexCoverRepository = Injekt.get(),
    private val preferences: ChimanewPreferences = Injekt.get(),
    private val trackRepository: TrackRepository = Injekt.get(),
    private val json: Json = Injekt.get(),
    private val context: Application = Injekt.get(),
    private val naverRepo: NaverWebtoonCoverRepository = Injekt.get(),
    private val kuaikanRepo: KuaikanCoverRepository = Injekt.get(),
    private val bookwalkerRepo: BookWalkerCoverRepository = Injekt.get(),
    private val bangumiRepo: BangumiCoverRepository = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, UnifiedArtwork>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val cacheFile by lazy { File(context.filesDir, "chimanew_artwork_waterfall_cache.json") }
    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
        prettyPrint = false
    }
    private var saveJob: Job? = null

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                if (content.isNotBlank()) {
                    val persistedMap = jsonHelper.decodeFromString<Map<String, PersistedArtworkEntry>>(content)
                    for ((title, entry) in persistedMap) {
                        val bannersWithOptions = if (entry.bannerOptions.isNotEmpty()) {
                            entry.bannerOptions.map { ArtworkBannerOption(it.url, it.source) }
                        } else {
                            entry.allBanners.map { url ->
                                val inferredSource = when {
                                    url.contains("kitsu", ignoreCase = true) -> "KITSU"
                                    url.contains("pstatic", ignoreCase = true) || url.contains("naver", ignoreCase = true) -> "NAVER"
                                    url.contains("kkmh", ignoreCase = true) || url.contains("kuaikan", ignoreCase = true) -> "KUAIKAN"
                                    url.contains("bookwalker", ignoreCase = true) -> "BOOKWALKER"
                                    url.contains("bgm.tv", ignoreCase = true) || url.contains("bangumi", ignoreCase = true) -> "BANGUMI"
                                    url.contains("anilist", ignoreCase = true) -> "ANILIST"
                                    else -> "ANILIST"
                                }
                                ArtworkBannerOption(url, inferredSource)
                            }
                        }
                        // Purge any BookWalker portrait covers erroneously saved as banners
                        val cleanBannerOptions = bannersWithOptions.filterNot {
                            it.source.equals("BOOKWALKER", ignoreCase = true) || it.url.contains("bookwalker", ignoreCase = true)
                        }
                        val cleanAllBanners = (entry.allBanners.ifEmpty { bannersWithOptions.map { it.url } }).filterNot {
                            it.contains("bookwalker", ignoreCase = true)
                        }
                        val loadedQueried = if (entry.queriedProviders.isNotEmpty()) {
                            entry.queriedProviders.toSet()
                        } else {
                            val inferred = mutableSetOf<String>()
                            if (bannersWithOptions.any { it.source.equals("ANILIST", true) } || entry.allCovers.any { it.source.equals("AniList", true) }) inferred.add("ANILIST")
                            if (bannersWithOptions.any { it.source.equals("NAVER", true) } || entry.allCovers.any { it.source.equals("Naver", true) }) inferred.add("NAVER")
                            if (bannersWithOptions.any { it.source.equals("KUAIKAN", true) } || entry.allCovers.any { it.source.equals("Kuaikan", true) }) inferred.add("KUAIKAN")
                            if (bannersWithOptions.any { it.source.equals("BANGUMI", true) } || entry.allCovers.any { it.source.equals("Bangumi", true) }) inferred.add("BANGUMI")
                            if (bannersWithOptions.any { it.source.equals("KITSU", true) } || entry.allCovers.any { it.source.equals("Kitsu", true) }) inferred.add("KITSU")
                            if (entry.allCovers.any { it.source.equals("BookWalker", true) }) inferred.add("BOOKWALKER")
                            if (entry.allCovers.any { it.source.equals("MangaDex", true) }) inferred.add("MANGADEX")
                            inferred
                        }
                        cache[title] = UnifiedArtwork(
                            bannerUrl = null,
                            posterUrl = null,
                            allAvailableBanners = cleanAllBanners,
                            allAvailableCovers = entry.allCovers.map { ArtworkCoverOption(it.url, it.source, it.subtitle) },
                            bannerOptions = cleanBannerOptions,
                            ratings = entry.ratings.map { CommunityRating(it.score10, it.score100, it.source) },
                            queriedProviders = loadedQueried,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logcat(throwable = e) { "Failed to load persistent artwork cache from disk" }
        }
    }

    private fun scheduleSaveToDisk() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500L) // Debounce writes
            try {
                val snapshot = cache.mapValues { (_, value) ->
                    PersistedArtworkEntry(
                        bannerUrl = value.bannerUrl,
                        posterUrl = value.posterUrl,
                        allBanners = value.allAvailableBanners,
                        allCovers = value.allAvailableCovers.map { PersistedCoverOption(it.url, it.source, it.subtitle) },
                        bannerOptions = value.bannerOptions.map { PersistedBannerOption(it.url, it.source) },
                        ratings = value.ratings.map { PersistedRating(it.score10, it.score100, it.source) },
                        queriedProviders = value.queriedProviders.toList(),
                    )
                }
                val content = jsonHelper.encodeToString(snapshot)
                val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                tempFile.writeText(content)
                if (tempFile.exists()) {
                    if (cacheFile.exists()) {
                        cacheFile.delete()
                    }
                    tempFile.renameTo(cacheFile)
                }
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to save persistent artwork cache to disk" }
            }
        }
    }

    fun isCached(title: String): Boolean = cache.containsKey(cleanTitle(title))

    fun getEffectiveArtwork(raw: UnifiedArtwork, defaultCoverUrl: String? = null, mangaId: Long? = null): UnifiedArtwork {
        val excludedBanners = if (mangaId != null) preferences.mangaExcludedBannerSources(mangaId).get() else emptySet()
        val excludedCovers = if (mangaId != null) preferences.mangaExcludedCoverSources(mangaId).get() else emptySet()

        val bannerOrder = parsePriorityOrder(
            preferences.artworkBannerOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
            allowed = listOf("NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "KITSU"),
        ).filterNot { it in excludedBanners }

        val coverOrder = parsePriorityOrder(
            preferences.artworkCoverOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
            allowed = listOf("MANGADEX", "BOOKWALKER", "BANGUMI", "ANILIST", "NAVER", "KUAIKAN", "KITSU", "DEFAULT"),
        ).filterNot { it in excludedCovers }

        val defaultCover = defaultCoverUrl?.takeIf { it.isNotBlank() }
            ?: raw.allAvailableCovers.firstOrNull { it.source.equals("Default", ignoreCase = true) }?.url

        var effectiveBanner: String? = null
        if (!excludedBanners.contains("ALL")) {
            for (provider in bannerOrder) {
                val banner = raw.bannerOptions.firstOrNull { it.source.equals(provider, ignoreCase = true) }?.url
                    ?: when (provider) {
                        "NAVER" -> raw.allAvailableBanners.firstOrNull { it.contains("pstatic", ignoreCase = true) || it.contains("naver", ignoreCase = true) }
                        "KUAIKAN" -> raw.allAvailableBanners.firstOrNull { it.contains("kkmh", ignoreCase = true) || it.contains("kuaikan", ignoreCase = true) }
                        "BANGUMI" -> raw.allAvailableBanners.firstOrNull { it.contains("bgm.tv", ignoreCase = true) || it.contains("bangumi", ignoreCase = true) }
                        "ANILIST" -> raw.allAvailableBanners.firstOrNull { it.contains("anilist", ignoreCase = true) }
                        "KITSU" -> raw.allAvailableBanners.firstOrNull { it.contains("kitsu", ignoreCase = true) }
                        else -> null
                    }
                if (!banner.isNullOrBlank() && !banner.contains("bookwalker", ignoreCase = true)) {
                    effectiveBanner = banner
                    break
                }
            }
        }

        var effectivePoster: String? = null
        if (!excludedCovers.contains("ALL")) {
            for (provider in coverOrder) {
                val cover = when (provider) {
                    "DEFAULT" -> defaultCover
                    else -> {
                        raw.allAvailableCovers.firstOrNull { it.source.equals(provider, ignoreCase = true) }?.url
                            ?: when (provider) {
                                "BOOKWALKER" -> raw.allAvailableCovers.firstOrNull { it.url.contains("bookwalker", ignoreCase = true) }?.url
                                "BANGUMI" -> raw.allAvailableCovers.firstOrNull { it.url.contains("bgm.tv", ignoreCase = true) || it.url.contains("bangumi", ignoreCase = true) }?.url
                                "NAVER" -> raw.allAvailableCovers.firstOrNull { it.url.contains("pstatic", ignoreCase = true) || it.url.contains("naver", ignoreCase = true) }?.url
                                "KUAIKAN" -> raw.allAvailableCovers.firstOrNull { it.url.contains("kkmh", ignoreCase = true) || it.url.contains("kuaikan", ignoreCase = true) }?.url
                                "MANGADEX" -> raw.allAvailableCovers.firstOrNull { it.url.contains("mangadex", ignoreCase = true) }?.url
                                "ANILIST" -> raw.allAvailableCovers.firstOrNull { it.url.contains("anilist", ignoreCase = true) }?.url
                                "KITSU" -> raw.allAvailableCovers.firstOrNull { it.url.contains("kitsu", ignoreCase = true) }?.url
                                else -> null
                            }
                    }
                }
                if (!cover.isNullOrBlank()) {
                    effectivePoster = cover
                    break
                }
            }
        }

        if (effectivePoster.isNullOrBlank() && !defaultCover.isNullOrBlank()) {
            effectivePoster = defaultCover
        }

        return raw.copy(
            bannerUrl = effectiveBanner,
            posterUrl = effectivePoster,
        )
    }

    fun getCached(title: String, defaultCoverUrl: String? = null, mangaId: Long? = null): UnifiedArtwork? {
        val raw = cache[cleanTitle(title)] ?: return null
        val effective = getEffectiveArtwork(raw, defaultCoverUrl, mangaId)
        ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Cache") {
            "Memory cache HIT for '$title': banner=${effective?.bannerUrl}, poster=${effective?.posterUrl}"
        }
        return effective
    }

    fun putCached(title: String, artwork: UnifiedArtwork) {
        val clean = cleanTitle(title)
        if (clean.isNotBlank()) {
            cache[clean] = artwork
            scheduleSaveToDisk()
        }
    }

    fun clearCache() {
        cache.clear()
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (_: Exception) {}
        anilistRepo.clearCache()
        ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Cache") {
            "Cleared entire persistent artwork waterfall cache and AniList disk cache"
        }
    }

    fun checkAndHandlePriorityChange(): Boolean {
        val currentHash = "${getActiveBannerOrder().joinToString(",")}|${getActiveCoverOrder().joinToString(",")}"
        val savedHash = preferences.artworkPriorityOrderHash().get()
        if (savedHash.isNotBlank() && savedHash != currentHash) {
            preferences.artworkPriorityOrderHash().set(currentHash)
            ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Waterfall") {
                "Priority order changed. Marking artwork to re-evaluate according to new hierarchy."
            }
            return true
        } else if (savedHash.isBlank()) {
            preferences.artworkPriorityOrderHash().set(currentHash)
        }
        return false
    }

    fun getActiveBannerOrder(): List<String> = parsePriorityOrder(
        preferences.artworkBannerOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
        allowed = listOf("NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "KITSU"),
    )

    fun getActiveCoverOrder(): List<String> = parsePriorityOrder(
        preferences.artworkCoverOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
        allowed = listOf("MANGADEX", "BOOKWALKER", "BANGUMI", "ANILIST", "NAVER", "KUAIKAN", "KITSU", "DEFAULT"),
    )

    suspend fun resolveArtwork(
        rawTitle: String,
        anilistRemoteId: Long? = null,
        kitsuRemoteId: Long? = null,
        malRemoteId: Long? = null,
        mangaId: Long? = null,
        defaultCoverUrl: String? = null,
        forceRefresh: Boolean = false,
    ): UnifiedArtwork = withContext(Dispatchers.IO) {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return@withContext UnifiedArtwork()

        if (forceRefresh) {
            cache.remove(title)
        }

        val excludedBanners = if (mangaId != null) preferences.mangaExcludedBannerSources(mangaId).get() else emptySet()
        val excludedCovers = if (mangaId != null) preferences.mangaExcludedCoverSources(mangaId).get() else emptySet()

        // If ALL sources excluded for both banner and cover, return default immediately
        if (excludedBanners.contains("ALL") && excludedCovers.contains("ALL")) {
            return@withContext UnifiedArtwork(
                bannerUrl = null,
                posterUrl = defaultCoverUrl,
                allAvailableBanners = emptyList(),
                allAvailableCovers = if (!defaultCoverUrl.isNullOrBlank()) {
                    listOf(ArtworkCoverOption(url = defaultCoverUrl, source = "Default", subtitle = "Source Cover"))
                } else emptyList(),
                bannerOptions = emptyList(),
                ratings = emptyList(),
                queriedProviders = setOf("ALL"),
            )
        }

        val bannerOrder = getActiveBannerOrder().filterNot { it in excludedBanners }
        val coverOrder = getActiveCoverOrder().filterNot { it in excludedCovers }

        val isNaverRequested = (!excludedBanners.contains("ALL") && "NAVER" in bannerOrder) ||
                               (!excludedCovers.contains("ALL") && "NAVER" in coverOrder)
        val isKuaikanRequested = (!excludedBanners.contains("ALL") && "KUAIKAN" in bannerOrder) ||
                                 (!excludedCovers.contains("ALL") && "KUAIKAN" in coverOrder)
        val isBangumiRequested = (!excludedBanners.contains("ALL") && "BANGUMI" in bannerOrder) ||
                                 (!excludedCovers.contains("ALL") && "BANGUMI" in coverOrder)
        val isBookWalkerRequested = !excludedCovers.contains("ALL") && "BOOKWALKER" in coverOrder
        val isKitsuRequested = (!excludedBanners.contains("ALL") && "KITSU" in bannerOrder) ||
                               (!excludedCovers.contains("ALL") && "KITSU" in coverOrder)
        val isMangaDexRequested = !excludedCovers.contains("ALL") && "MANGADEX" in coverOrder
        val isAniListRequested = (!excludedBanners.contains("ALL") && "ANILIST" in bannerOrder) ||
                                 (!excludedCovers.contains("ALL") && "ANILIST" in coverOrder)
        val needsAsianBridge = isNaverRequested || isKuaikanRequested || isBangumiRequested || isBookWalkerRequested

        val requestedProviders = mutableSetOf<String>()
        if (isNaverRequested) requestedProviders.add("NAVER")
        if (isKuaikanRequested) requestedProviders.add("KUAIKAN")
        if (isBangumiRequested) requestedProviders.add("BANGUMI")
        if (isBookWalkerRequested) requestedProviders.add("BOOKWALKER")
        if (isKitsuRequested) requestedProviders.add("KITSU")
        if (isMangaDexRequested) requestedProviders.add("MANGADEX")
        if (isAniListRequested) requestedProviders.add("ANILIST")

        val cachedEntry = cache[title]
        if (cachedEntry != null && !forceRefresh) {
            val missingProviders = requestedProviders - cachedEntry.queriedProviders
            if (missingProviders.isEmpty()) {
                val cached = getEffectiveArtwork(cachedEntry, defaultCoverUrl, mangaId)
                ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Waterfall") {
                    "Resolved from cache for '$title': banner=${cached.bannerUrl}, poster=${cached.posterUrl}"
                }
                return@withContext cached
            }
        }

        val missingProviders = if (cachedEntry != null) {
            requestedProviders - cachedEntry.queriedProviders
        } else {
            requestedProviders
        }

        ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Waterfall") {
            "Starting network discovery for '$rawTitle' (clean='$title', missing=$missingProviders, AL=$anilistRemoteId, MAL=$malRemoteId, Kitsu=$kitsuRemoteId)"
        }

        var effectiveAniListId = anilistRemoteId
        var effectiveKitsuId = kitsuRemoteId
        var effectiveMalId = malRemoteId

        if (mangaId != null && (effectiveAniListId == null || effectiveMalId == null || effectiveKitsuId == null)) {
            try {
                val tracks = trackRepository.getTracksByMangaId(mangaId)
                if (effectiveAniListId == null) {
                    effectiveAniListId = tracks.firstOrNull { it.trackerId == 2L }?.remoteId?.takeIf { it > 0 }
                }
                if (effectiveMalId == null) {
                    effectiveMalId = tracks.firstOrNull { it.trackerId == 1L }?.remoteId?.takeIf { it > 0 }
                }
                if (effectiveKitsuId == null) {
                    effectiveKitsuId = tracks.firstOrNull { it.trackerId == 3L }?.remoteId?.takeIf { it > 0 }
                }
            } catch (_: Exception) {}
        }

        val banners = mutableListOf<String>()
        val bannerOptions = mutableListOf<ArtworkBannerOption>()
        val covers = mutableListOf<ArtworkCoverOption>()
        val ratings = mutableListOf<CommunityRating>()

        // 1. ANILIST (Primary metadata & Rosetta Stone bridge for native Hangul/Hanzi names)
        val shouldQueryAniList = ("ANILIST" in missingProviders) || (needsAsianBridge && "ANILIST" !in (cachedEntry?.queriedProviders ?: emptySet()))
        var alArt = if (shouldQueryAniList) {
            var art = if (effectiveAniListId != null && effectiveAniListId > 0) {
                anilistRepo.getCached(effectiveAniListId) ?: anilistRepo.fetchBatch(listOf(effectiveAniListId))[effectiveAniListId]
            } else null

            if ((art == null || (art.bannerUrl == null && art.extraLargeCoverUrl == null)) && effectiveMalId != null && effectiveMalId > 0) {
                art = anilistRepo.getCachedByMalId(effectiveMalId) ?: anilistRepo.fetchByMalId(effectiveMalId)
            }

            if (art == null || (art.bannerUrl == null && art.extraLargeCoverUrl == null)) {
                art = anilistRepo.getCachedByTitle(title) ?: anilistRepo.fetchByTitle(title)
            }

            if (art != null && art.countryOfOrigin == null) {
                val refreshed = if (effectiveAniListId != null && effectiveAniListId > 0) {
                    anilistRepo.fetchBatch(listOf(effectiveAniListId))[effectiveAniListId]
                } else if (effectiveMalId != null && effectiveMalId > 0) {
                    anilistRepo.fetchByMalId(effectiveMalId)
                } else {
                    anilistRepo.fetchByTitle(title)
                }
                if (refreshed != null) art = refreshed
            }
            art
        } else {
            if (effectiveAniListId != null && effectiveAniListId > 0) {
                anilistRepo.getCached(effectiveAniListId)
            } else null
        }

        // Only register AniList artwork if AniList was requested by the user
        if (isAniListRequested && "ANILIST" in missingProviders) {
            alArt?.bannerUrl?.let {
                if (it.isNotBlank()) {
                    banners.add(it)
                    bannerOptions.add(ArtworkBannerOption(it, "ANILIST"))
                }
            }
            alArt?.extraLargeCoverUrl?.let {
                if (it.isNotBlank()) {
                    covers.add(ArtworkCoverOption(url = it, source = "AniList", subtitle = "HD Poster"))
                }
            }
            if (alArt?.score10 != null && alArt.score100 != null) {
                ratings.add(CommunityRating(score10 = alArt.score10, score100 = alArt.score100, source = "AniList"))
            }
        }

        // 2. NAVER WEBTOON (Native Korean Manhwa & LINE Webtoon Global)
        if (isNaverRequested && "NAVER" in missingProviders) {
            val isNotJapanese = alArt?.countryOfOrigin != "JP" && !containsJapanese(title)
            val koreanQuery = listOfNotNull(
                alArt?.nativeTitle?.takeIf { containsHangul(it) },
                alArt?.synonyms?.firstOrNull { containsHangul(it) },
                title.takeIf { containsHangul(it) },
            ).firstOrNull() ?: if (isNotJapanese) title else null

            if (koreanQuery != null) {
                val naverArt = naverRepo.getCached(koreanQuery) ?: naverRepo.fetchArtwork(koreanQuery)
                naverArt?.bannerUrl?.let {
                    if (it.isNotBlank()) {
                        banners.add(it)
                        bannerOptions.add(ArtworkBannerOption(it, "NAVER"))
                    }
                }
                naverArt?.coverUrl?.let {
                    if (it.isNotBlank()) {
                        covers.add(ArtworkCoverOption(url = it, source = "Naver", subtitle = "Official Korean Art"))
                    }
                }
            }
        }

        // 3. KUAIKAN MANHUA (Native Chinese Manhua)
        if (isKuaikanRequested && "KUAIKAN" in missingProviders) {
            val isChinese = alArt?.countryOfOrigin == "CN" ||
                containsHanzi(title) ||
                (alArt?.nativeTitle != null && containsHanzi(alArt.nativeTitle))

            val chineseQuery = listOfNotNull(
                alArt?.nativeTitle?.takeIf { containsHanzi(it) },
                alArt?.synonyms?.firstOrNull { containsHanzi(it) },
                title.takeIf { containsHanzi(it) },
            ).firstOrNull() ?: if (isChinese) title else null

            if (chineseQuery != null) {
                val kkArt = kuaikanRepo.getCached(chineseQuery) ?: kuaikanRepo.fetchArtwork(chineseQuery)
                kkArt?.bannerUrl?.let {
                    if (it.isNotBlank()) {
                        banners.add(it)
                        bannerOptions.add(ArtworkBannerOption(it, "KUAIKAN"))
                    }
                }
                kkArt?.coverUrl?.let {
                    if (it.isNotBlank()) {
                        covers.add(ArtworkCoverOption(url = it, source = "Kuaikan", subtitle = "Official Chinese Art"))
                    }
                }
            }
        }

        // 4. JAPANESE ARTWORK PROVIDERS (BookWalker Japan & Bangumi)
        if ((isBookWalkerRequested && "BOOKWALKER" in missingProviders) || (isBangumiRequested && "BANGUMI" in missingProviders)) {
            val isJapanese = alArt?.countryOfOrigin == "JP" ||
                containsJapanese(title) ||
                (alArt?.nativeTitle != null && containsJapanese(alArt.nativeTitle))

            val japaneseQuery = listOfNotNull(
                alArt?.nativeTitle?.takeIf { containsJapanese(it) },
                alArt?.synonyms?.firstOrNull { containsJapanese(it) },
                title.takeIf { containsJapanese(it) },
                alArt?.romajiTitle?.takeIf { !containsHangul(it) },
                title.takeIf { !containsHangul(it) },
            ).firstOrNull() ?: if (isJapanese) title else null

            if (japaneseQuery != null) {
                // BookWalker Japan (Official tankōbon book covers only - NEVER pass Hangul!)
                if (isBookWalkerRequested && "BOOKWALKER" in missingProviders && !containsHangul(japaneseQuery)) {
                    val bwArt = bookwalkerRepo.getCached(japaneseQuery) ?: bookwalkerRepo.fetchArtwork(japaneseQuery)
                    bwArt?.coverUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "BookWalker", subtitle = "Official Tankōbon Art"))
                        }
                    }
                }

                // Bangumi (bgm.tv - Official Series Art & Anime Adaptation Banners)
                if (isBangumiRequested && "BANGUMI" in missingProviders) {
                    val bgmArt = bangumiRepo.getCached(japaneseQuery) ?: bangumiRepo.fetchArtwork(japaneseQuery)
                    bgmArt?.coverUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "Bangumi", subtitle = "Official Series Art"))
                        }
                    }
                    bgmArt?.bannerUrl?.let {
                        if (it.isNotBlank() && it != bgmArt.coverUrl) {
                            banners.add(it)
                            bannerOptions.add(ArtworkBannerOption(it, "BANGUMI"))
                        }
                    }
                    if (bgmArt?.score10 != null && bgmArt.score100 != null) {
                        ratings.add(CommunityRating(score10 = bgmArt.score10, score100 = bgmArt.score100, source = "Bangumi"))
                    }
                }
            }
        }

        // 5. KITSU
        if (isKitsuRequested && "KITSU" in missingProviders) {
            val kitsuArt = if (effectiveKitsuId != null && effectiveKitsuId > 0) {
                kitsuRepo.getCached(effectiveKitsuId) ?: kitsuRepo.fetchByKitsuId(effectiveKitsuId)
            } else {
                kitsuRepo.getCachedByTitle(title) ?: kitsuRepo.fetchByTitle(title)
            }

            kitsuArt?.bannerUrl?.let {
                if (it.isNotBlank()) {
                    banners.add(it)
                    bannerOptions.add(ArtworkBannerOption(it, "KITSU"))
                }
            }
            kitsuArt?.posterUrl?.let {
                if (it.isNotBlank()) {
                    covers.add(ArtworkCoverOption(url = it, source = "Kitsu", subtitle = "Official Poster"))
                }
            }
            if (kitsuArt?.score10 != null && kitsuArt.score100 != null) {
                ratings.add(CommunityRating(score10 = kitsuArt.score10, score100 = kitsuArt.score100, source = "Kitsu"))
            }
        }

        // 6. MANGADEX
        if (isMangaDexRequested && "MANGADEX" in missingProviders) {
            val mdArt = mangadexRepo.getCachedByTitle(title) ?: mangadexRepo.fetchCoversByTitle(title)
            mdArt?.covers?.let { mdCovers ->
                mdCovers.forEach { c ->
                    val sub = listOfNotNull(c.volume?.let { "Vol. $it" }, c.locale).joinToString(" ").ifBlank { null }
                    covers.add(ArtworkCoverOption(url = c.url, source = "MangaDex", subtitle = sub))
                }
            }
        }

        if (!defaultCoverUrl.isNullOrBlank()) {
            covers.add(ArtworkCoverOption(url = defaultCoverUrl, source = "Default", subtitle = "Source Cover"))
            bannerOptions.add(ArtworkBannerOption(url = defaultCoverUrl, source = "DEFAULT"))
        }

        val updatedQueriedProviders = (cachedEntry?.queriedProviders ?: emptySet()) + requestedProviders
        val mergedBanners = (banners + (cachedEntry?.allAvailableBanners ?: emptyList())).distinct()
        val mergedCovers = (covers + (cachedEntry?.allAvailableCovers ?: emptyList())).distinctBy { it.url }
        val mergedBannerOptions = (bannerOptions + (cachedEntry?.bannerOptions ?: emptyList())).distinctBy { it.url }
        val mergedRatings = (ratings + (cachedEntry?.ratings ?: emptyList())).distinctBy { it.source }

        val rawResult = UnifiedArtwork(
            bannerUrl = null,
            posterUrl = null,
            allAvailableBanners = mergedBanners,
            allAvailableCovers = mergedCovers,
            bannerOptions = mergedBannerOptions,
            ratings = mergedRatings,
            queriedProviders = updatedQueriedProviders,
        )
        if (mergedBanners.isNotEmpty() || mergedCovers.isNotEmpty() || mergedRatings.isNotEmpty()) {
            cache[title] = rawResult
            scheduleSaveToDisk()
        }
        val effective = getEffectiveArtwork(rawResult, defaultCoverUrl, mangaId)
        ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Waterfall") {
            "Network discovery completed for '$title' -> banner=${effective.bannerUrl}, cover=${effective.posterUrl} (covers=${mergedCovers.size}, banners=${mergedBanners.size})"
        }
        logcat(priority = LogPriority.INFO) { "Resolved waterfall artwork for '$title': banner=${effective.bannerUrl}, cover=${effective.posterUrl}, coversCount=${mergedCovers.size}, bannersCount=${mergedBanners.size}" }
        return@withContext effective
    }

    companion object {
        private val HANGUL_REGEX = Regex("""[\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F]""")
        private val HANZI_REGEX = Regex("""[\u4E00-\u9FFF\u3400-\u4DBF]""")
        private val JAPANESE_REGEX = Regex("""[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]""")

        fun containsHangul(text: String): Boolean = HANGUL_REGEX.containsMatchIn(text)
        fun containsHanzi(text: String): Boolean = HANZI_REGEX.containsMatchIn(text)
        fun containsJapanese(text: String): Boolean = JAPANESE_REGEX.containsMatchIn(text)

        fun cleanTitle(title: String): String {
            val cleaned = title
                .replace(Regex("""\s*[\(\[\{].*?[\)\]\}]"""), " ")
                .replace(Regex("""(?i)\s*-\s*(webtoon|manhwa|manga|comic|scanlations?|scan).*"""), " ")
                .replace(Regex("""(?i)\s+(?:season|s)\s*\d+.*"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            return cleaned.ifBlank { title.trim() }
        }

        fun parsePriorityOrder(
            raw: String,
            allowed: List<String> = listOf("NAVER", "KUAIKAN", "BOOKWALKER", "BANGUMI", "ANILIST", "KITSU", "MANGADEX", "DEFAULT"),
        ): List<String> {
            if (raw.isBlank()) return allowed
            val result = mutableListOf<String>()
            var anySpecified = false
            for (token in raw.split(",")) {
                val trimmed = token.trim()
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    val id = parts[0].uppercase()
                    val enabled = parts[1].toBooleanStrictOrNull() ?: true
                    if (id in allowed) {
                        anySpecified = true
                        if (enabled) {
                            result.add(id)
                        }
                    }
                } else if (trimmed.isNotBlank()) {
                    val id = trimmed.uppercase()
                    if (id in allowed) {
                        anySpecified = true
                        result.add(id)
                    }
                }
            }
            return if (anySpecified) result else allowed
        }
    }
}
