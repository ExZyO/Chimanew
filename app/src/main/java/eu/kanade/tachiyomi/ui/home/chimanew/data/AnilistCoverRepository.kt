// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
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
    val countryOfOrigin: String? = null,
    val nativeTitle: String? = null,
    val romajiTitle: String? = null,
    val englishTitle: String? = null,
    val synonyms: List<String> = emptyList(),
)

class AnilistCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val malCache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val titleCache = ConcurrentHashMap<String, AnilistMediaArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    private fun handleException(e: Exception, context: String) {
        val msg = e.message.orEmpty()
        if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
            rateLimitedUntil = System.currentTimeMillis() + 60_000L
            logcat(throwable = e) { "AniList rate limited (429) during $context. Pausing calls for 60s." }
        } else {
            logcat(throwable = e) { "Failed to fetch AniList artwork during $context" }
        }
    }

    private fun JsonElement?.asStringOrNull(): String? {
        if (this == null || this is JsonNull) return null
        return (this as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
    }

    fun getCached(remoteId: Long): AnilistMediaArtwork? = cache[remoteId]

    fun getCachedByMalId(malId: Long): AnilistMediaArtwork? = malCache[malId]

    fun getCachedByTitle(rawTitle: String): AnilistMediaArtwork? = titleCache[cleanTitle(rawTitle)]

    suspend fun fetchByMalId(malId: Long): AnilistMediaArtwork? {
        if (malId <= 0) return null
        malCache[malId]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val query = """
                    query (${'$'}idMal: Int) {
                        Media(idMal: ${'$'}idMal, type: MANGA) {
                            id
                            idMal
                            countryOfOrigin
                            title {
                                english
                                romaji
                                native
                            }
                            synonyms
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
                    val country = media["countryOfOrigin"].asStringOrNull()
                    val titleObj = media["title"]?.jsonObject
                    val english = titleObj?.get("english").asStringOrNull()
                    val romaji = titleObj?.get("romaji").asStringOrNull()
                    val native = titleObj?.get("native").asStringOrNull()
                    val synonymsList = media["synonyms"]?.jsonArray?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                    val banner = media["bannerImage"].asStringOrNull()
                    val coverObj = media["coverImage"]?.jsonObject
                    val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                        ?: coverObj?.get("large").asStringOrNull()
                    val avgScore = media["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val score100 = avgScore ?: meanScore
                    val score10 = score100?.let { it / 10f }

                    val artwork = AnilistMediaArtwork(
                        bannerUrl = banner,
                        extraLargeCoverUrl = extraLarge,
                        score10 = score10,
                        score100 = score100,
                        countryOfOrigin = country,
                        nativeTitle = native,
                        romajiTitle = romaji,
                        englishTitle = english,
                        synonyms = synonymsList,
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
                handleException(e, "fetchByMalId($malId)")
                null
            }
        }
    }

    suspend fun fetchBatchByMalIds(malIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = malIds.distinct().filter { it > 0 && !malCache.containsKey(it) }
        if (uncached.isNotEmpty() && !isRateLimited()) {
            withContext(Dispatchers.IO) {
                uncached.chunked(50).forEach { chunk ->
                    if (isRateLimited()) return@forEach
                    try {
                        val query = """
                            query (${'$'}ids: [Int]) {
                                Page(perPage: 50) {
                                    media(idMal_in: ${'$'}ids, type: MANGA) {
                                        id
                                        idMal
                                        countryOfOrigin
                                        title {
                                            english
                                            romaji
                                            native
                                        }
                                        synonyms
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
                            val country = obj["countryOfOrigin"].asStringOrNull()
                            val titleObj = obj["title"]?.jsonObject
                            val english = titleObj?.get("english").asStringOrNull()
                            val romaji = titleObj?.get("romaji").asStringOrNull()
                            val native = titleObj?.get("native").asStringOrNull()
                            val synonymsList = obj["synonyms"]?.jsonArray?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val banner = obj["bannerImage"].asStringOrNull()
                            val coverObj = obj["coverImage"]?.jsonObject
                            val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                                ?: coverObj?.get("large").asStringOrNull()
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val score100 = avgScore ?: meanScore
                            val score10 = score100?.let { it / 10f }

                            val art = AnilistMediaArtwork(
                                bannerUrl = banner,
                                extraLargeCoverUrl = extraLarge,
                                score10 = score10,
                                score100 = score100,
                                countryOfOrigin = country,
                                nativeTitle = native,
                                romajiTitle = romaji,
                                englishTitle = english,
                                synonyms = synonymsList,
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
                        handleException(e, "fetchBatchByMalIds($chunk)")
                    }
                }
            }
        }
        return malIds.associateWith { malCache[it] ?: AnilistMediaArtwork() }
    }

    suspend fun fetchBatch(remoteIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = remoteIds.distinct().filter { !cache.containsKey(it) }
        if (uncached.isNotEmpty() && !isRateLimited()) {
            withContext(Dispatchers.IO) {
                uncached.chunked(50).forEach { chunk ->
                    if (isRateLimited()) return@forEach
                    try {
                        val query = """
                            query (${'$'}ids: [Int]) {
                                Page(perPage: 50) {
                                    media(id_in: ${'$'}ids, type: MANGA) {
                                        id
                                        countryOfOrigin
                                        title {
                                            english
                                            romaji
                                            native
                                        }
                                        synonyms
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
                            val country = obj["countryOfOrigin"].asStringOrNull()
                            val titleObj = obj["title"]?.jsonObject
                            val english = titleObj?.get("english").asStringOrNull()
                            val romaji = titleObj?.get("romaji").asStringOrNull()
                            val native = titleObj?.get("native").asStringOrNull()
                            val synonymsList = obj["synonyms"]?.jsonArray?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val banner = obj["bannerImage"].asStringOrNull()
                            val coverObj = obj["coverImage"]?.jsonObject
                            val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                                ?: coverObj?.get("large").asStringOrNull()
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                            val score100 = avgScore ?: meanScore
                            val score10 = score100?.let { it / 10f }

                            cache[id] = AnilistMediaArtwork(
                                bannerUrl = banner,
                                extraLargeCoverUrl = extraLarge,
                                score10 = score10,
                                score100 = score100,
                                countryOfOrigin = country,
                                nativeTitle = native,
                                romajiTitle = romaji,
                                englishTitle = english,
                                synonyms = synonymsList,
                            )
                        }

                        // Mark any unreturned IDs as empty so we don't repeat queries endlessly on successful response
                        if (mediaList != null) {
                            chunk.forEach { id ->
                                cache.putIfAbsent(id, AnilistMediaArtwork())
                            }
                        }
                    } catch (e: Exception) {
                        handleException(e, "fetchBatch($chunk)")
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
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val query = """
                    query (${'$'}title: String) {
                        Media(search: ${'$'}title, type: MANGA) {
                            id
                            countryOfOrigin
                            title {
                                english
                                romaji
                                native
                            }
                            synonyms
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
                    val country = media["countryOfOrigin"].asStringOrNull()
                    val titleObj = media["title"]?.jsonObject
                    val english = titleObj?.get("english").asStringOrNull()
                    val romaji = titleObj?.get("romaji").asStringOrNull()
                    val native = titleObj?.get("native").asStringOrNull()
                    val synonymsList = media["synonyms"]?.jsonArray?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                    val banner = media["bannerImage"].asStringOrNull()
                    val coverObj = media["coverImage"]?.jsonObject
                    val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                        ?: coverObj?.get("large").asStringOrNull()
                    val avgScore = media["averageScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.content?.toIntOrNull()
                    val score100 = avgScore ?: meanScore
                    val score10 = score100?.let { it / 10f }

                    val artwork = AnilistMediaArtwork(
                        bannerUrl = banner,
                        extraLargeCoverUrl = extraLarge,
                        score10 = score10,
                        score100 = score100,
                        countryOfOrigin = country,
                        nativeTitle = native,
                        romajiTitle = romaji,
                        englishTitle = english,
                        synonyms = synonymsList,
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
                handleException(e, "fetchByTitle($title)")
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
