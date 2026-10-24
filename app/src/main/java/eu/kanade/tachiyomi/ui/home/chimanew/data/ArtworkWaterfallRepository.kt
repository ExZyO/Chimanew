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
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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
)

class ArtworkWaterfallRepository(
    private val anilistRepo: AnilistCoverRepository = Injekt.get(),
    private val kitsuRepo: KitsuCoverRepository = Injekt.get(),
    private val mangadexRepo: MangadexCoverRepository = Injekt.get(),
    private val preferences: ChimanewPreferences = Injekt.get(),
    private val json: Json = Injekt.get(),
    private val context: Application = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, UnifiedArtwork>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val cacheFile by lazy { File(context.filesDir, "chimanew_artwork_waterfall_cache.json") }
    private var saveJob: Job? = null

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                if (content.isNotBlank()) {
                    val persistedMap = json.decodeFromString<Map<String, PersistedArtworkEntry>>(content)
                    for ((title, entry) in persistedMap) {
                        cache[title] = UnifiedArtwork(
                            bannerUrl = entry.bannerUrl,
                            posterUrl = entry.posterUrl,
                            allAvailableBanners = entry.allBanners,
                            allAvailableCovers = entry.allCovers.map { ArtworkCoverOption(it.url, it.source, it.subtitle) },
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
                    )
                }
                val content = json.encodeToString(snapshot)
                cacheFile.writeText(content)
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to save persistent artwork cache to disk" }
            }
        }
    }

    fun getCached(title: String): UnifiedArtwork? = cache[cleanTitle(title)]

    suspend fun resolveArtwork(
        rawTitle: String,
        anilistRemoteId: Long? = null,
        kitsuRemoteId: Long? = null,
    ): UnifiedArtwork {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return UnifiedArtwork()
        cache[title]?.let { return it }

        val configuredOrder = parsePriorityOrder(preferences.artworkWaterfallOrder().get())

        // Always query configured providers first, then any unlisted providers for additional covers
        val allProviders = listOf("ANILIST", "KITSU", "MANGADEX")
        val fullOrder = configuredOrder + (allProviders - configuredOrder.toSet())

        var resolvedBanner: String? = null
        var resolvedPoster: String? = null
        val banners = mutableListOf<String>()
        val covers = mutableListOf<ArtworkCoverOption>()

        for (provider in fullOrder) {
            val isEnabled = provider in configuredOrder
            when (provider) {
                "ANILIST" -> {
                    val alArt = if (anilistRemoteId != null && anilistRemoteId > 0) {
                        anilistRepo.getCached(anilistRemoteId) ?: anilistRepo.fetchBatch(listOf(anilistRemoteId))[anilistRemoteId]
                    } else {
                        anilistRepo.getCachedByTitle(title) ?: anilistRepo.fetchByTitle(title)
                    }

                    alArt?.bannerUrl?.let {
                        if (it.isNotBlank()) {
                            banners.add(it)
                            if (resolvedBanner == null && isEnabled) {
                                resolvedBanner = it
                            }
                        }
                    }
                    alArt?.extraLargeCoverUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "AniList", subtitle = "HD Poster"))
                            if (resolvedPoster == null && isEnabled) {
                                resolvedPoster = it
                            }
                        }
                    }
                }
                "KITSU" -> {
                    val kitsuArt = if (kitsuRemoteId != null && kitsuRemoteId > 0) {
                        kitsuRepo.getCached(kitsuRemoteId) ?: kitsuRepo.fetchByKitsuId(kitsuRemoteId)
                    } else {
                        kitsuRepo.getCachedByTitle(title) ?: kitsuRepo.fetchByTitle(title)
                    }

                    kitsuArt?.bannerUrl?.let {
                        if (it.isNotBlank()) {
                            banners.add(it)
                            if (resolvedBanner == null && isEnabled) {
                                resolvedBanner = it
                            }
                        }
                    }
                    kitsuArt?.posterUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "Kitsu", subtitle = "Official Poster"))
                            if (resolvedPoster == null && isEnabled) {
                                resolvedPoster = it
                            }
                        }
                    }
                }
                "MANGADEX" -> {
                    val mdArt = mangadexRepo.getCachedByTitle(title) ?: mangadexRepo.fetchCoversByTitle(title)
                    mdArt?.covers?.let { mdCovers ->
                        mdCovers.forEach { c ->
                            val sub = listOfNotNull(c.volume?.let { "Vol. $it" }, c.locale).joinToString(" ").ifBlank { null }
                            covers.add(ArtworkCoverOption(url = c.url, source = "MangaDex", subtitle = sub))
                        }
                        if (resolvedPoster == null && mdCovers.isNotEmpty() && isEnabled) {
                            resolvedPoster = mdCovers.firstOrNull()?.url
                        }
                    }
                }
            }
        }

        val result = UnifiedArtwork(
            bannerUrl = resolvedBanner,
            posterUrl = resolvedPoster,
            allAvailableBanners = banners.distinct(),
            allAvailableCovers = covers.distinctBy { it.url },
        )
        cache[title] = result
        scheduleSaveToDisk()
        return result
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

        fun parsePriorityOrder(raw: String): List<String> {
            if (raw.isBlank()) return listOf("ANILIST", "KITSU", "MANGADEX")
            val result = mutableListOf<String>()
            for (token in raw.split(",")) {
                val trimmed = token.trim()
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    val id = parts[0].uppercase()
                    val enabled = parts[1].toBooleanStrictOrNull() ?: true
                    if (enabled && id in listOf("ANILIST", "KITSU", "MANGADEX")) {
                        result.add(id)
                    }
                } else if (trimmed.isNotBlank()) {
                    val id = trimmed.uppercase()
                    if (id in listOf("ANILIST", "KITSU", "MANGADEX")) {
                        result.add(id)
                    }
                }
            }
            return result.ifEmpty { listOf("ANILIST", "KITSU", "MANGADEX") }
        }
    }
}
