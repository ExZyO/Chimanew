// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import android.app.Application
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_ANIME
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_MANGA
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL
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
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.track.repository.TrackRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CategoryArtworkRule(
    val categoryId: Long,
    val coverSources: List<String> = emptyList(), // e.g. ["MANGADEX", "ANILIST"]
    val bannerSources: List<String> = emptyList(), // e.g. ["ANILIST", "BANGUMI"]
    val coverFallback: String = FALLBACK_WATERFALL, // "waterfall", "default"
    val bannerFallback: String = FALLBACK_WATERFALL, // "waterfall", "none"
    // Backward compatibility with v2.6.54:
    val coverSource: String? = null,
    val bannerSource: String? = null,
    val fallbackMode: String? = null,
) {
    companion object {
        const val FALLBACK_WATERFALL = "waterfall"
        const val FALLBACK_DEFAULT = "default"
        const val FALLBACK_NONE = "none"
    }

    fun effectiveCoverSources(): List<String> {
        if (coverSources.isNotEmpty()) return coverSources
        if (!coverSource.isNullOrBlank()) {
            return when (coverSource.lowercase()) {
                "waterfall" -> emptyList()
                "default" -> listOf("DEFAULT")
                else -> listOf(coverSource.uppercase())
            }
        }
        return emptyList()
    }

    fun effectiveBannerSources(): List<String> {
        if (bannerSources.isNotEmpty()) return bannerSources
        if (!bannerSource.isNullOrBlank()) {
            return when (bannerSource.lowercase()) {
                "waterfall" -> emptyList()
                "none", "default" -> listOf("NONE")
                else -> listOf(bannerSource.uppercase())
            }
        }
        return emptyList()
    }

    fun effectiveCoverFallback(): String {
        if (coverFallback.isNotBlank()) return coverFallback.lowercase()
        return if (fallbackMode == "strict") FALLBACK_DEFAULT else FALLBACK_WATERFALL
    }

    fun effectiveBannerFallback(): String {
        if (bannerFallback.isNotBlank()) return bannerFallback.lowercase()
        return if (fallbackMode == "strict") FALLBACK_NONE else FALLBACK_WATERFALL
    }
}

@Serializable
data class CategoryArtworkRulesConfig(
    val priorityCategoryIds: List<Long> = emptyList(),
    val rules: Map<String, CategoryArtworkRule> = emptyMap(),
    // Backward compatibility:
    val defaultCoverSource: String? = null,
    val defaultBannerSource: String? = null,
    val defaultFallbackMode: String? = null,
) {
    fun getRuleForManga(categoryIds: List<Long>): CategoryArtworkRule? {
        if (rules.isEmpty()) return null
        val catSet = categoryIds.toSet()
        for (catId in priorityCategoryIds) {
            if (catId in catSet) {
                rules[catId.toString()]?.let { return it }
            }
        }
        for (catId in categoryIds) {
            rules[catId.toString()]?.let { return it }
        }
        return null
    }
}

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
    private val getManga: GetManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, UnifiedArtwork>()
    private val mangaCategoriesCache = ConcurrentHashMap<Long, List<Long>>()

    fun setMangaCategories(mangaId: Long, categoryIds: List<Long>) {
        mangaCategoriesCache[mangaId] = categoryIds
    }

    fun getMangaCategories(mangaId: Long): List<Long>? = mangaCategoriesCache[mangaId]

    @Volatile
    private var cachedRulesConfig: CategoryArtworkRulesConfig? = null
    @Volatile
    private var cachedRulesConfigTimestamp: Long = -1L

    fun getCategoryRulesConfig(): CategoryArtworkRulesConfig {
        val currentTimestamp = preferences.categoryArtworkRulesUpdated().get()
        val cached = cachedRulesConfig
        if (cached != null && cachedRulesConfigTimestamp == currentTimestamp) {
            return cached
        }
        val raw = preferences.categoryArtworkRulesConfig().get()
        val parsed = if (raw.isBlank()) {
            CategoryArtworkRulesConfig()
        } else {
            try {
                jsonHelper.decodeFromString<CategoryArtworkRulesConfig>(raw)
            } catch (_: Exception) {
                CategoryArtworkRulesConfig()
            }
        }
        cachedRulesConfig = parsed
        cachedRulesConfigTimestamp = currentTimestamp
        return parsed
    }
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
                        val unified = UnifiedArtwork(
                            bannerUrl = null,
                            posterUrl = null,
                            allAvailableBanners = cleanAllBanners,
                            allAvailableCovers = entry.allCovers.map { ArtworkCoverOption(it.url, it.source, it.subtitle) },
                            bannerOptions = cleanBannerOptions,
                            ratings = entry.ratings.map { CommunityRating(it.score10, it.score100, it.source) },
                            queriedProviders = loadedQueried,
                        )
                        val finalKey = if (title.contains(":")) title else "manga:$title"
                        cache[finalKey] = unified
                        if (!title.contains(":")) {
                            cache[title] = unified
                        }
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

    fun makeKey(title: String, mediaType: Int = MEDIA_TYPE_MANGA): String {
        val clean = cleanTitle(title)
        val prefix = when (mediaType) {
            MEDIA_TYPE_ANIME -> "anime"
            MEDIA_TYPE_NOVEL -> "novel"
            else -> "manga"
        }
        return "$prefix:$clean"
    }

    fun isCached(title: String, mediaType: Int = MEDIA_TYPE_MANGA): Boolean =
        cache.containsKey(makeKey(title, mediaType)) || (mediaType == MEDIA_TYPE_MANGA && cache.containsKey(cleanTitle(title)))

    fun getEffectiveArtwork(
        raw: UnifiedArtwork,
        defaultCoverUrl: String? = null,
        mangaId: Long? = null,
        categoryIds: List<Long>? = null,
        mediaType: Int = MEDIA_TYPE_MANGA,
    ): UnifiedArtwork {
        val excludedBanners = when {
            mangaId == null -> emptySet()
            mediaType == MEDIA_TYPE_ANIME -> preferences.animeExcludedBannerSources(mangaId).get()
            mediaType == MEDIA_TYPE_NOVEL -> preferences.novelExcludedBannerSources(mangaId).get()
            else -> preferences.mangaExcludedBannerSources(mangaId).get()
        }
        val excludedCovers = when {
            mangaId == null -> emptySet()
            mediaType == MEDIA_TYPE_ANIME -> preferences.animeExcludedCoverSources(mangaId).get()
            mediaType == MEDIA_TYPE_NOVEL -> preferences.novelExcludedCoverSources(mangaId).get()
            else -> preferences.mangaExcludedCoverSources(mangaId).get()
        }

        if (mangaId != null && categoryIds != null) {
            mangaCategoriesCache[mangaId] = categoryIds
        }
        val effCategories = categoryIds
            ?: (if (mangaId != null) mangaCategoriesCache[mangaId] else null)
            ?: emptyList()

        val rulesConfig = getCategoryRulesConfig()
        val rule = if (mediaType == MEDIA_TYPE_MANGA) rulesConfig.getRuleForManga(effCategories) else null

        val globalBannerOrder = when (mediaType) {
            MEDIA_TYPE_ANIME -> parsePriorityOrder(
                preferences.animeArtworkBannerOrder().get(),
                allowed = listOf("ANILIST", "KITSU", "BANGUMI", "DEFAULT"),
            )
            MEDIA_TYPE_NOVEL -> parsePriorityOrder(
                preferences.novelArtworkBannerOrder().get(),
                allowed = listOf("BOOKWALKER", "NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "DEFAULT"),
            )
            else -> parsePriorityOrder(
                preferences.artworkBannerOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
                allowed = listOf("NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "KITSU"),
            )
        }.filterNot { it in excludedBanners }

        val globalCoverOrder = when (mediaType) {
            MEDIA_TYPE_ANIME -> parsePriorityOrder(
                preferences.animeArtworkCoverOrder().get(),
                allowed = listOf("ANILIST", "KITSU", "BANGUMI", "DEFAULT"),
            )
            MEDIA_TYPE_NOVEL -> parsePriorityOrder(
                preferences.novelArtworkCoverOrder().get(),
                allowed = listOf("BOOKWALKER", "NAVER", "KUAIKAN", "BANGUMI", "ANILIST", "DEFAULT"),
            )
            else -> parsePriorityOrder(
                preferences.artworkCoverOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
                allowed = listOf("MANGADEX", "BOOKWALKER", "BANGUMI", "ANILIST", "NAVER", "KUAIKAN", "KITSU", "DEFAULT"),
            )
        }.filterNot { it in excludedCovers }

        val defaultCover = defaultCoverUrl?.takeIf { it.isNotBlank() }
            ?: raw.allAvailableCovers.firstOrNull { it.source.equals("Default", ignoreCase = true) }?.url

        fun findBannerFromProvider(provider: String): String? {
            val p = provider.uppercase()
            val banner = raw.bannerOptions.firstOrNull { it.source.equals(p, ignoreCase = true) }?.url
                ?: when (p) {
                    "NAVER" -> raw.allAvailableBanners.firstOrNull { it.contains("pstatic", ignoreCase = true) || it.contains("naver", ignoreCase = true) }
                    "KUAIKAN" -> raw.allAvailableBanners.firstOrNull { it.contains("kkmh", ignoreCase = true) || it.contains("kuaikan", ignoreCase = true) }
                    "BANGUMI" -> raw.allAvailableBanners.firstOrNull { it.contains("bgm.tv", ignoreCase = true) || it.contains("bangumi", ignoreCase = true) }
                    "ANILIST" -> raw.allAvailableBanners.firstOrNull { it.contains("anilist", ignoreCase = true) }
                    "KITSU" -> raw.allAvailableBanners.firstOrNull { it.contains("kitsu", ignoreCase = true) }
                    else -> null
                }
            return if (!banner.isNullOrBlank() && !banner.contains("bookwalker", ignoreCase = true)) banner else null
        }

        fun findCoverFromProvider(provider: String): String? {
            val p = provider.uppercase()
            return when (p) {
                "DEFAULT" -> defaultCover
                else -> {
                    raw.allAvailableCovers.firstOrNull { it.source.equals(p, ignoreCase = true) }?.url
                        ?: when (p) {
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
        }

        var effectiveBanner: String? = null
        if (!excludedBanners.contains("ALL")) {
            if (rule != null) {
                val candidateBannerSources = rule.effectiveBannerSources().filterNot { it in excludedBanners }
                for (source in candidateBannerSources) {
                    if (source.equals("NONE", ignoreCase = true) || source.equals("DEFAULT", ignoreCase = true)) {
                        effectiveBanner = null
                        break
                    }
                    val banner = findBannerFromProvider(source)
                    if (!banner.isNullOrBlank()) {
                        effectiveBanner = banner
                        break
                    }
                }
                if (effectiveBanner.isNullOrBlank() && !candidateBannerSources.any { it.equals("NONE", ignoreCase = true) || it.equals("DEFAULT", ignoreCase = true) }) {
                    if (rule.effectiveBannerFallback() == CategoryArtworkRule.FALLBACK_WATERFALL) {
                        for (provider in globalBannerOrder) {
                            val banner = findBannerFromProvider(provider)
                            if (!banner.isNullOrBlank()) {
                                effectiveBanner = banner
                                break
                            }
                        }
                    }
                }
            } else {
                for (provider in globalBannerOrder) {
                    val banner = findBannerFromProvider(provider)
                    if (!banner.isNullOrBlank()) {
                        effectiveBanner = banner
                        break
                    }
                }
            }
        }

        var effectivePoster: String? = null
        if (!excludedCovers.contains("ALL")) {
            if (rule != null) {
                val candidateCoverSources = rule.effectiveCoverSources().filterNot { it in excludedCovers }
                for (source in candidateCoverSources) {
                    if (source.equals("DEFAULT", ignoreCase = true)) {
                        effectivePoster = defaultCover
                        break
                    }
                    val cover = findCoverFromProvider(source)
                    if (!cover.isNullOrBlank()) {
                        effectivePoster = cover
                        break
                    }
                }
                if (effectivePoster.isNullOrBlank() && !candidateCoverSources.any { it.equals("DEFAULT", ignoreCase = true) }) {
                    if (rule.effectiveCoverFallback() == CategoryArtworkRule.FALLBACK_WATERFALL) {
                        for (provider in globalCoverOrder) {
                            val cover = findCoverFromProvider(provider)
                            if (!cover.isNullOrBlank()) {
                                effectivePoster = cover
                                break
                            }
                        }
                    } else {
                        effectivePoster = defaultCover
                    }
                }
            } else {
                for (provider in globalCoverOrder) {
                    val cover = findCoverFromProvider(provider)
                    if (!cover.isNullOrBlank()) {
                        effectivePoster = cover
                        break
                    }
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

    fun getCached(
        title: String,
        defaultCoverUrl: String? = null,
        mangaId: Long? = null,
        categoryIds: List<Long>? = null,
        mediaType: Int = MEDIA_TYPE_MANGA,
    ): UnifiedArtwork? {
        val key = makeKey(title, mediaType)
        val raw = cache[key] ?: (if (mediaType == MEDIA_TYPE_MANGA) cache[cleanTitle(title)] else null) ?: return null
        val effective = getEffectiveArtwork(raw, defaultCoverUrl, mangaId, categoryIds, mediaType)
        ChimanewDiagnostics.log(DiagnosticCategory.ARTWORK, "Cache") {
            "Memory cache HIT for '[$mediaType] $title': banner=${effective.bannerUrl}, poster=${effective.posterUrl}"
        }
        return effective
    }

    fun putCached(title: String, artwork: UnifiedArtwork, mediaType: Int = MEDIA_TYPE_MANGA) {
        val clean = cleanTitle(title)
        if (clean.isNotBlank()) {
            val key = makeKey(title, mediaType)
            cache[key] = artwork
            if (mediaType == MEDIA_TYPE_MANGA) {
                cache[clean] = artwork
            }
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
        altTitle: String? = null,
        categoryIds: List<Long>? = null,
        mediaType: Int = MEDIA_TYPE_MANGA,
    ): UnifiedArtwork = withContext(Dispatchers.IO) {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return@withContext UnifiedArtwork()

        val cacheKey = makeKey(title, mediaType)

        if (mangaId != null) {
            if (categoryIds != null) {
                mangaCategoriesCache[mangaId] = categoryIds
            } else if (!mangaCategoriesCache.containsKey(mangaId)) {
                try {
                    val cats = getCategories.await(mangaId).map { it.id }
                    mangaCategoriesCache[mangaId] = cats
                } catch (_: Exception) {}
            }
        }
        val effCategories = categoryIds
            ?: (if (mangaId != null) mangaCategoriesCache[mangaId] else null)
            ?: emptyList()

        if (forceRefresh) {
            cache.remove(cacheKey)
            if (mediaType == MEDIA_TYPE_MANGA) {
                cache.remove(title)
                mangadexRepo.removeTitleFromCache(title)
                if (!altTitle.isNullOrBlank()) {
                    mangadexRepo.removeTitleFromCache(altTitle)
                }
                mangadexRepo.resetRateLimit()
            }
        }

        if (mediaType == MEDIA_TYPE_ANIME) {
            val cachedEntry = cache[cacheKey]
            if (cachedEntry != null && !forceRefresh) {
                return@withContext getEffectiveArtwork(
                    raw = cachedEntry,
                    defaultCoverUrl = defaultCoverUrl,
                    mangaId = mangaId,
                    categoryIds = effCategories,
                    mediaType = MEDIA_TYPE_ANIME,
                )
            }

            val banners = mutableListOf<String>()
            val bannerOptions = mutableListOf<ArtworkBannerOption>()
            val covers = mutableListOf<ArtworkCoverOption>()
            val ratings = mutableListOf<CommunityRating>()

            if (!defaultCoverUrl.isNullOrBlank()) {
                covers.add(ArtworkCoverOption(url = defaultCoverUrl, source = "Default", subtitle = "Source Cover"))
            }

            // 1. ANILIST ANIME (Official TV visual & wide banner)
            try {
                val alArt = if (anilistRemoteId != null && anilistRemoteId > 0) {
                    anilistRepo.fetchBatch(listOf(anilistRemoteId), isAnime = true)[anilistRemoteId]
                } else if (malRemoteId != null && malRemoteId > 0) {
                    anilistRepo.fetchByMalId(malRemoteId, isAnime = true)
                } else {
                    anilistRepo.fetchByTitle(title, isAnime = true)
                }

                alArt?.bannerUrl?.let {
                    if (it.isNotBlank()) {
                        banners.add(it)
                        bannerOptions.add(ArtworkBannerOption(it, "ANILIST"))
                    }
                }
                alArt?.extraLargeCoverUrl?.let {
                    if (it.isNotBlank()) {
                        covers.add(ArtworkCoverOption(url = it, source = "AniList", subtitle = "TV Anime Poster"))
                    }
                }
                if (alArt?.score10 != null && alArt.score100 != null) {
                    ratings.add(CommunityRating(score10 = alArt.score10, score100 = alArt.score100, source = "AniList"))
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed AniList anime fetch for $title" }
            }

            // 2. KITSU ANIME
            try {
                val kitsuArt = if (kitsuRemoteId != null && kitsuRemoteId > 0) {
                    kitsuRepo.fetchByKitsuId(kitsuRemoteId, isAnime = true)
                } else {
                    kitsuRepo.fetchByTitle(title, isAnime = true)
                }
                kitsuArt?.bannerUrl?.let {
                    if (it.isNotBlank()) {
                        banners.add(it)
                        bannerOptions.add(ArtworkBannerOption(it, "KITSU"))
                    }
                }
                kitsuArt?.posterUrl?.let {
                    if (it.isNotBlank()) {
                        covers.add(ArtworkCoverOption(url = it, source = "Kitsu", subtitle = "Key Visual"))
                    }
                }
                if (kitsuArt?.score10 != null && kitsuArt.score100 != null) {
                    ratings.add(CommunityRating(score10 = kitsuArt.score10, score100 = kitsuArt.score100, source = "Kitsu"))
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed Kitsu anime fetch for $title" }
            }

            // 3. BANGUMI ANIME
            try {
                val bangumiArt = bangumiRepo.fetchArtwork(title, isAnime = true)
                bangumiArt?.bannerUrl?.let {
                    if (it.isNotBlank()) {
                        banners.add(it)
                        bannerOptions.add(ArtworkBannerOption(it, "BANGUMI"))
                    }
                }
                bangumiArt?.coverUrl?.let {
                    if (it.isNotBlank()) {
                        covers.add(ArtworkCoverOption(url = it, source = "Bangumi", subtitle = "Official Anime Art"))
                    }
                }
                if (bangumiArt?.score10 != null && bangumiArt.score100 != null) {
                    ratings.add(CommunityRating(score10 = bangumiArt.score10, score100 = bangumiArt.score100, source = "Bangumi"))
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed Bangumi anime fetch for $title" }
            }

            val unified = UnifiedArtwork(
                bannerUrl = null,
                posterUrl = null,
                allAvailableBanners = banners.distinct(),
                allAvailableCovers = covers.distinctBy { it.url },
                bannerOptions = bannerOptions.distinctBy { it.url },
                ratings = ratings,
                queriedProviders = setOf("ANILIST", "KITSU", "BANGUMI"),
            )
            cache[cacheKey] = unified
            scheduleSaveToDisk()

            return@withContext getEffectiveArtwork(
                raw = unified,
                defaultCoverUrl = defaultCoverUrl,
                mangaId = mangaId,
                categoryIds = effCategories,
                mediaType = MEDIA_TYPE_ANIME,
            )
        }

        val rulesConfig = getCategoryRulesConfig()
        val rule = rulesConfig.getRuleForManga(effCategories)

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

        val ruleCoverSources = rule?.effectiveCoverSources() ?: emptyList()
        val ruleBannerSources = rule?.effectiveBannerSources() ?: emptyList()
        val ruleCoverFallback = rule?.effectiveCoverFallback() ?: CategoryArtworkRule.FALLBACK_WATERFALL
        val ruleBannerFallback = rule?.effectiveBannerFallback() ?: CategoryArtworkRule.FALLBACK_WATERFALL

        val neededCoverSources = when {
            excludedCovers.contains("ALL") -> emptySet()
            rule != null -> {
                val explicit = ruleCoverSources.filterNot { it.equals("DEFAULT", ignoreCase = true) }.toSet()
                if (ruleCoverFallback == CategoryArtworkRule.FALLBACK_WATERFALL) {
                    explicit + coverOrder.filterNot { it.equals("DEFAULT", ignoreCase = true) }
                } else {
                    explicit
                }
            }
            else -> coverOrder.filterNot { it.equals("DEFAULT", ignoreCase = true) }.toSet()
        }

        val neededBannerSources = when {
            excludedBanners.contains("ALL") -> emptySet()
            rule != null -> {
                val explicit = ruleBannerSources.filterNot { it.equals("NONE", ignoreCase = true) || it.equals("DEFAULT", ignoreCase = true) }.toSet()
                if (ruleBannerFallback == CategoryArtworkRule.FALLBACK_WATERFALL) {
                    explicit + bannerOrder
                } else {
                    explicit
                }
            }
            else -> bannerOrder.toSet()
        }

        val requestedProviders = (neededCoverSources + neededBannerSources).map { it.uppercase() }.toSet()

        if (requestedProviders.isEmpty()) {
            return@withContext UnifiedArtwork(
                bannerUrl = null,
                posterUrl = defaultCoverUrl,
                allAvailableBanners = emptyList(),
                allAvailableCovers = if (!defaultCoverUrl.isNullOrBlank()) {
                    listOf(ArtworkCoverOption(url = defaultCoverUrl, source = "Default", subtitle = "Source Cover"))
                } else emptyList(),
                bannerOptions = emptyList(),
                ratings = emptyList(),
                queriedProviders = setOf("DEFAULT"),
            )
        }

        val isNaverRequested = "NAVER" in requestedProviders
        val isKuaikanRequested = "KUAIKAN" in requestedProviders
        val isBangumiRequested = "BANGUMI" in requestedProviders
        val isBookWalkerRequested = "BOOKWALKER" in requestedProviders
        val isKitsuRequested = "KITSU" in requestedProviders
        val isMangaDexRequested = "MANGADEX" in requestedProviders
        val isAniListRequested = "ANILIST" in requestedProviders
        val needsAsianBridge = isNaverRequested || isKuaikanRequested || isBangumiRequested || isBookWalkerRequested

        val cachedEntry = cache[title]
        if (cachedEntry != null && !forceRefresh) {
            val missingProviders = requestedProviders - cachedEntry.queriedProviders
            if (missingProviders.isEmpty()) {
                val cached = getEffectiveArtwork(cachedEntry, defaultCoverUrl, mangaId, effCategories)
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
        var effectiveMangadexId: String? = null
        var effectiveAltTitle = altTitle

        if (mangaId != null) {
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
                val mdTrack = tracks.firstOrNull { it.trackerId == 60L }
                effectiveMangadexId = mdTrack?.remoteUrl?.substringAfter("/manga/")?.substringBefore("/")?.substringBefore("?")?.trim()?.takeIf { it.length >= 32 }

                val mangaObj = getManga.await(mangaId)
                if (mangaObj != null) {
                    if (effectiveMangadexId == null && mangaObj.url.contains("/manga/")) {
                        effectiveMangadexId = mangaObj.url.substringAfter("/manga/").substringBefore("/").substringBefore("?").trim().takeIf { it.length >= 32 }
                    }
                    if (effectiveAltTitle == null) {
                        val cTitle = cleanTitle(rawTitle)
                        val mTitle = mangaObj.title.trim()
                        val mOgTitle = mangaObj.ogTitle?.trim()
                        if (cTitle == cleanTitle(mTitle) && !mOgTitle.isNullOrBlank()) {
                            effectiveAltTitle = mOgTitle
                        } else if (!mTitle.isBlank() && cTitle != cleanTitle(mTitle)) {
                            effectiveAltTitle = mTitle
                        }
                    }
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
        val hasExistingMdCovers = cachedEntry?.allAvailableCovers?.any { it.source.equals("MangaDex", true) } == true
        val shouldQueryMangaDex = isMangaDexRequested && ("MANGADEX" in missingProviders || !hasExistingMdCovers || forceRefresh)
        if (shouldQueryMangaDex) {
            val mdArt = if (!effectiveMangadexId.isNullOrBlank()) {
                if (forceRefresh) mangadexRepo.fetchCoversByMangaId(effectiveMangadexId, forceRefresh = true)
                else mangadexRepo.getCached(effectiveMangadexId) ?: mangadexRepo.fetchCoversByMangaId(effectiveMangadexId)
            } else null
                ?: (if (!forceRefresh) mangadexRepo.getCachedByTitle(title) else null)
                ?: mangadexRepo.fetchCoversByTitle(
                    rawTitle = title,
                    forceRefresh = forceRefresh,
                    anilistId = effectiveAniListId,
                    malId = effectiveMalId,
                )
                ?: (if (!effectiveAltTitle.isNullOrBlank() && effectiveAltTitle != title) {
                    mangadexRepo.fetchCoversByTitle(
                        rawTitle = effectiveAltTitle,
                        forceRefresh = forceRefresh,
                        anilistId = effectiveAniListId,
                        malId = effectiveMalId,
                    )
                } else null)

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
            cache[cacheKey] = rawResult
            if (mediaType == MEDIA_TYPE_MANGA) {
                cache[title] = rawResult
            }
            scheduleSaveToDisk()
        }
        val effective = getEffectiveArtwork(rawResult, defaultCoverUrl, mangaId, effCategories, mediaType)
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
