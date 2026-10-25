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
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class NaverWebtoonArtwork(
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val title: String? = null,
    val titleId: Long? = null,
)

class NaverWebtoonCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
    private val json: Json = Injekt.get(),
) {
    private val cache = ConcurrentHashMap<String, NaverWebtoonArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    fun getCached(query: String): NaverWebtoonArtwork? = cache[normalizeKey(query)]

    suspend fun fetchArtwork(query: String): NaverWebtoonArtwork? {
        val key = normalizeKey(query)
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Query Naver Webtoon API (Best for native Hangul names)
                val naverResult = fetchFromNaverComic(query)
                if (naverResult != null && (naverResult.bannerUrl != null || naverResult.coverUrl != null)) {
                    cache[key] = naverResult
                    return@withContext naverResult
                }

                // Step 2: Query WEBTOON Global portal (Best for English titles & official localized manhwas)
                val globalResult = fetchFromWebtoonsGlobal(query)
                if (globalResult != null && (globalResult.bannerUrl != null || globalResult.coverUrl != null)) {
                    cache[key] = globalResult
                    return@withContext globalResult
                }

                val empty = NaverWebtoonArtwork()
                cache[key] = empty
                empty
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    logcat(throwable = e) { "Naver Webtoon rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(throwable = e) { "Failed to fetch Naver Webtoon artwork for query: $query" }
                }
                null
            }
        }
    }

    private suspend fun fetchFromNaverComic(query: String): NaverWebtoonArtwork? {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val searchUrl = "https://comic.naver.com/api/search/all?keyword=$encoded"
        val request = GET(
            searchUrl,
            headers = okhttp3.Headers.Builder()
                .add("Referer", "https://comic.naver.com/webtoon")
                .add("User-Agent", USER_AGENT_DESKTOP)
                .build(),
        )

        val response = client.newCall(request).awaitSuccess()
        val bodyString = response.body.string()
        if (bodyString.isBlank()) return null

        val root = json.parseToJsonElement(bodyString).jsonObject
        val searchWebtoonResult = root["searchWebtoonResult"]?.jsonObject
        val webtoonList = searchWebtoonResult?.get("searchViewList")?.jsonArray

        var titleId: Long? = null
        var thumbnailUrl: String? = null
        var matchedTitle: String? = null

        if (!webtoonList.isNullOrEmpty()) {
            val queryClean = query.replace(" ", "").lowercase()
            val matchedObj = webtoonList.firstOrNull { elem ->
                val name = elem.jsonObject["titleName"]?.jsonPrimitive?.content.orEmpty().replace(" ", "").lowercase()
                name == queryClean || name.contains(queryClean) || queryClean.contains(name)
            }?.jsonObject ?: if (containsHangul(query)) webtoonList.first().jsonObject else null

            if (matchedObj != null) {
                titleId = matchedObj["titleId"]?.jsonPrimitive?.content?.toLongOrNull()
                thumbnailUrl = matchedObj["thumbnailUrl"]?.jsonPrimitive?.content
                matchedTitle = matchedObj["titleName"]?.jsonPrimitive?.content
            }
        }

        if (titleId == null) {
            // Check Nbooks comic/novel results as fallback
            val nbooks = root["searchNbooksNovelResult"]?.jsonObject?.get("searchViewList")?.jsonArray
                ?: root["searchNbooksComicResult"]?.jsonObject?.get("searchViewList")?.jsonArray
            if (!nbooks.isNullOrEmpty()) {
                val item = nbooks.first().jsonObject
                matchedTitle = item["titleName"]?.jsonPrimitive?.content
            }
        }

        if (titleId == null && thumbnailUrl.isNullOrBlank()) {
            return null
        }

        var bannerUrl: String? = null
        if (titleId != null) {
            bannerUrl = fetchNaverMobileHeaderBanner(titleId)
        }

        val bannerHttps = bannerUrl?.replace("http://", "https://")?.ifBlank { null }
        val coverHttps = thumbnailUrl?.replace("http://", "https://")?.ifBlank { null }

        logcat(priority = LogPriority.INFO) { "Fetched Naver Comic artwork for '$query': cover=$coverHttps, banner=$bannerHttps" }
        return NaverWebtoonArtwork(
            bannerUrl = bannerHttps,
            coverUrl = coverHttps,
            title = matchedTitle,
            titleId = titleId,
        )
    }

    private suspend fun fetchNaverMobileHeaderBanner(titleId: Long): String? {
        return try {
            val mobileUrl = "https://m.comic.naver.com/webtoon/list?titleId=$titleId"
            val request = GET(
                mobileUrl,
                headers = okhttp3.Headers.Builder()
                    .add("User-Agent", USER_AGENT_MOBILE)
                    .add("Referer", "https://m.comic.naver.com/")
                    .build(),
            )
            val response = client.newCall(request).awaitSuccess()
            val html = response.body.string()

            // Look for backImage high-res wide header
            val backImageMatch = BACK_IMAGE_REGEX.find(html)
            if (backImageMatch != null) {
                return backImageMatch.groupValues[1]
            }

            // Fallback: og:image
            val ogMatch = OG_IMAGE_REGEX.find(html)
            ogMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            logcat(throwable = e) { "Failed to extract mobile header banner for Naver titleId $titleId" }
            null
        }
    }

    private suspend fun fetchFromWebtoonsGlobal(query: String): NaverWebtoonArtwork? {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "https://www.webtoons.com/en/search?keyword=$encoded"
            val request = GET(
                searchUrl,
                headers = okhttp3.Headers.Builder()
                    .add("User-Agent", USER_AGENT_DESKTOP)
                    .build(),
            )
            val response = client.newCall(request).awaitSuccess()
            val html = response.body.string()

            val cardMatches = WEBTOONS_CARD_LINK_REGEX.findAll(html).toList()
            if (cardMatches.isEmpty()) return null

            val qClean = query.replace(" ", "").lowercase()
            val seriesUrl = cardMatches.firstOrNull { m ->
                val u = m.groupValues[1].lowercase()
                u.contains(qClean.take(6)) || qClean.split(Regex("""[^a-z0-9]+""")).any { it.length >= 4 && u.contains(it) }
            }?.groupValues?.get(1) ?: cardMatches.first().groupValues[1]

            val seriesReq = GET(
                seriesUrl,
                headers = okhttp3.Headers.Builder()
                    .add("User-Agent", USER_AGENT_DESKTOP)
                    .add("Referer", "https://www.webtoons.com/")
                    .build(),
            )
            val seriesResp = client.newCall(seriesReq).awaitSuccess()
            val seriesHtml = seriesResp.body.string()

            val bannerUrl = WEBTOONS_BG_REGEX.find(seriesHtml)?.groupValues?.get(1)?.replace("http://", "https://")
            val rawCoverUrl = WEBTOONS_COVER_REGEX.find(seriesHtml)?.groupValues?.get(1)
            val coverUrl = rawCoverUrl?.replace(Regex("""\?type=crop\d+_\d+"""), "?type=q90")?.replace("http://", "https://")

            if (bannerUrl != null || coverUrl != null) {
                logcat(priority = LogPriority.INFO) { "Fetched Webtoons Global artwork for '$query': cover=$coverUrl, banner=$bannerUrl" }
                NaverWebtoonArtwork(
                    bannerUrl = bannerUrl,
                    coverUrl = coverUrl,
                    title = query,
                )
            } else null
        } catch (e: Exception) {
            logcat(priority = LogPriority.INFO, throwable = e) { "Failed to fetch from Webtoons Global for query: $query" }
            null
        }
    }

    companion object {
        private const val USER_AGENT_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val USER_AGENT_MOBILE =
            "Mozilla/5.0 (Linux; Android 14; Galaxy S24 Ultra) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        private val BACK_IMAGE_REGEX = Regex("""src=["'](https://[^"']*backImage_[^"']+)["']""")
        private val OG_IMAGE_REGEX = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""")
        private val WEBTOONS_CARD_LINK_REGEX = Regex("""<a\s+href=["'](https://www\.webtoons\.com/en/[^"']+/list\?title_no=\d+)["']""")
        private val WEBTOONS_BG_REGEX = Regex("""class=["']detail_bg["']\s+style=["'][^"']*url\(['"]?([^'"\)]+)['"]?\)""")
        private val WEBTOONS_COVER_REGEX = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""")

        fun normalizeKey(query: String): String {
            return query.trim().lowercase()
        }
    }
}
