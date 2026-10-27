package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.domain.entries.anime.model.Anime
import java.io.File
import java.io.IOException
import java.io.InputStream

class AnimeCoverCache(private val context: Context) {

    companion object {
        private const val COVERS_DIR = "animecovers"
        private const val CUSTOM_COVERS_DIR = "animecovers/custom"
    }

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

    fun hasCustomCover(animeId: Long?): Boolean {
        val file = getCustomCoverFile(animeId)
        if (!customCoversIndexed) return file.exists()
        return synchronized(customCoverNames) { file.name in customCoverNames }
    }

    fun getCoverFile(animeThumbnailUrl: String?): File? {
        return animeThumbnailUrl?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    fun getCustomCoverFile(animeId: Long?): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(animeId.toString()))
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

    fun deleteCustomCover(animeId: Long?): Boolean {
        val file = getCustomCoverFile(animeId)
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
