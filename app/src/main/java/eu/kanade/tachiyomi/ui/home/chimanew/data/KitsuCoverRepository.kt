// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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

data class KitsuMediaArtwork(
    val bannerUrl: String? = null,
    val posterUrl: String? = null,
    val score10: Float? = null,
    val score100: Int? = null,
)

class KitsuCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val titleCache = ConcurrentHashMap<String, KitsuMediaArtwork>()
    private val idCache = ConcurrentHashMap<Long, KitsuMediaArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean = System.currentTimeMillis() < rateLimitedUntil

    fun getCached(kitsuId: Long): KitsuMediaArtwork? = idCache[kitsuId]

    fun getCachedByTitle(rawTitle: String): KitsuMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchByKitsuId(kitsuId: Long): KitsuMediaArtwork? {
        idCache[kitsuId]?.let { return it }
        if (isRateLimited()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = "$API_URL/manga/$kitsuId"
                val headers = Headers.Builder()
                    .add("Accept", "application/vnd.api+json")
                    .add("User-Agent", "Chimanew/2.6.1")
                    .build()

                val response = client.newCall(GET(url, headers)).awaitSuccess()
                val body = response.body.string()
                val root = json.parseToJsonElement(body).jsonObject
                val dataObj = root["data"]?.jsonObject ?: return@withContext null
                val attributes = dataObj["attributes"]?.jsonObject ?: return@withContext null

                val bannerUrl = attributes["coverImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }
                val posterUrl = attributes["posterImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }
                val avgRating = attributes["averageRating"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                val score10 = avgRating?.let { it / 10f }
                val score100 = avgRating?.toInt()

                val artwork = KitsuMediaArtwork(bannerUrl, posterUrl, score10, score100)
                idCache[kitsuId] = artwork
                artwork
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                }
                logcat(throwable = e) { "Failed to fetch Kitsu artwork for ID: $kitsuId" }
                null
            }
        }
    }

    suspend fun fetchByTitle(rawTitle: String): KitsuMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        titleCache[title]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(title, "UTF-8")
                val url = "$API_URL/manga?filter[text]=$encoded&page[limit]=5"
                val headers = Headers.Builder()
                    .add("Accept", "application/vnd.api+json")
                    .add("User-Agent", "Chimanew/2.6.1")
                    .build()

                val response = client.newCall(GET(url, headers)).awaitSuccess()
                val body = response.body.string()
                val root = json.parseToJsonElement(body).jsonObject
                val dataArr = root["data"]?.jsonArray ?: return@withContext null
                if (dataArr.isEmpty()) {
                    val empty = KitsuMediaArtwork()
                    titleCache[title] = empty
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kitsu") {
                        "No Kitsu results returned for '$title'"
                    }
                    return@withContext null
                }

                val candidateSummaries = mutableListOf<String>()
                val matchedObj = dataArr.firstNotNullOfOrNull { item ->
                    val attrs = item.jsonObject["attributes"]?.jsonObject ?: return@firstNotNullOfOrNull null
                    val canonical = attrs["canonicalTitle"]?.jsonPrimitive?.contentOrNull
                    val titlesList = attrs["titles"]?.jsonObject?.values?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val abbrevList = attrs["abbreviatedTitles"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val candidateTitles = listOfNotNull(canonical) + titlesList + abbrevList

                    candidateSummaries.add(canonical ?: "unknown")
                    if (isTitleMatch(title, candidateTitles)) attrs else null
                }

                if (matchedObj == null) {
                    val empty = KitsuMediaArtwork()
                    titleCache[title] = empty
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kitsu") {
                        "No title match for '$title' among Kitsu candidates: $candidateSummaries"
                    }
                    return@withContext null
                }

                val bannerUrl = matchedObj["coverImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }
                val posterUrl = matchedObj["posterImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }
                val avgRating = matchedObj["averageRating"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                val score10 = avgRating?.let { it / 10f }
                val score100 = avgRating?.toInt()

                val artwork = KitsuMediaArtwork(bannerUrl, posterUrl, score10, score100)
                titleCache[title] = artwork
                ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kitsu") {
                    "Found Kitsu match for '$title': canonical='${matchedObj["canonicalTitle"]?.jsonPrimitive?.contentOrNull}', banner=${!bannerUrl.isNullOrBlank()}, poster=${!posterUrl.isNullOrBlank()}"
                }
                artwork
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kitsu") {
                        "Kitsu rate limited (429). Pausing calls for 60s."
                    }
                } else {
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kitsu", e) {
                        "Failed Kitsu search for '$title': ${e.message}"
                    }
                }
                logcat(throwable = e) { "Failed to fetch Kitsu artwork for title: $title" }
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://kitsu.io/api/edge"

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
            val qTokens = qNorm.split(" ").filter { it.length > 2 }.toSet()

            for (candidate in candidateTitles) {
                val cNorm = normalizeForComparison(candidate)
                if (cNorm.isBlank()) continue
                if (qNorm == cNorm) return true
                if (qNorm.contains(cNorm) || cNorm.contains(qNorm)) {
                    val minLen = minOf(qNorm.length, cNorm.length)
                    val maxLen = maxOf(qNorm.length, cNorm.length)
                    if (minLen.toFloat() / maxLen >= 0.55f) return true
                }
                val cTokens = cNorm.split(" ").filter { it.length > 2 }.toSet()
                if (qTokens.isNotEmpty() && cTokens.isNotEmpty()) {
                    val intersection = qTokens.intersect(cTokens).size
                    val union = qTokens.union(cTokens).size
                    if (intersection.toFloat() / union >= 0.5f) return true
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
