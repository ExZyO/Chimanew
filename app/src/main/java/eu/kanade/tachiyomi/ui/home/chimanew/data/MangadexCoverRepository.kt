// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class MangadexCoverArt(
    val volume: String?,
    val locale: String?,
    val url: String,
    val isOriginal: Boolean = false,
)

data class MangadexMediaArtwork(
    val mangaId: String? = null,
    val covers: List<MangadexCoverArt> = emptyList(),
)

class MangadexCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val titleCache = ConcurrentHashMap<String, MangadexMediaArtwork>()
    private val idCache = ConcurrentHashMap<String, MangadexMediaArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private val requestMutex = Mutex()
    private var lastRequestTime = 0L

    private suspend fun throttle() {
        requestMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < 250L) {
                delay(250L - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    private fun isRateLimited(): Boolean = System.currentTimeMillis() < rateLimitedUntil

    fun resetRateLimit() {
        rateLimitedUntil = 0L
    }

    fun removeTitleFromCache(rawTitle: String) {
        titleCache.remove(cleanTitle(rawTitle))
        rateLimitedUntil = 0L
    }

    fun clearCache() {
        titleCache.clear()
        idCache.clear()
        rateLimitedUntil = 0L
    }

    fun getCached(mangaId: String): MangadexMediaArtwork? = idCache[mangaId]

    fun getCachedByTitle(rawTitle: String): MangadexMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchCoversByMangaId(mangaId: String, forceRefresh: Boolean = false): MangadexMediaArtwork? {
        if (forceRefresh) {
            idCache.remove(mangaId)
            rateLimitedUntil = 0L
        } else {
            idCache[mangaId]?.takeIf { it.covers.isNotEmpty() }?.let { return it }
        }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                throttle()
                val url = "$API_URL/cover?manga[]=$mangaId&limit=100&order[volume]=desc"
                val headers = Headers.Builder()
                    .add("User-Agent", USER_AGENT)
                    .build()

                val response = client.newCall(GET(url, headers)).awaitSuccess()
                val body = response.body.string()
                val root = json.parseToJsonElement(body) as? JsonObject ?: return@withContext null
                val dataArr = root["data"] as? JsonArray ?: return@withContext null

                val covers = dataArr.mapNotNull { item ->
                    val attrs = (item as? JsonObject)?.get("attributes") as? JsonObject ?: return@mapNotNull null
                    val fileName = attrs["fileName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val volume = attrs["volume"]?.jsonPrimitive?.contentOrNull
                    val locale = attrs["locale"]?.jsonPrimitive?.contentOrNull
                    val coverUrl = "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
                    MangadexCoverArt(volume, locale, coverUrl)
                }

                val artwork = MangadexMediaArtwork(mangaId, covers)
                if (covers.isNotEmpty()) {
                    idCache[mangaId] = artwork
                }
                artwork
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 4_000L
                }
                logcat(throwable = e) { "Failed to fetch MangaDex covers for ID: $mangaId" }
                null
            }
        }
    }

    suspend fun fetchCoversByTitle(
        rawTitle: String,
        forceRefresh: Boolean = false,
        anilistId: Long? = null,
        malId: Long? = null,
    ): MangadexMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null

        if (forceRefresh) {
            titleCache.remove(title)
            rateLimitedUntil = 0L
        } else {
            titleCache[title]?.takeIf { it.covers.isNotEmpty() }?.let { return it }
        }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val headers = Headers.Builder()
                    .add("User-Agent", USER_AGENT)
                    .build()

                suspend fun executeSearch(query: String): JsonArray? {
                    throttle()
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val searchUrl = "$API_URL/manga?title=$encoded&limit=15&includes[]=cover_art&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&order[relevance]=desc"
                    val searchResponse = client.newCall(GET(searchUrl, headers)).awaitSuccess()
                    val searchBody = searchResponse.body.string()
                    val searchRoot = json.parseToJsonElement(searchBody) as? JsonObject ?: return null
                    return searchRoot["data"] as? JsonArray
                }

                var dataArr = executeSearch(title)
                if (dataArr.isNullOrEmpty()) {
                    // Try simplified alphanumeric search if title contained symbols or colons
                    val simplified = title.replace(Regex("""[^a-zA-Z0-9\s]"""), " ").replace(Regex("""\s+"""), " ").trim()
                    if (simplified != title && simplified.length >= 2) {
                        dataArr = executeSearch(simplified)
                    }
                }

                if (dataArr.isNullOrEmpty()) {
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex") {
                        "No MangaDex results returned for '$title'"
                    }
                    return@withContext null
                }

                data class ScoredCandidate(
                    val id: String,
                    val score: Int,
                    val embeddedCoverUrl: String?,
                    val summary: String,
                )

                val scoredCandidates = mutableListOf<ScoredCandidate>()

                for (item in dataArr) {
                    val obj = item as? JsonObject ?: continue
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val attrs = obj["attributes"] as? JsonObject ?: continue
                    val links = attrs["links"] as? JsonObject

                    var score = 0

                    // 1. Direct Tracker cross-reference link matching (+1000)
                    val alLink = (links?.get("al") as? JsonPrimitive)?.contentOrNull
                    val malLink = (links?.get("mal") as? JsonPrimitive)?.contentOrNull
                    if (anilistId != null && alLink == anilistId.toString()) {
                        score += 1000
                    }
                    if (malId != null && malLink == malId.toString()) {
                        score += 1000
                    }

                    // 2. Title matching
                    val titleMap = attrs["title"] as? JsonObject
                    val mainTitles = titleMap?.values?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
                    val altTitlesList = (attrs["altTitles"] as? JsonArray)?.flatMap { altObj ->
                        (altObj as? JsonObject)?.values?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
                    } ?: emptyList()
                    val allCandidateTitles = mainTitles + altTitlesList

                    val qNorm = normalizeForComparison(title)
                    val qTokens = qNorm.split(" ").filter { it.isNotBlank() }.toSet()

                    var bestTitleScore = 0
                    for (cTitle in allCandidateTitles) {
                        val cNorm = normalizeForComparison(cTitle)
                        if (cNorm.isBlank()) continue
                        if (cNorm == qNorm) {
                            bestTitleScore = maxOf(bestTitleScore, 500)
                        } else if (cNorm.startsWith(qNorm) || qNorm.startsWith(cNorm)) {
                            bestTitleScore = maxOf(bestTitleScore, 300)
                        } else if (cNorm.contains(qNorm) || qNorm.contains(cNorm)) {
                            bestTitleScore = maxOf(bestTitleScore, 250)
                        } else {
                            val cTokens = cNorm.split(" ").filter { it.isNotBlank() }.toSet()
                            if (qTokens.isNotEmpty() && cTokens.isNotEmpty()) {
                                val intersection = qTokens.intersect(cTokens).size
                                val ratio = intersection.toFloat() / qTokens.size
                                if (ratio >= 0.7f) {
                                    bestTitleScore = maxOf(bestTitleScore, (ratio * 200).toInt())
                                }
                            }
                        }
                    }
                    score += bestTitleScore

                    // 3. Penalize fan doujinshis & fan cuts
                    val tags = (attrs["tags"] as? JsonArray)?.mapNotNull { tagObj ->
                        ((tagObj as? JsonObject)?.get("attributes") as? JsonObject)?.get("name")?.jsonObject?.get("en")?.jsonPrimitive?.contentOrNull?.lowercase()
                    } ?: emptyList()

                    val isDoujinshi = "doujinshi" in tags || allCandidateTitles.any { it.contains("doujinshi", ignoreCase = true) }
                    if (isDoujinshi) {
                        score -= 500
                    }

                    val isFanOrColored = allCandidateTitles.any {
                        it.contains("fan colored", ignoreCase = true) ||
                        it.contains("official colored", ignoreCase = true) ||
                        it.contains("side story", ignoreCase = true)
                    }
                    if (isFanOrColored) {
                        score -= 50
                    }

                    // Extract embedded primary cover if available
                    val relationships = obj["relationships"] as? JsonArray
                    val coverArtRel = relationships?.firstOrNull { (it as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull == "cover_art" } as? JsonObject
                    val coverFileName = (coverArtRel?.get("attributes") as? JsonObject)?.get("fileName")?.jsonPrimitive?.contentOrNull
                    val embeddedCoverUrl = if (coverFileName != null) {
                        "https://uploads.mangadex.org/covers/$id/$coverFileName.512.jpg"
                    } else null

                    if (score > 0 && bestTitleScore > 0) {
                        scoredCandidates.add(ScoredCandidate(id, score, embeddedCoverUrl, mainTitles.firstOrNull() ?: id))
                    }
                }

                scoredCandidates.sortByDescending { it.score }

                if (scoredCandidates.isEmpty()) {
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex") {
                        "No title match for '$title' among MangaDex candidates"
                    }
                    return@withContext null
                }

                // Try top candidates until one provides volume covers
                var finalArtwork: MangadexMediaArtwork? = null
                for (cand in scoredCandidates.take(3)) {
                    val coversArt = fetchCoversByMangaId(cand.id, forceRefresh = forceRefresh)
                    if (coversArt != null && coversArt.covers.isNotEmpty()) {
                        finalArtwork = coversArt
                        break
                    } else if (cand.embeddedCoverUrl != null && finalArtwork == null) {
                        // Keep embedded cover as fallback if volume query yields 0 covers
                        finalArtwork = MangadexMediaArtwork(
                            cand.id,
                            listOf(MangadexCoverArt(volume = null, locale = null, url = cand.embeddedCoverUrl, isOriginal = true)),
                        )
                    }
                }

                if (finalArtwork != null && finalArtwork.covers.isNotEmpty()) {
                    titleCache[title] = finalArtwork
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex") {
                        "Found ${finalArtwork.covers.size} MangaDex covers for '$title' (ID: ${finalArtwork.mangaId})"
                    }
                    return@withContext finalArtwork
                }

                ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex") {
                    "MangaDex candidate matches had 0 covers for '$title'"
                }
                null
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 4_000L
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex") {
                        "MangaDex rate limited (429). Pausing calls for 4s."
                    }
                } else {
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "MangaDex", e) {
                        "Failed MangaDex search for '$title': ${e.message}"
                    }
                }
                logcat(throwable = e) { "Failed to fetch MangaDex manga search for title: $title" }
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://api.mangadex.org"
        private const val USER_AGENT = "Chimanew/2.6.52 (https://github.com/ExZyO/Chimanew)"

        fun cleanTitle(title: String): String {
            val cleaned = title
                .replace(Regex("""\s*[\(\[\{].*?[\)\]\}]"""), " ")
                .replace(Regex("""(?i)\s*-\s*(webtoon|manhwa|manga|comic|scanlations?|scan).*"""), " ")
                .replace(Regex("""(?i)\s+(?:season|s)\s*\d+.*"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            return cleaned.ifBlank { title.trim() }
        }

        fun isTitleMatch(queryTitle: String, candidateTitles: List<String>): Boolean {
            val qNorm = normalizeForComparison(queryTitle)
            if (qNorm.isBlank()) return false
            val qTokens = qNorm.split(" ").filter { it.isNotBlank() }.toSet()

            for (candidate in candidateTitles) {
                val cNorm = normalizeForComparison(candidate)
                if (cNorm.isBlank()) continue
                if (qNorm == cNorm) return true
                if (cNorm.startsWith(qNorm) || qNorm.startsWith(cNorm)) return true
                if (qNorm.contains(cNorm) || cNorm.contains(qNorm)) {
                    val minLen = minOf(qNorm.length, cNorm.length)
                    val maxLen = maxOf(qNorm.length, cNorm.length)
                    if (minLen.toFloat() / maxLen >= 0.35f) return true
                }
                val cTokens = cNorm.split(" ").filter { it.isNotBlank() }.toSet()
                if (qTokens.isNotEmpty() && cTokens.isNotEmpty()) {
                    val intersection = qTokens.intersect(cTokens).size
                    if (intersection.toFloat() / qTokens.size >= 0.7f) return true
                    val union = qTokens.union(cTokens).size
                    if (intersection.toFloat() / union >= 0.4f) return true
                }
            }
            return false
        }

        private fun normalizeForComparison(s: String): String {
            return s.lowercase()
                .replace(Regex("""[^a-z0-9\s]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }
    }
}
