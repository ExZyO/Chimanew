// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import logcat.LogPriority
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class DiscoveryItem(
    val remoteId: Long,
    val title: String,
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val mediaType: Int = 0, // 0 = Manga, 1 = Anime, 2 = Novel
)

@Serializable
data class DiscoveryDiskCache(
    val lastFetchedAt: Long = 0L,
    val trendingManga: List<DiscoveryItem> = emptyList(),
    val trendingManhwa: List<DiscoveryItem> = emptyList(),
    val trendingAnime: List<DiscoveryItem> = emptyList(),
    val trendingNovels: List<DiscoveryItem> = emptyList(),
)

class DiscoveryRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
    private val context: Application = Injekt.get(),
) {
    companion object {
        private const val API_URL = "https://graphql.anilist.co"
        private const val CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
    }

    private val cacheFile by lazy { File(context.filesDir, "chimanew_discovery_cache.json") }
    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val memCache = ConcurrentHashMap<String, List<DiscoveryItem>>()
    private var lastFetchedTime: Long = 0L

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (cacheFile.exists()) {
                val text = cacheFile.readText(Charsets.UTF_8)
                val disk = jsonHelper.decodeFromString<DiscoveryDiskCache>(text)
                lastFetchedTime = disk.lastFetchedAt
                if (disk.trendingManga.isNotEmpty()) memCache["manga"] = disk.trendingManga
                if (disk.trendingManhwa.isNotEmpty()) memCache["manhwa"] = disk.trendingManhwa
                if (disk.trendingAnime.isNotEmpty()) memCache["anime"] = disk.trendingAnime
                if (disk.trendingNovels.isNotEmpty()) memCache["novels"] = disk.trendingNovels
                logcat(LogPriority.INFO) {
                    "[DISCOVERY] Loaded from disk: manga=${disk.trendingManga.size}, anime=${disk.trendingAnime.size}, novels=${disk.trendingNovels.size}, age=${(System.currentTimeMillis() - disk.lastFetchedAt) / 60000}m"
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "[DISCOVERY] Failed to load disk cache" }
        }
    }

    private fun saveToDisk() {
        try {
            val disk = DiscoveryDiskCache(
                lastFetchedAt = lastFetchedTime,
                trendingManga = memCache["manga"] ?: emptyList(),
                trendingManhwa = memCache["manhwa"] ?: emptyList(),
                trendingAnime = memCache["anime"] ?: emptyList(),
                trendingNovels = memCache["novels"] ?: emptyList(),
            )
            cacheFile.writeText(jsonHelper.encodeToString(disk), Charsets.UTF_8)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "[DISCOVERY] Failed to persist cache to disk" }
        }
    }

    fun invalidateCache() {
        lastFetchedTime = 0L
        memCache.clear()
        try {
            if (cacheFile.exists()) cacheFile.delete()
        } catch (_: Exception) {}
    }

    suspend fun getTrendingManga(forceRefresh: Boolean = false): List<DiscoveryItem> {
        val cached = memCache["manga"]
        val isFresh = (System.currentTimeMillis() - lastFetchedTime) < CACHE_TTL_MS
        if (!forceRefresh && isFresh && !cached.isNullOrEmpty()) {
            return cached
        }
        return fetchDiscovery(forceRefresh)["manga"] ?: cached ?: emptyList()
    }

    suspend fun getTrendingManhwa(forceRefresh: Boolean = false): List<DiscoveryItem> {
        val cached = memCache["manhwa"]
        val isFresh = (System.currentTimeMillis() - lastFetchedTime) < CACHE_TTL_MS
        if (!forceRefresh && isFresh && !cached.isNullOrEmpty()) {
            return cached
        }
        return fetchDiscovery(forceRefresh)["manhwa"] ?: cached ?: emptyList()
    }

    suspend fun getTrendingAnime(forceRefresh: Boolean = false): List<DiscoveryItem> {
        val cached = memCache["anime"]
        val isFresh = (System.currentTimeMillis() - lastFetchedTime) < CACHE_TTL_MS
        if (!forceRefresh && isFresh && !cached.isNullOrEmpty()) {
            return cached
        }
        return fetchDiscovery(forceRefresh)["anime"] ?: cached ?: emptyList()
    }

    suspend fun getTrendingNovels(forceRefresh: Boolean = false): List<DiscoveryItem> {
        val cached = memCache["novels"]
        val isFresh = (System.currentTimeMillis() - lastFetchedTime) < CACHE_TTL_MS
        if (!forceRefresh && isFresh && !cached.isNullOrEmpty()) {
            return cached
        }
        return fetchDiscovery(forceRefresh)["novels"] ?: cached ?: emptyList()
    }

    private suspend fun fetchDiscovery(forceRefresh: Boolean): Map<String, List<DiscoveryItem>> = withContext(Dispatchers.IO) {
        val isFresh = (System.currentTimeMillis() - lastFetchedTime) < CACHE_TTL_MS
        if (!forceRefresh && isFresh && memCache.isNotEmpty()) {
            return@withContext memCache
        }

        val query = """
            query {
              manga: Page(page: 1, perPage: 20) {
                media(type: MANGA, sort: TRENDING_DESC) {
                  id
                  title { english romaji native }
                  bannerImage
                  coverImage { extraLarge large }
                  description
                  genres
                  averageScore
                }
              }
              manhwa: Page(page: 1, perPage: 20) {
                media(type: MANGA, countryOfOrigin: "KR", sort: TRENDING_DESC) {
                  id
                  title { english romaji native }
                  bannerImage
                  coverImage { extraLarge large }
                  description
                  genres
                  averageScore
                }
              }
              anime: Page(page: 1, perPage: 20) {
                media(type: ANIME, sort: TRENDING_DESC) {
                  id
                  title { english romaji native }
                  bannerImage
                  coverImage { extraLarge large }
                  description
                  genres
                  averageScore
                }
              }
              novel: Page(page: 1, perPage: 20) {
                media(type: MANGA, format: NOVEL, sort: TRENDING_DESC) {
                  id
                  title { english romaji native }
                  bannerImage
                  coverImage { extraLarge large }
                  description
                  genres
                  averageScore
                }
              }
            }
        """.trimIndent()

        try {
            logcat(LogPriority.INFO) { "[DISCOVERY] Querying AniList discovery GraphQL API..." }
            val payload = buildJsonObject {
                put("query", query)
            }
            val request = POST(
                API_URL,
                body = payload.toString().toRequestBody(jsonMime),
            )
            val response = client.newCall(request).awaitSuccess()
            val bodyString = response.body.string()
            val root = json.parseToJsonElement(bodyString) as? JsonObject
            val dataObj = root?.get("data") as? JsonObject

            fun parseMediaList(pageKey: String, mediaType: Int): List<DiscoveryItem> {
                val pageObj = dataObj?.get(pageKey) as? JsonObject ?: return emptyList()
                val mediaArray = pageObj["media"] as? JsonArray ?: return emptyList()
                return mediaArray.mapNotNull { element ->
                    val obj = element as? JsonObject ?: return@mapNotNull null
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null
                    val titleObj = obj["title"] as? JsonObject
                    val title = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("native")?.jsonPrimitive?.contentOrNull
                        ?: return@mapNotNull null
                    val banner = obj["bannerImage"]?.jsonPrimitive?.contentOrNull
                    val coverObj = obj["coverImage"] as? JsonObject
                    val cover = coverObj?.get("extraLarge")?.jsonPrimitive?.contentOrNull
                        ?: coverObj?.get("large")?.jsonPrimitive?.contentOrNull
                    val description = obj["description"]?.jsonPrimitive?.contentOrNull
                        ?.replace(Regex("<[^>]*>"), "")
                        ?.replace(Regex("\\s+"), " ")
                        ?.trim()
                    val genres = (obj["genres"] as? JsonArray)?.mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    }.orEmpty()
                    val score = obj["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

                    DiscoveryItem(
                        remoteId = id,
                        title = title,
                        bannerUrl = banner,
                        coverUrl = cover,
                        description = description,
                        genres = genres,
                        averageScore = score,
                        mediaType = mediaType,
                    )
                }
            }

            val mangaList = parseMediaList("manga", 0)
            val manhwaList = parseMediaList("manhwa", 0)
            val animeList = parseMediaList("anime", 1)
            val novelList = parseMediaList("novel", 2)

            if (mangaList.isNotEmpty()) memCache["manga"] = mangaList
            if (manhwaList.isNotEmpty()) memCache["manhwa"] = manhwaList
            if (animeList.isNotEmpty()) memCache["anime"] = animeList
            if (novelList.isNotEmpty()) memCache["novels"] = novelList

            lastFetchedTime = System.currentTimeMillis()
            saveToDisk()
            logcat(LogPriority.INFO) {
                "[DISCOVERY] Successfully fetched: manga=${mangaList.size}, manhwa=${manhwaList.size}, anime=${animeList.size}, novels=${novelList.size}"
            }
            return@withContext memCache
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "[DISCOVERY] Failed to fetch discovery items" }
            return@withContext memCache
        }
    }
}
