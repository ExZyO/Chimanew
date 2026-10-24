// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
)

class KitsuCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val titleCache = ConcurrentHashMap<String, KitsuMediaArtwork>()
    private val idCache = ConcurrentHashMap<Long, KitsuMediaArtwork>()

    fun getCached(kitsuId: Long): KitsuMediaArtwork? = idCache[kitsuId]

    fun getCachedByTitle(rawTitle: String): KitsuMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchByKitsuId(kitsuId: Long): KitsuMediaArtwork? {
        idCache[kitsuId]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val url = "$API_URL/manga/$kitsuId"
                val headers = Headers.Builder()
                    .add("Accept", "application/vnd.api+json")
                    .add("User-Agent", "Chimanew/2.5.8")
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

                val artwork = KitsuMediaArtwork(bannerUrl, posterUrl)
                idCache[kitsuId] = artwork
                artwork
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to fetch Kitsu artwork for ID: $kitsuId" }
                null
            }
        }
    }

    suspend fun fetchByTitle(rawTitle: String): KitsuMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        titleCache[title]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(title, "UTF-8")
                val url = "$API_URL/manga?filter[text]=$encoded&page[limit]=1"
                val headers = Headers.Builder()
                    .add("Accept", "application/vnd.api+json")
                    .add("User-Agent", "Chimanew/2.5.8")
                    .build()

                val response = client.newCall(GET(url, headers)).awaitSuccess()
                val body = response.body.string()
                val root = json.parseToJsonElement(body).jsonObject
                val dataArr = root["data"]?.jsonArray ?: return@withContext null
                if (dataArr.isEmpty()) {
                    titleCache[title] = KitsuMediaArtwork()
                    return@withContext null
                }

                val first = dataArr[0].jsonObject
                val attributes = first["attributes"]?.jsonObject ?: return@withContext null

                val bannerUrl = attributes["coverImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }
                val posterUrl = attributes["posterImage"]?.jsonObject?.let {
                    it["large"]?.jsonPrimitive?.content ?: it["original"]?.jsonPrimitive?.content
                }

                val artwork = KitsuMediaArtwork(bannerUrl, posterUrl)
                titleCache[title] = artwork
                artwork
            } catch (e: Exception) {
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
    }
}
