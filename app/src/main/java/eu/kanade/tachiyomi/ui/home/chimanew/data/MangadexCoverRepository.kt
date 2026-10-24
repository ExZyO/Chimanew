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
                    .add("User-Agent", "Chimanew/2.5.8")
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
                val searchUrl = "$API_URL/manga?title=$encoded&limit=1"
                val headers = Headers.Builder()
                    .add("User-Agent", "Chimanew/2.5.8")
                    .build()

                val searchResponse = client.newCall(GET(searchUrl, headers)).awaitSuccess()
                val searchBody = searchResponse.body.string()
                val searchRoot = json.parseToJsonElement(searchBody).jsonObject
                val dataArr = searchRoot["data"]?.jsonArray ?: return@withContext null
                if (dataArr.isEmpty()) {
                    titleCache[title] = MangadexMediaArtwork()
                    return@withContext null
                }

                val mangaId = dataArr[0].jsonObject["id"]?.jsonPrimitive?.content ?: return@withContext null
                val result = fetchCoversByMangaId(mangaId) ?: MangadexMediaArtwork(mangaId, emptyList())
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
    }
}
