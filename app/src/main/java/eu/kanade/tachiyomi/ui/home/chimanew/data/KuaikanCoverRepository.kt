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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import logcat.LogPriority
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

                val root = json.parseToJsonElement(bodyString) as? JsonObject ?: return@withContext null
                val hits = (root["data"] as? JsonObject)?.get("hit") as? JsonArray

                if (!hits.isNullOrEmpty()) {
                    val queryClean = query.replace(" ", "").lowercase()
                    val matchedObj = hits.firstNotNullOfOrNull { elem ->
                        val obj = elem as? JsonObject ?: return@firstNotNullOfOrNull null
                        val name = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty().replace(" ", "").lowercase()
                        if (name == queryClean || name.contains(queryClean) || queryClean.contains(name)) obj else null
                    } ?: hits.firstOrNull() as? JsonObject ?: return@withContext null

                    val topicId = matchedObj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val rawBanner = matchedObj["cover_image_url"]?.jsonPrimitive?.contentOrNull
                    val rawCover = matchedObj["vertical_image_url"]?.jsonPrimitive?.contentOrNull
                    val matchedTitle = matchedObj["title"]?.jsonPrimitive?.contentOrNull

                    val cleanBanner = rawBanner?.let { stripResolutionSuffix(it) }
                    val cleanCover = rawCover?.let { stripResolutionSuffix(it) }

                    val artwork = KuaikanArtwork(
                        bannerUrl = cleanBanner?.ifBlank { null },
                        coverUrl = cleanCover?.ifBlank { null },
                        title = matchedTitle,
                        topicId = topicId,
                    )
                    cache[key] = artwork
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kuaikan") {
                        "Found Kuaikan artwork for '$query': banner=$cleanBanner, cover=$cleanCover"
                    }
                    logcat(priority = LogPriority.INFO) { "Fetched Kuaikan artwork for '$query': cover=$cleanCover, banner=$cleanBanner" }
                    artwork
                } else {
                    val empty = KuaikanArtwork()
                    cache[key] = empty
                    ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kuaikan") {
                        "No Kuaikan artwork hits for '$query'"
                    }
                    empty
                }
            } catch (e: Exception) {
                ChimanewDiagnostics.log(DiagnosticCategory.NETWORK, "Kuaikan", e) {
                    "Failed to fetch Kuaikan artwork for '$query': ${e.message}"
                }
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    logcat(priority = LogPriority.WARN, throwable = e) { "Kuaikan Manhua rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(priority = LogPriority.INFO, throwable = e) { "Failed to fetch Kuaikan Manhua artwork for query: $query" }
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
