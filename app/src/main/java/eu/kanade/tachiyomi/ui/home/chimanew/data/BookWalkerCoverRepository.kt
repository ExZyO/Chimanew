// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.data

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                // BookWalker Japan search expects 'word' parameter, not 'q'
                val searchUrl = "https://bookwalker.jp/search/?word=$encoded"
                val request = GET(
                    searchUrl,
                    headers = okhttp3.Headers.Builder()
                        .add("User-Agent", USER_AGENT)
                        .build(),
                )

                val response = client.newCall(request).awaitSuccess()
                val html = response.body.string()
                if (html.isBlank()) return@withContext null

                // Extract BookWalker digital cover image ID
                val match = THUMB_ID_REGEX.find(html) ?: CIMG_ID_REGEX.find(html) ?: RIMG_ID_REGEX.find(html)
                if (match != null) {
                    val id = match.groupValues[1]
                    val hdCoverUrl = "https://c.bookwalker.jp/$id/t_700x780.jpg"
                    val artwork = BookWalkerArtwork(
                        bannerUrl = hdCoverUrl,
                        coverUrl = hdCoverUrl,
                        title = query,
                        bookId = id,
                    )
                    cache[key] = artwork
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
                    logcat(throwable = e) { "BookWalker Japan rate limited (429). Pausing calls for 60s." }
                } else {
                    logcat(throwable = e) { "Failed to fetch BookWalker artwork for query: $query" }
                }
                null
            }
        }
    }

    companion object {
        // Use non-blocked User-Agent to bypass AWS WAF 202 challenge
        private const val USER_AGENT =
            "Chimanew/2.6.13 (Android; https://github.com/ExZyO/Chimanew)"

        private val THUMB_ID_REGEX = Regex("""thumbnailImage_([a-zA-Z0-9]+)\.jpg""")
        private val CIMG_ID_REGEX = Regex("""https://c\.bookwalker\.jp/([a-zA-Z0-9]+)/""")
        private val RIMG_ID_REGEX = Regex("""https://rimg\.bookwalker\.jp/([a-zA-Z0-9]+)/""")

        fun normalizeKey(query: String): String {
            return query.trim().lowercase()
        }
    }
}
