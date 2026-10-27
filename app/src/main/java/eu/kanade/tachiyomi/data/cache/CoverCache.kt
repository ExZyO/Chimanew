package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.entries.anime.model.Anime
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Names of files are created with the md5 of the thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class CoverCache(private val context: Context) {

    companion object {
        private const val COVERS_DIR = "covers"
        private const val CUSTOM_COVERS_DIR = "covers/custom"
    }

    /**
     * Cache directory used for cache management.
     */
    private val cacheDir = getCacheDir(COVERS_DIR)

    private val customCoverCacheDir = getCacheDir(CUSTOM_COVERS_DIR)

    private val customCoverNames = mutableSetOf<String>()
    private val deletedWhileIndexing = mutableSetOf<String>()

    @Volatile
    private var customCoversIndexed = false

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val names = customCoverCacheDir.list().orEmpty()
            synchronized(customCoverNames) {
                names.filterNotTo(customCoverNames) { it in deletedWhileIndexing }
                deletedWhileIndexing.clear()
                customCoversIndexed = true
            }
        }
    }

    /**
     * Checks if a custom cover exists without hitting the disk on every check.
     */
    fun hasCustomCover(mangaId: Long?): Boolean {
        val file = getCustomCoverFile(mangaId)
        if (!customCoversIndexed) return file.exists()
        return synchronized(customCoverNames) { file.name in customCoverNames }
    }

    /**
     * Returns the cover from cache.
     *
     * @param mangaThumbnailUrl thumbnail url for the manga.
     * @return cover image.
     */
    fun getCoverFile(mangaThumbnailUrl: String?): File? {
        return mangaThumbnailUrl?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param mangaId the manga id.
     * @return cover image.
     */
    fun getCustomCoverFile(mangaId: Long?): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(mangaId.toString()))
    }

    /**
     * Saves the given stream as the manga's custom cover to cache.
     *
     * @param manga the manga.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(manga: Manga, inputStream: InputStream) {
        val file = getCustomCoverFile(manga.id)
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        synchronized(customCoverNames) {
            customCoverNames.add(file.name)
            deletedWhileIndexing.remove(file.name)
        }
    }

    @Throws(IOException::class)
    fun setCustomCoverToCache(anime: Anime, inputStream: InputStream) {
        val file = getCustomCoverFile(anime.id)
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        synchronized(customCoverNames) {
            customCoverNames.add(file.name)
            deletedWhileIndexing.remove(file.name)
        }
    }

    fun deleteFromCache(anime: Anime, deleteCustomCover: Boolean = false): Int {
        var deleted = 0

        getCoverFile(anime.thumbnailUrl)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomCover) {
            if (deleteCustomCover(anime.id)) ++deleted
        }

        return deleted
    }

    /**
     * Delete the cover files of the manga from the cache.
     *
     * @param manga the manga.
     * @param deleteCustomCover whether the custom cover should be deleted.
     * @return number of files that were deleted.
     */
    fun deleteFromCache(manga: Manga, deleteCustomCover: Boolean = false): Int {
        var deleted = 0

        getCoverFile(manga.thumbnailUrl)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomCover) {
            if (deleteCustomCover(manga.id)) ++deleted
        }

        return deleted
    }

    /**
     * Delete custom cover of the manga from the cache
     *
     * @param mangaId the manga id.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(mangaId: Long?): Boolean {
        val file = getCustomCoverFile(mangaId)
        val deleted = file.exists() && file.delete()
        synchronized(customCoverNames) {
            customCoverNames.remove(file.name)
            if (!customCoversIndexed) deletedWhileIndexing.add(file.name)
        }
        return deleted
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
