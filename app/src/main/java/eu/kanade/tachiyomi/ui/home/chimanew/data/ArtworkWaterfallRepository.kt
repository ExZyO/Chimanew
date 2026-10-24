// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

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
) {
    private val cache = ConcurrentHashMap<String, UnifiedArtwork>()

    fun getCached(title: String): UnifiedArtwork? = cache[cleanTitle(title)]

    suspend fun resolveArtwork(
        rawTitle: String,
        anilistRemoteId: Long? = null,
        kitsuRemoteId: Long? = null,
    ): UnifiedArtwork {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return UnifiedArtwork()
        cache[title]?.let { return it }

        val configuredOrder = preferences.artworkWaterfallOrder().get()
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("ANILIST", "KITSU", "MANGADEX") }

        // Combine configured priority order with any remaining providers for full discovery
        val allProviders = listOf("ANILIST", "KITSU", "MANGADEX")
        val fullOrder = configuredOrder + (allProviders - configuredOrder.toSet())

        var resolvedBanner: String? = null
        var resolvedPoster: String? = null
        val banners = mutableListOf<String>()
        val covers = mutableListOf<ArtworkCoverOption>()

        for (provider in fullOrder) {
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
                            if (resolvedBanner == null && provider in configuredOrder) {
                                resolvedBanner = it
                            }
                        }
                    }
                    alArt?.extraLargeCoverUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "AniList", subtitle = "HD Poster"))
                            if (resolvedPoster == null && provider in configuredOrder) {
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
                            if (resolvedBanner == null && provider in configuredOrder) {
                                resolvedBanner = it
                            }
                        }
                    }
                    kitsuArt?.posterUrl?.let {
                        if (it.isNotBlank()) {
                            covers.add(ArtworkCoverOption(url = it, source = "Kitsu", subtitle = "Official Poster"))
                            if (resolvedPoster == null && provider in configuredOrder) {
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
                        if (resolvedPoster == null && mdCovers.isNotEmpty() && provider in configuredOrder) {
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
    }
}
