// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

@Serializable
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

@Serializable
data class AnilistDiskCache(
    val byId: Map<Long, AnilistMediaArtwork> = emptyMap(),
    val malIdToId: Map<Long, Long> = emptyMap(),
    val titleToId: Map<String, Long> = emptyMap(),
    val unmatchedMalIds: List<Long> = emptyList(),
    val unmatchedTitles: List<String> = emptyList(),
    // Legacy backwards compatibility:
    val byMalId: Map<Long, AnilistMediaArtwork> = emptyMap(),
    val byTitle: Map<String, AnilistMediaArtwork> = emptyMap(),
)

class AnilistCoverRepository(
    rawClient: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
    private val context: Application = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val malCache = ConcurrentHashMap<Long, AnilistMediaArtwork>()
    private val titleCache = ConcurrentHashMap<String, AnilistMediaArtwork>()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val cacheFile by lazy { File(context.filesDir, "chimanew_anilist_title_cache.json") }
    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
        prettyPrint = false
    }
    private var saveJob: Job? = null

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun calculateResetDelayMs(resetHeader: Long?): Long? {
        if (resetHeader == null || resetHeader <= 0L) return null
        return if (resetHeader > 1_000_000_000L) {
            val diff = (resetHeader * 1000L) - System.currentTimeMillis()
            diff.coerceIn(1_000L, 65_000L)
        } else {
            (resetHeader * 1000L).coerceIn(1_000L, 65_000L)
        }
    }

    private val aniListClient: OkHttpClient by lazy {
        rawClient.newBuilder()
            .rateLimit(permits = 30, period = 1.minutes)
            .addInterceptor { chain ->
                val wait = rateLimitedUntil - System.currentTimeMillis()
                if (wait > 0) {
                    try {
                        Thread.sleep(wait.coerceAtMost(65_000L))
                    } catch (_: InterruptedException) {
                    }
                }

                val response = chain.proceed(chain.request())

                val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()
                val resetHeader = response.header("X-RateLimit-Reset")?.toLongOrNull()

                if (response.code == 429) {
                    val delayMs = calculateResetDelayMs(resetHeader) ?: 60_000L
                    rateLimitedUntil = System.currentTimeMillis() + delayMs
                    logcat { "AniList HTTP 429 received. Gas gauge pausing background queries for ${delayMs / 1000}s." }
                } else if (remaining != null && remaining < 15) {
                    val delayMs = calculateResetDelayMs(resetHeader) ?: 30_000L
                    rateLimitedUntil = System.currentTimeMillis() + delayMs
                    logcat { "AniList rate limit gas gauge low ($remaining left). Proactively pausing queries for ${delayMs / 1000}s." }
                }

                response
            }
            .build()
    }

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                if (content.isNotBlank()) {
                    val diskCache = jsonHelper.decodeFromString<AnilistDiskCache>(content)

                    // 1. Load canonical artworks
                    for ((id, art) in diskCache.byId) {
                        cache[id] = art
                    }

                    // 2. Link MAL IDs by memory reference
                    for ((malId, id) in diskCache.malIdToId) {
                        diskCache.byId[id]?.let { malCache[malId] = it }
                    }
                    for ((malId, art) in diskCache.byMalId) {
                        if (!malCache.containsKey(malId)) {
                            val canonical = diskCache.byId.values.firstOrNull { it.romajiTitle == art.romajiTitle && it.nativeTitle == art.nativeTitle } ?: art
                            malCache[malId] = canonical
                        }
                    }

                    // 3. Link Titles by memory reference
                    for ((title, id) in diskCache.titleToId) {
                        diskCache.byId[id]?.let { titleCache[title] = it }
                    }
                    for ((title, art) in diskCache.byTitle) {
                        if (!titleCache.containsKey(title)) {
                            val canonical = diskCache.byId.values.firstOrNull { it.romajiTitle == art.romajiTitle && it.nativeTitle == art.nativeTitle } ?: art
                            titleCache[title] = canonical
                        }
                    }

                    // 4. Link unmatched entries to singleton EMPTY_ARTWORK (0 heap overhead)
                    for (malId in diskCache.unmatchedMalIds) {
                        malCache[malId] = EMPTY_ARTWORK
                    }
                    for (title in diskCache.unmatchedTitles) {
                        titleCache[title] = EMPTY_ARTWORK
                    }

                    logcat { "Loaded optimized AniList cache: ${cache.size} canonical IDs, ${malCache.size} MAL mappings, ${titleCache.size} title mappings" }
                }
            }
        } catch (e: Exception) {
            logcat(throwable = e) { "Failed to load AniList persistent cache from disk" }
        }
    }

    private fun scheduleSaveToDisk() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500L)
            try {
                // Collect canonical non-empty artworks
                val nonNullArtworks = mutableMapOf<Long, AnilistMediaArtwork>()
                for ((id, art) in cache) {
                    if (art !== EMPTY_ARTWORK && (art.bannerUrl != null || art.extraLargeCoverUrl != null || art.romajiTitle != null)) {
                        nonNullArtworks[id] = art
                    }
                }

                val malIdMap = mutableMapOf<Long, Long>()
                val unmatchedMals = mutableListOf<Long>()
                for ((malId, art) in malCache) {
                    if (art === EMPTY_ARTWORK || (art.bannerUrl == null && art.extraLargeCoverUrl == null && art.romajiTitle == null)) {
                        unmatchedMals.add(malId)
                    } else {
                        val canonicalId = nonNullArtworks.entries.firstOrNull { it.value === art || (it.value.romajiTitle == art.romajiTitle && it.value.nativeTitle == art.nativeTitle) }?.key
                        if (canonicalId != null) {
                            malIdMap[malId] = canonicalId
                        } else {
                            nonNullArtworks[-malId] = art
                            malIdMap[malId] = -malId
                        }
                    }
                }

                val titleMap = mutableMapOf<String, Long>()
                val unmatchedTitles = mutableListOf<String>()
                for ((title, art) in titleCache) {
                    if (art === EMPTY_ARTWORK || (art.bannerUrl == null && art.extraLargeCoverUrl == null && art.romajiTitle == null)) {
                        unmatchedTitles.add(title)
                    } else {
                        val canonicalId = nonNullArtworks.entries.firstOrNull { it.value === art || (it.value.romajiTitle == art.romajiTitle && it.value.nativeTitle == art.nativeTitle) }?.key
                        if (canonicalId != null) {
                            titleMap[title] = canonicalId
                        } else {
                            val genId = -(title.hashCode().toLong().let { if (it == 0L) -1L else it })
                            nonNullArtworks[genId] = art
                            titleMap[title] = genId
                        }
                    }
                }

                val diskCache = AnilistDiskCache(
                    byId = nonNullArtworks,
                    malIdToId = malIdMap,
                    titleToId = titleMap,
                    unmatchedMalIds = unmatchedMals,
                    unmatchedTitles = unmatchedTitles,
                )
                val content = jsonHelper.encodeToString(diskCache)

                // Atomic write via temp file
                val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                tempFile.writeText(content)
                if (tempFile.exists()) {
                    if (cacheFile.exists()) {
                        cacheFile.delete()
                    }
                    tempFile.renameTo(cacheFile)
                }
            } catch (e: Exception) {
                logcat(throwable = e) { "Failed to save AniList persistent cache to disk" }
            }
        }
    }

    fun clearCache() {
        cache.clear()
        malCache.clear()
        titleCache.clear()
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (_: Exception) {}
        logcat { "Cleared AniList persistent disk and memory cache" }
    }

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    private suspend fun awaitRateLimit() {
        val wait = rateLimitedUntil - System.currentTimeMillis()
        if (wait > 0) {
            delay(wait.coerceAtMost(65_000L))
        }
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
        awaitRateLimit()

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

                val response = aniListClient.newCall(request).awaitSuccess()
                val bodyString = response.body.string()
                val root = json.parseToJsonElement(bodyString) as? JsonObject
                val media = (root?.get("data") as? JsonObject)?.get("Media") as? JsonObject

                if (media != null) {
                    val id = media["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val country = media["countryOfOrigin"].asStringOrNull()
                    val titleObj = media["title"] as? JsonObject
                    val english = titleObj?.get("english").asStringOrNull()
                    val romaji = titleObj?.get("romaji").asStringOrNull()
                    val native = titleObj?.get("native").asStringOrNull()
                    val synonymsList = (media["synonyms"] as? JsonArray)?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                    val banner = media["bannerImage"].asStringOrNull()
                    val coverObj = media["coverImage"] as? JsonObject
                    val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                        ?: coverObj?.get("large").asStringOrNull()
                    val avgScore = media["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
                    scheduleSaveToDisk()
                    artwork
                } else {
                    malCache[malId] = EMPTY_ARTWORK
                    scheduleSaveToDisk()
                    EMPTY_ARTWORK
                }
            } catch (e: Exception) {
                handleException(e, "fetchByMalId($malId)")
                null
            }
        }
    }

    suspend fun fetchBatchByMalIds(malIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = malIds.distinct().filter { it > 0 && !malCache.containsKey(it) }
        if (uncached.isNotEmpty()) {
            awaitRateLimit()
            withContext(Dispatchers.IO) {
                var addedAny = false
                uncached.chunked(50).forEach { chunk ->
                    awaitRateLimit()
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

                        val response = aniListClient.newCall(request).awaitSuccess()
                        val bodyString = response.body.string()
                        val root = json.parseToJsonElement(bodyString) as? JsonObject
                        val mediaList = ((root?.get("data") as? JsonObject)?.get("Page") as? JsonObject)?.get("media") as? JsonArray

                        mediaList?.forEach { element ->
                            val obj = element as? JsonObject ?: return@forEach
                            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                            val idMal = obj["idMal"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                            val country = obj["countryOfOrigin"].asStringOrNull()
                            val titleObj = obj["title"] as? JsonObject
                            val english = titleObj?.get("english").asStringOrNull()
                            val romaji = titleObj?.get("romaji").asStringOrNull()
                            val native = titleObj?.get("native").asStringOrNull()
                            val synonymsList = (obj["synonyms"] as? JsonArray)?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val banner = obj["bannerImage"].asStringOrNull()
                            val coverObj = obj["coverImage"] as? JsonObject
                            val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                                ?: coverObj?.get("large").asStringOrNull()
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
                            addedAny = true
                        }

                        if (mediaList != null) {
                            chunk.forEach { id ->
                                if (malCache.putIfAbsent(id, EMPTY_ARTWORK) == null) {
                                    addedAny = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        handleException(e, "fetchBatchByMalIds($chunk)")
                    }
                }
                if (addedAny) {
                    scheduleSaveToDisk()
                }
            }
        }
        return malIds.associateWith { malCache[it] ?: EMPTY_ARTWORK }
    }

    suspend fun fetchBatch(remoteIds: List<Long>): Map<Long, AnilistMediaArtwork> {
        val uncached = remoteIds.distinct().filter { !cache.containsKey(it) }
        if (uncached.isNotEmpty()) {
            awaitRateLimit()
            withContext(Dispatchers.IO) {
                var addedAny = false
                uncached.chunked(50).forEach { chunk ->
                    awaitRateLimit()
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

                        val response = aniListClient.newCall(request).awaitSuccess()
                        val bodyString = response.body?.string() ?: return@forEach
                        val root = json.parseToJsonElement(bodyString) as? JsonObject
                        val mediaList = ((root?.get("data") as? JsonObject)?.get("Page") as? JsonObject)?.get("media") as? JsonArray

                        mediaList?.forEach { element ->
                            val obj = element as? JsonObject ?: return@forEach
                            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@forEach
                            val country = obj["countryOfOrigin"].asStringOrNull()
                            val titleObj = obj["title"] as? JsonObject
                            val english = titleObj?.get("english").asStringOrNull()
                            val romaji = titleObj?.get("romaji").asStringOrNull()
                            val native = titleObj?.get("native").asStringOrNull()
                            val synonymsList = (obj["synonyms"] as? JsonArray)?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            val banner = obj["bannerImage"].asStringOrNull()
                            val coverObj = obj["coverImage"] as? JsonObject
                            val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                                ?: coverObj?.get("large").asStringOrNull()
                            val avgScore = obj["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                            val meanScore = obj["meanScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
                            addedAny = true
                        }

                        // Mark any unreturned IDs as empty so we don't repeat queries endlessly on successful response
                        if (mediaList != null) {
                            chunk.forEach { id ->
                                if (cache.putIfAbsent(id, EMPTY_ARTWORK) == null) {
                                    addedAny = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        handleException(e, "fetchBatch($chunk)")
                    }
                }
                if (addedAny) {
                    scheduleSaveToDisk()
                }
            }
        }
        return remoteIds.associateWith { cache[it] ?: EMPTY_ARTWORK }
    }

    suspend fun fetchByTitle(rawTitle: String): AnilistMediaArtwork? {
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        titleCache[title]?.let { return it }
        awaitRateLimit()

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

                val response = aniListClient.newCall(request).awaitSuccess()
                val bodyString = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(bodyString) as? JsonObject
                val media = (root?.get("data") as? JsonObject)?.get("Media") as? JsonObject

                if (media != null) {
                    val id = media["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val country = media["countryOfOrigin"].asStringOrNull()
                    val titleObj = media["title"] as? JsonObject
                    val english = titleObj?.get("english").asStringOrNull()
                    val romaji = titleObj?.get("romaji").asStringOrNull()
                    val native = titleObj?.get("native").asStringOrNull()
                    val synonymsList = (media["synonyms"] as? JsonArray)?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                    val banner = media["bannerImage"].asStringOrNull()
                    val coverObj = media["coverImage"] as? JsonObject
                    val extraLarge = coverObj?.get("extraLarge").asStringOrNull()
                        ?: coverObj?.get("large").asStringOrNull()
                    val avgScore = media["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    val meanScore = media["meanScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
                    scheduleSaveToDisk()
                    artwork
                } else {
                    titleCache[title] = EMPTY_ARTWORK
                    scheduleSaveToDisk()
                    EMPTY_ARTWORK
                }
            } catch (e: Exception) {
                handleException(e, "fetchByTitle($title)")
                null
            }
        }
    }

    companion object {
        private const val API_URL = "https://graphql.anilist.co"

        val EMPTY_ARTWORK = AnilistMediaArtwork()

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
