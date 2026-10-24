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
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class KuaikanArtwork(
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val title: String? = null,
    val topicId: Long? = null,
)

class KuaikanCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, KuaikanArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    fun getCached(query: String): KuaikanArtwork? = cache[normalizeKey(query)]

    suspend fun fetchArtwork(query: String): KuaikanArtwork? {
        val key = normalizeKey(query)
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                val searchUrl = "https://api.kuaikanmanhua.com/v1/search/topic?q=$encoded&size=5"
                val request = GET(
                    searchUrl,
                    headers = okhttp3.Headers.Builder()
                        .add("User-Agent", USER_AGENT)
                        .build(),
                )

                val response = client.newCall(request).awaitSuccess()
                val bodyString = response.body.string()
                if (bodyString.isBlank()) return@withContext null

                val root = json.parseToJsonElement(bodyString).jsonObject
                val hits = root["data"]?.jsonObject?.get("hit")?.jsonArray

                if (!hits.isNullOrEmpty()) {
                    val queryClean = query.replace(" ", "").lowercase()
                    val matchedObj = hits.firstOrNull { elem ->
                        val name = elem.jsonObject["title"]?.jsonPrimitive?.content.orEmpty().replace(" ", "").lowercase()
                        name == queryClean || name.contains(queryClean)
                    }?.jsonObject ?: hits.first().jsonObject

                    val topicId = matchedObj["id"]?.jsonPrimitive?.content?.toLongOrNull()
                    val rawBanner = matchedObj["cover_image_url"]?.jsonPrimitive?.content
                    val rawCover = matchedObj["vertical_image_url"]?.jsonPrimitive?.content
                    val matchedTitle = matchedObj["title"]?.jsonPrimitive?.content

                    val cleanBanner = rawBanner?.let { stripResolutionSuffix(it) }
                    val cleanCover = rawCover?.let { stripResolutionSuffix(it) }

                    val artwork = KuaikanArtwork(
                        bannerUrl = cleanBanner?.ifBlank { null },
                        coverUrl = cleanCover?.ifBlank { null },
                        title = matchedTitle,
                        topicId = topicId,
                    )
                    cache[key] = artwork
                    artwork
                } else {
                    val empty = KuaikanArtwork()
                    cache[key] = empty
                    empty
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    logcat(throwable = e) { "Kuaikan Manhua rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(throwable = e) { "Failed to fetch Kuaikan Manhua artwork for query: $query" }
                }
                null
            }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val RES_SUFFIX_REGEX = Regex("""-w\d+(\.w)?$""")

        fun stripResolutionSuffix(url: String): String {
            return url.replace(RES_SUFFIX_REGEX, "")
        }

        fun normalizeKey(query: String): String {
            return query.trim().lowercase()
        }
    }
}
