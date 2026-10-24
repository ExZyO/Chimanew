// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
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

    fun getCached(mangaId: String): MangadexMediaArtwork? = idCache[mangaId]

    fun getCachedByTitle(rawTitle: String): MangadexMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchCoversByMangaId(mangaId: String): MangadexMediaArtwork? {
        idCache[mangaId]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val url = "$API_URL/cover?manga[]=$mangaId&limit=50&order[volume]=desc"
                val headers = Headers.Builder()
                    .add("User-Agent", "Chimanew/2.6.0")
                    .build()

                val response = client.newCall(GET(url, headers)).awaitSuccess()
                val body = response.body.string()
                val root = json.parseToJsonElement(body).jsonObject
                val dataArr = root["data"]?.jsonArray ?: return@withContext null

                val covers = dataArr.mapNotNull { item ->
                    val attrs = item.jsonObject["attributes"]?.jsonObject ?: return@mapNotNull null
                    val fileName = attrs["fileName"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val volume = attrs["volume"]?.jsonPrimitive?.content
                    val locale = attrs["locale"]?.jsonPrimitive?.content
                    val coverUrl = "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
                    MangadexCoverArt(volume, locale, coverUrl)
                }

                val artwork = MangadexMediaArtwork(mangaId, covers)
                idCache[mangaId] = artwork
                artwork
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to fetch MangaDex covers for ID: $mangaId" }
                null
            }
        }
    }

    suspend fun fetchCoversByTitle(rawTitle: String): MangadexMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        titleCache[title]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(title, "UTF-8")
                val searchUrl = "$API_URL/manga?title=$encoded&limit=5&order[relevance]=desc"
                val headers = Headers.Builder()
                    .add("User-Agent", "Chimanew/2.6.0")
                    .build()

                val searchResponse = client.newCall(GET(searchUrl, headers)).awaitSuccess()
                val searchBody = searchResponse.body.string()
                val searchRoot = json.parseToJsonElement(searchBody).jsonObject
                val dataArr = searchRoot["data"]?.jsonArray ?: return@withContext null
                if (dataArr.isEmpty()) {
                    val empty = MangadexMediaArtwork()
                    titleCache[title] = empty
                    return@withContext null
                }

                val matchedMangaId = dataArr.firstNotNullOfOrNull { item ->
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@firstNotNullOfOrNull null
                    val attrs = obj["attributes"]?.jsonObject ?: return@firstNotNullOfOrNull null
                    val titleMap = attrs["title"]?.jsonObject
                    val mainTitles = titleMap?.values?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val altTitlesList = attrs["altTitles"]?.jsonArray?.flatMap { altObj ->
                        altObj.jsonObject.values.mapNotNull { it.jsonPrimitive.contentOrNull }
                    } ?: emptyList()

                    val allCandidateTitles = mainTitles + altTitlesList
                    if (isTitleMatch(title, allCandidateTitles)) id else null
                }

                if (matchedMangaId == null) {
                    val empty = MangadexMediaArtwork()
                    titleCache[title] = empty
                    return@withContext null
                }

                val result = fetchCoversByMangaId(matchedMangaId) ?: MangadexMediaArtwork(matchedMangaId, emptyList())
                titleCache[title] = result
                result
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to fetch MangaDex manga search for title: $title" }
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://api.mangadex.org"

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
