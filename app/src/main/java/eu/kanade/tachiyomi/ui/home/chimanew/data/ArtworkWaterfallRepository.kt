// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import android.app.Application
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
)

class ArtworkWaterfallRepository(
    private val anilistRepo: AnilistCoverRepository = Injekt.get(),
    private val kitsuRepo: KitsuCoverRepository = Injekt.get(),
    private val mangadexRepo: MangadexCoverRepository = Injekt.get(),
    private val preferences: ChimanewPreferences = Injekt.get(),
    private val trackRepository: TrackRepository = Injekt.get(),
    private val json: Json = Injekt.get(),
    private val context: Application = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, UnifiedArtwork>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val cacheFile by lazy { File(context.filesDir, "chimanew_artwork_waterfall_cache.json") }
    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
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
                                    url.contains("anilist", ignoreCase = true) -> "ANILIST"
                                    else -> "ANILIST"
                                }
                                ArtworkBannerOption(url, inferredSource)
                            }
                        }
                        cache[title] = UnifiedArtwork(
                            bannerUrl = null,
                            posterUrl = null,
                            allAvailableBanners = entry.allBanners.ifEmpty { bannersWithOptions.map { it.url } },
                            allAvailableCovers = entry.allCovers.map { ArtworkCoverOption(it.url, it.source, it.subtitle) },
                            bannerOptions = bannersWithOptions,
                            ratings = entry.ratings.map { CommunityRating(it.score10, it.score100, it.source) },
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
                    )
                }
                val content = jsonHelper.encodeToString(snapshot)
                cacheFile.writeText(content)
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to save persistent artwork cache to disk" }
            }
        }
    }

    fun isCached(title: String): Boolean = cache.containsKey(cleanTitle(title))

    fun getEffectiveArtwork(raw: UnifiedArtwork): UnifiedArtwork {
        val bannerOrder = parsePriorityOrder(
            preferences.artworkBannerOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
            allowed = listOf("ANILIST", "KITSU"),
        )
        val coverOrder = parsePriorityOrder(
            preferences.artworkCoverOrder().get().ifBlank { preferences.artworkWaterfallOrder().get() },
            allowed = listOf("MANGADEX", "ANILIST", "KITSU"),
        )

        var effectiveBanner: String? = null
        for (provider in bannerOrder) {
            val banner = raw.bannerOptions.firstOrNull { it.source.equals(provider, ignoreCase = true) }?.url
                ?: when (provider) {
                    "ANILIST" -> raw.allAvailableBanners.firstOrNull { it.contains("anilist", ignoreCase = true) }
                    "KITSU" -> raw.allAvailableBanners.firstOrNull { it.contains("kitsu", ignoreCase = true) }
                    else -> null
                }
            if (!banner.isNullOrBlank()) {
                effectiveBanner = banner
                break
            }
        }

        var effectivePoster: String? = null
        for (provider in coverOrder) {
            val cover = raw.allAvailableCovers.firstOrNull { it.source.equals(provider, ignoreCase = true) }?.url
            if (!cover.isNullOrBlank()) {
                effectivePoster = cover
                break
            }
        }

        return raw.copy(
            bannerUrl = effectiveBanner,
            posterUrl = effectivePoster,
        )
    }

    fun getCached(title: String): UnifiedArtwork? {
        val raw = cache[cleanTitle(title)] ?: return null
        return getEffectiveArtwork(raw)
    }

    suspend fun resolveArtwork(
        rawTitle: String,
        anilistRemoteId: Long? = null,
        kitsuRemoteId: Long? = null,
        malRemoteId: Long? = null,
        mangaId: Long? = null,
    ): UnifiedArtwork {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return UnifiedArtwork()
        cache[title]?.let { return getEffectiveArtwork(it) }

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

        val allProviders = listOf("ANILIST", "KITSU", "MANGADEX")
        val banners = mutableListOf<String>()
        val bannerOptions = mutableListOf<ArtworkBannerOption>()
        val covers = mutableListOf<ArtworkCoverOption>()
        val ratings = mutableListOf<CommunityRating>()

        for (provider in allProviders) {
            when (provider) {
                "ANILIST" -> {
                    var alArt = if (effectiveAniListId != null && effectiveAniListId > 0) {
                        anilistRepo.getCached(effectiveAniListId) ?: anilistRepo.fetchBatch(listOf(effectiveAniListId))[effectiveAniListId]
                    } else null

                    if ((alArt == null || (alArt.bannerUrl == null && alArt.extraLargeCoverUrl == null)) && effectiveMalId != null && effectiveMalId > 0) {
                        alArt = anilistRepo.getCachedByMalId(effectiveMalId) ?: anilistRepo.fetchByMalId(effectiveMalId)
                    }

                    if (alArt == null || (alArt.bannerUrl == null && alArt.extraLargeCoverUrl == null)) {
                        alArt = anilistRepo.getCachedByTitle(title) ?: anilistRepo.fetchByTitle(title)
                    }

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
                "KITSU" -> {
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
                "MANGADEX" -> {
                    val mdArt = mangadexRepo.getCachedByTitle(title) ?: mangadexRepo.fetchCoversByTitle(title)
                    mdArt?.covers?.let { mdCovers ->
                        mdCovers.forEach { c ->
                            val sub = listOfNotNull(c.volume?.let { "Vol. $it" }, c.locale).joinToString(" ").ifBlank { null }
                            covers.add(ArtworkCoverOption(url = c.url, source = "MangaDex", subtitle = sub))
                        }
                    }
                }
            }
        }

        val rawResult = UnifiedArtwork(
            bannerUrl = null,
            posterUrl = null,
            allAvailableBanners = banners.distinct(),
            allAvailableCovers = covers.distinctBy { it.url },
            bannerOptions = bannerOptions.distinctBy { it.url },
            ratings = ratings.distinctBy { it.source },
        )
        cache[title] = rawResult
        scheduleSaveToDisk()
        return getEffectiveArtwork(rawResult)
    }

    companion object {
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
            allowed: List<String> = listOf("ANILIST", "KITSU", "MANGADEX"),
        ): List<String> {
            if (raw.isBlank()) return allowed
            val result = mutableListOf<String>()
            for (token in raw.split(",")) {
                val trimmed = token.trim()
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    val id = parts[0].uppercase()
                    val enabled = parts[1].toBooleanStrictOrNull() ?: true
                    if (enabled && id in allowed) {
                        result.add(id)
                    }
                } else if (trimmed.isNotBlank()) {
                    val id = trimmed.uppercase()
                    if (id in allowed) {
                        result.add(id)
                    }
                }
            }
            return result.ifEmpty { allowed }
        }
    }
}
