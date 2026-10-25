// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class BookWalkerArtwork(
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val title: String? = null,
    val bookId: String? = null,
)

class BookWalkerCoverRepository(
    private val client: OkHttpClient = Injekt.get<NetworkHelper>().client,
) {
    private val cache = ConcurrentHashMap<String, BookWalkerArtwork>()

    @Volatile
    private var rateLimitedUntil: Long = 0L

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntil
    }

    fun getCached(query: String): BookWalkerArtwork? = cache[normalizeKey(query)]

    suspend fun fetchArtwork(query: String): BookWalkerArtwork? {
        val key = normalizeKey(query)
        if (key.isBlank()) return null
        cache[key]?.let { return it }
        if (isRateLimited()) return null

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                // BookWalker Japan search expects 'word' parameter
                val searchUrl = "https://bookwalker.jp/search/?word=$encoded"
                val request = GET(
                    searchUrl,
                    headers = okhttp3.Headers.Builder()
                        .add("User-Agent", USER_AGENT)
                        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .add("Accept-Language", "ja,en-US;q=0.7,en;q=0.3")
                        .build(),
                )

                val response = client.newCall(request).awaitSuccess()
                val html = response.body.string()
                if (html.isBlank()) return@withContext null

                // Extract BookWalker digital cover image
                val thumbMatch = THUMB_ID_REGEX.find(html)
                val bookId = thumbMatch?.groupValues?.get(1)
                val coverUrl = if (!bookId.isNullOrBlank()) {
                    "https://c.bookwalker.jp/thumbnailImage_$bookId.jpg"
                } else {
                    DATA_ORIGINAL_REGEX.find(html)?.groupValues?.get(1)
                }

                if (!coverUrl.isNullOrBlank()) {
                    val artwork = BookWalkerArtwork(
                        bannerUrl = null, // BookWalker is a manga tankōbon bookstore and only provides portrait covers
                        coverUrl = coverUrl,
                        title = query,
                        bookId = bookId,
                    )
                    cache[key] = artwork
                    logcat(priority = LogPriority.INFO) { "Fetched BookWalker artwork for '$query': cover=$coverUrl" }
                    artwork
                } else {
                    val empty = BookWalkerArtwork()
                    cache[key] = empty
                    empty
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("429") || msg.contains("Too Many Requests", ignoreCase = true)) {
                    rateLimitedUntil = System.currentTimeMillis() + 60_000L
                    logcat(priority = LogPriority.WARN, throwable = e) { "BookWalker Japan rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(priority = LogPriority.INFO, throwable = e) { "Failed to fetch BookWalker artwork for query: $query" }
                }
                null
            }
        }
    }

    companion object {
        // Mobile Safari User-Agent bypasses AWS WAF challenge without captcha
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

        private val THUMB_ID_REGEX = Regex("""thumbnailImage_([a-zA-Z0-9]+)\.jpg""")
        private val DATA_ORIGINAL_REGEX = Regex("""data-original=["'](https://c\.bookwalker\.jp/[^"']+)["']""")

        fun normalizeKey(query: String): String {
            return query.trim().lowercase()
        }
    }
}
