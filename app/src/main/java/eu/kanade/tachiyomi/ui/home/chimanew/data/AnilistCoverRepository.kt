// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
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

data class AnilistMediaArtwork(
    val bannerUrl: String? = null,
    val extraLargeCoverUrl: String? = null,
)

class AnilistCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<Long, AnilistMediaArtwork>()

    fun getCached(remoteId: Long): AnilistMediaArtwork? = cache[remoteId]

    suspend fun fetchBatch(remoteIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = remoteIds.distinct().filter { it !in cache }
        if (uncached.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val query = """
                        query (${'$'}ids: [Int]) {
                            Page(perPage: 50) {
                                media(id_in: ${'$'}ids, type: MANGA) {
                                    id
                                    bannerImage
                                    coverImage {
                                        extraLarge
                                        large
                                    }
                                }
                            }
                        }
                    """.trimIndent()

                    val payload = buildJsonObject {
                        put("query", query)
                        putJsonObject("variables") {
                            putJsonArray("ids") {
                                uncached.forEach { add(it) }
                            }
                        }
                    }

                    val request = POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    )

                    val response = client.newCall(request).awaitSuccess()
                    val bodyString = response.body.string()
                    val root = json.parseToJsonElement(bodyString).jsonObject
                    val mediaList = root["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray

                    mediaList?.forEach { element ->
                        val obj = element.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@forEach
                        val banner = obj["bannerImage"]?.jsonPrimitive?.content
                        val coverObj = obj["coverImage"]?.jsonObject
                        val extraLarge = coverObj?.get("extraLarge")?.jsonPrimitive?.content
                            ?: coverObj?.get("large")?.jsonPrimitive?.content

                        cache[id] = AnilistMediaArtwork(
                            bannerUrl = banner?.ifBlank { null },
                            extraLargeCoverUrl = extraLarge?.ifBlank { null },
                        )
                    }
                } catch (e: Exception) {
                    logcat(throwable = e) { "Failed to fetch AniList artwork batch for $uncached" }
                }
            }
        }
        return remoteIds.associateWith { cache[it] ?: AnilistMediaArtwork() }
    }

    companion object {
        private const val API_URL = "https://graphql.anilist.co"
    }
}
