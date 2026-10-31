package eu.kanade.tachiyomi.data.coil

import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.MangaCover
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.manga.model.Manga as DomainManga

class MangaKeyer : Keyer<DomainManga> {
    private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()

    override fun key(data: DomainManga, options: Options): String {
        return if (data.hasCustomCover()) {
            val customUrl = getCustomMangaInfo.get(data.id)?.thumbnailUrl
            "${data.id};${customUrl.orEmpty()};${data.coverLastModified}"
        } else {
            "${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class MangaCoverKeyer : Keyer<MangaCover> {
    private val coverCache: CoverCache by injectLazy()
    private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()

    override fun key(data: MangaCover, options: Options): String {
        return if (coverCache.hasCustomCover(data.mangaId)) {
            val customUrl = getCustomMangaInfo.get(data.mangaId)?.thumbnailUrl
            "${data.mangaId};${customUrl.orEmpty()};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
