// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

data class BangumiArtwork(
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val title: String? = null,
    val score10: Float? = null,
    val score100: Int? = null,
    val subjectId: Long? = null,
)

class BangumiCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, BangumiArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    fun getCached(query: String): BangumiArtwork? = cache[normalizeKey(query)]

    suspend fun fetchArtwork(query: String): BangumiArtwork? {
        val key = normalizeKey(query)
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    put("keyword", query.trim())
                    putJsonObject("filter") {
                        putJsonArray("type") {
                            add(1) // 1 = Book / Manga
                        }
                    }
                }

                val request = POST(
                    API_URL,
                    headers = okhttp3.Headers.Builder()
                        .add("User-Agent", USER_AGENT)
                        .build(),
                    body = payload.toString().toRequestBody(jsonMime),
                )

                val response = client.newCall(request).awaitSuccess()
                val bodyString = response.body.string()
                if (bodyString.isBlank()) return@withContext null

                val root = json.parseToJsonElement(bodyString).jsonObject
                val data = root["data"]?.jsonArray

                if (!data.isNullOrEmpty()) {
                    val first = data.first().jsonObject
                    val subjectId = first["id"]?.jsonPrimitive?.content?.toLongOrNull()
                    val titleName = first["name"]?.jsonPrimitive?.content
                        ?: first["name_cn"]?.jsonPrimitive?.content

                    val imagesObj = first["images"]?.jsonObject
                    val coverUrl = imagesObj?.get("large")?.jsonPrimitive?.content
                        ?: imagesObj?.get("common")?.jsonPrimitive?.content
                        ?: first["image"]?.jsonPrimitive?.content

                    val ratingObj = first["rating"]?.jsonObject
                    val score = ratingObj?.get("score")?.jsonPrimitive?.floatOrNull
                    val score100 = score?.let { (it * 10).toInt() }

                    // Fetch anime adaptation visual if available for wide banner
                    var bannerUrl: String? = null
                    if (subjectId != null) {
                        try {
                            val relUrl = "https://api.bgm.tv/v0/subjects/$subjectId/subjects"
                            val relReq = GET(
                                relUrl,
                                headers = okhttp3.Headers.Builder().add("User-Agent", USER_AGENT).build(),
                            )
                            val relResp = client.newCall(relReq).awaitSuccess()
                            val relBody = relResp.body.string()
                            if (relBody.isNotBlank()) {
                                val relArray = json.parseToJsonElement(relBody).jsonArray
                                val animeSubject = relArray.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "2" }?.jsonObject
                                val animeImg = animeSubject?.get("images")?.jsonObject?.get("large")?.jsonPrimitive?.content
                                    ?: animeSubject?.get("images")?.jsonObject?.get("common")?.jsonPrimitive?.content
                                if (!animeImg.isNullOrBlank()) {
                                    bannerUrl = animeImg
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    if (bannerUrl.isNullOrBlank()) {
                        bannerUrl = coverUrl
                    }

                    val artwork = BangumiArtwork(
                        bannerUrl = bannerUrl?.ifBlank { null },
                        coverUrl = coverUrl?.ifBlank { null },
                        title = titleName,
                        score10 = score,
                        score100 = score100,
                        subjectId = subjectId,
                    )
                    cache[key] = artwork
                    artwork
                } else {
                    val empty = BangumiArtwork()
                    cache[key] = empty
                    empty
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    logcat(throwable = e) { "Bangumi rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(throwable = e) { "Failed to fetch Bangumi artwork for query: $query" }
                }
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://api.bgm.tv/v0/search/subjects"
        private const val USER_AGENT = "Chimanew/2.6.13 (Android; https://github.com/ExZyO/Chimanew)"

        fun normalizeKey(query: String): String {
            return query.trim().lowercase()
        }
    }
}
