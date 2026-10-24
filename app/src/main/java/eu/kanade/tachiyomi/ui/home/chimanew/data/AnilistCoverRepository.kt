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
    val score10: Float? = null,
    val score100: Int? = null,
)

class AnilistCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val malCache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val titleCache = ConcurrentHashMap<String, AnilistMediaArtwork>()

    fun getCached(remoteId: Long): AnilistMediaArtwork? = cache[remoteId]

    fun getCachedByMalId(malId: Long): AnilistMediaArtwork? = malCache[malId]

    fun getCachedByTitle(rawTitle: String): AnilistMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchByMalId(malId: Long): AnilistMediaArtwork? {
        if (malId <= 0) return null
        malCache[malId]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val query = """
                    query (${'$'}idMal: Int) {
                        Media(idMal: ${'$'}idMal, type: MANGA) {
                            id
                            idMal
                            bannerImage
                            coverImage {
                                extraLarge
                                large
                            }
                            averageScore
                            meanScore
                        }
                    }
                """.trimIndent()

                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("idMal", malId)
                    }
                }

                val request = POST(
                    API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                )

                val response = client.newCall(request).awaitSuccess()
                val bodyString = response.body.string()
                val root = json.parseToJsonElement(bodyString).jsonObject
                val media = root["data"]?.jsonObject?.get("Media")?.jsonObject

                if (media != null) {
                    val id = media["id"]?.jsonPrimitive?.content?.toLongOrNull()
                    val banner = media["bannerImage"]?.jsonPrimitive?.content
                    val coverObj = media["coverImage"]?.jsonObject
                    val extraLarge = coverObj?.get("extraLarge")?.jsonPrimitive?.content
                        ?: coverObj?.get("large")?.jsonPrimitive?.content
                    val avgScore = media["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val score100 = avgScore ?: meanScore
                    val score10 = score100?.let { it / 10f }

                    val artwork = AnilistMediaArtwork(
                        bannerUrl = banner?.ifBlank { null },
                        extraLargeCoverUrl = extraLarge?.ifBlank { null },
                        score10 = score10,
                        score100 = score100,
                    )
                    malCache[malId] = artwork
                    if (id != null) {
                        cache[id] = artwork
                    }
                    artwork
                } else {
                    val empty = AnilistMediaArtwork()
                    malCache[malId] = empty
                    empty
                }
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to fetch AniList artwork for MAL ID: $malId" }
                null
            }
        }
    }

    suspend fun fetchBatchByMalIds(malIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = malIds.distinct().filter { it > 0 && !malCache.containsKey(it) }
        if (uncached.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                uncached.chunked(50).forEach { chunk ->
                    try {
                        val query = """
                            query (${'$'}ids: [Int]) {
                                Page(perPage: 50) {
                                    media(idMal_in: ${'$'}ids, type: MANGA) {
                                        id
                                        idMal
                                        bannerImage
                                        coverImage {
                                            extraLarge
                                            large
                                        }
                                        averageScore
                                        meanScore
                                    }
                                }
                            }
                        """.trimIndent()

                        val payload = buildJsonObject {
                            put("query", query)
                            putJsonObject("variables") {
                                putJsonArray("ids") {
                                    chunk.forEach { add(it) }
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
                            val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull()
                            val idMal = obj["idMal"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@forEach
                            val banner = obj["bannerImage"]?.jsonPrimitive?.content
                            val coverObj = obj["coverImage"]?.jsonObject
                            val extraLarge = coverObj?.get("extraLarge")?.jsonPrimitive?.content
                                ?: coverObj?.get("large")?.jsonPrimitive?.content
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val score100 = avgScore ?: meanScore
                            val score10 = score100?.let { it / 10f }

                            val art = AnilistMediaArtwork(
                                bannerUrl = banner?.ifBlank { null },
                                extraLargeCoverUrl = extraLarge?.ifBlank { null },
                                score10 = score10,
                                score100 = score100,
                            )
                            malCache[idMal] = art
                            if (id != null) {
                                cache[id] = art
                            }
                        }

                        if (mediaList != null) {
                            chunk.forEach { id ->
                                malCache.putIfAbsent(id, AnilistMediaArtwork())
                            }
                        }
                    } catch (e: Exception) {
                        logcat(throwable = e) { "Failed to fetch AniList artwork batch for MAL IDs: $chunk" }
                    }
                }
            }
        }
        return malIds.associateWith { malCache[it] ?: AnilistMediaArtwork() }
    }

    suspend fun fetchBatch(remoteIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = remoteIds.distinct().filter { !cache.containsKey(it) }
        if (uncached.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                uncached.chunked(50).forEach { chunk ->
                    try {
                        val query = """
                            query (${'$'}ids: [Int]) {
                                Page(perPage: 50) {
                                    media(id_in: ${'$'}ids) {
                                        id
                                        bannerImage
                                        coverImage {
                                            extraLarge
                                            large
                                        }
                                        averageScore
                                        meanScore
                                    }
                                }
                            }
                        """.trimIndent()

                        val payload = buildJsonObject {
                            put("query", query)
                            putJsonObject("variables") {
                                putJsonArray("ids") {
                                    chunk.forEach { add(it) }
                                }
                            }
                        }

                        val request = POST(
                            API_URL,
                            body = payload.toString().toRequestBody(jsonMime),
                        )

                        val response = client.newCall(request).awaitSuccess()
                        val bodyString = response.body?.string() ?: return@forEach
                        val root = json.parseToJsonElement(bodyString).jsonObject
                        val mediaList = root["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray

                        mediaList?.forEach { element ->
                            val obj = element.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@forEach
                            val banner = obj["bannerImage"]?.jsonPrimitive?.content
                            val coverObj = obj["coverImage"]?.jsonObject
                            val extraLarge = coverObj?.get("extraLarge")?.jsonPrimitive?.content
                                ?: coverObj?.get("large")?.jsonPrimitive?.content
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val score100 = avgScore ?: meanScore
                            val score10 = score100?.let { it / 10f }

                            cache[id] = AnilistMediaArtwork(
                                bannerUrl = banner?.ifBlank { null },
                                extraLargeCoverUrl = extraLarge?.ifBlank { null },
                                score10 = score10,
                                score100 = score100,
                            )
                        }

                        // Mark any unreturned IDs as empty so we don't repeat queries endlessly on successful response
                        if (mediaList != null) {
                            chunk.forEach { id ->
                                cache.putIfAbsent(id, AnilistMediaArtwork())
                            }
                        }
                    } catch (e: Exception) {
                        logcat(throwable = e) { "Failed to fetch AniList artwork batch for $chunk" }
                    }
                }
            }
        }
        return remoteIds.associateWith { cache[it] ?: AnilistMediaArtwork() }
    }

    suspend fun fetchByTitle(rawTitle: String): AnilistMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        titleCache[title]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val query = """
                    query (${'$'}title: String) {
                        Media(search: ${'$'}title) {
                            id
                            bannerImage
                            coverImage {
                                extraLarge
                                large
                            }
                            averageScore
                            meanScore
                        }
                    }
                """.trimIndent()

                val payload = buildJsonObject {
                    put("query", query)
                    putJsonObject("variables") {
                        put("title", title)
                    }
                }

                val request = POST(
                    API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                )

                val response = client.newCall(request).awaitSuccess()
                val bodyString = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(bodyString).jsonObject
                val media = root["data"]?.jsonObject?.get("Media")?.jsonObject

                if (media != null) {
                    val id = media["id"]?.jsonPrimitive?.content?.toLongOrNull()
                    val banner = media["bannerImage"]?.jsonPrimitive?.content
                    val coverObj = media["coverImage"]?.jsonObject
                    val extraLarge = coverObj?.get("extraLarge")?.jsonPrimitive?.content
                        ?: coverObj?.get("large")?.jsonPrimitive?.content
                    val avgScore = media["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val score100 = avgScore ?: meanScore
                    val score10 = score100?.let { it / 10f }

                    val artwork = AnilistMediaArtwork(
                        bannerUrl = banner?.ifBlank { null },
                        extraLargeCoverUrl = extraLarge?.ifBlank { null },
                        score10 = score10,
                        score100 = score100,
                    )
                    titleCache[title] = artwork
                    if (id != null) {
                        cache[id] = artwork
                    }
                    artwork
                } else {
                    val empty = AnilistMediaArtwork()
                    titleCache[title] = empty
                    empty
                }
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to fetch AniList artwork for title: $title" }
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://graphql.anilist.co"

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
