package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.saveTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import java.io.File
import java.io.IOException

/**
 * Class used to create chapter cache.
 * High-concurrency lockless file cache with TTL and disk-quota management.
 * For each image in a chapter a file is created (format: *md5key*.0).
 * For each chapter a JSON page list is created (*md5key*.json).
 *
 * @param context the application context.
 */
class ChapterCache(
    private val context: Context,
    private val json: Json,
    readerPreferences: ReaderPreferences,
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    val cacheDir: File = File(context.cacheDir, "chapter_disk_cache").also { it.mkdirs() }

    @Volatile
    private var maxCacheSizeBytes: Long = readerPreferences.cacheSize().get().toLong() * 1024 * 1024

    init {
        // Clean up any stale partial writes on start
        scope.launch {
            try {
                cacheDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { it.delete() }
                trimCache()
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed initial cache cleanup" }
            }
        }

        readerPreferences.cacheSize().changes()
            .drop(1)
            .onEach {
                maxCacheSizeBytes = it.toLong() * 1024 * 1024
                trimCache()
            }
            .launchIn(scope)
    }

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    /**
     * Get page list from cache.
     *
     * @param chapter the chapter.
     * @return the list of pages.
     */
    fun getPageListFromCache(chapter: Chapter): List<Page> {
        val file = getPageListFile(chapter)
        if (!file.exists()) {
            throw IOException("Page list for chapter not in cache")
        }
        return json.decodeFromString(file.readText())
    }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        val destFile = getPageListFile(chapter)
        val tempFile = File.createTempFile("pl_", ".tmp", cacheDir)
        try {
            val cachedValue = json.encodeToString(pages)
            tempFile.writeText(cachedValue)
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            tempFile.delete()
            logcat(LogPriority.WARN, e) { "Failed to put page list to cache" }
        }
    }

    /**
     * Returns true if image is in cache.
     * Lockless check without monitor contention.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    fun isImageInCache(imageUrl: String): Boolean {
        val file = getImageFile(imageUrl)
        return file.exists() && file.length() > 0
    }

    /**
     * Get image file from url.
     *
     * @param imageUrl url of image.
     * @return path of image.
     */
    fun getImageFile(imageUrl: String): File {
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(cacheDir, imageName)
    }

    /**
     * Add image to cache without locking the entire cache.
     * Uses atomic temporary file rename for concurrent write safety.
     *
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, response: Response) {
        val destFile = getImageFile(imageUrl)
        val tempFile = File.createTempFile("ch_", ".tmp", cacheDir)
        try {
            response.body.source().use { source ->
                tempFile.outputStream().use { output ->
                    source.saveTo(output)
                }
            }
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            response.body.close()
        }
    }

    /**
     * Clear all files from the chapter cache.
     */
    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (it.isFile && it.delete()) {
                deletedFiles++
            }
        }
        return deletedFiles
    }

    /**
     * Trims cache to stay within max size and purges files older than 7 days (TTL).
     */
    fun trimCache() {
        val files = cacheDir.listFiles() ?: return
        val now = System.currentTimeMillis()
        val ttlMillis = 7L * 24 * 60 * 60 * 1000 // 7 days TTL

        val validFiles = files.filter { it.isFile && !it.name.endsWith(".tmp") }
            .sortedBy { it.lastModified() } // oldest first

        var currentTotalSize = validFiles.sumOf { it.length() }

        for (file in validFiles) {
            val length = file.length()
            val isExpired = (now - file.lastModified()) > ttlMillis
            if (isExpired || currentTotalSize > maxCacheSizeBytes) {
                if (file.delete()) {
                    currentTotalSize -= length
                }
            }
        }
    }

    private fun getPageListFile(chapter: Chapter): File {
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))
        return File(cacheDir, "$key.json")
    }

    private fun getKey(chapter: Chapter): String {
        return "${chapter.mangaId}${chapter.url}"
    }
}
