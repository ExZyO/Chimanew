package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun LibraryComfortableGrid(
    items: List<LibraryItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    // KMK -->
    usePanoramaCover: Boolean = false,
    // KMK <--
) {
    val preferences = remember { Injekt.get<ChimanewPreferences>() }
    val waterfallRepo = remember { Injekt.get<ArtworkWaterfallRepository>() }
    val useAnilistArtwork by preferences.useAnilistArtworkInLibrary().collectPrefAsState()
    val artworkWaterfallOrder by preferences.artworkWaterfallOrder().collectPrefAsState()
    val artworkCoverOrder by preferences.artworkCoverOrder().collectPrefAsState()

    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        items(
            items = items,
            contentType = { "library_comfortable_grid_item" },
        ) { libraryItem ->
            val manga = libraryItem.libraryManga.manga
            val highResCover by produceState<String?>(
                initialValue = if (useAnilistArtwork) {
                    val mTitle = manga.ogTitle ?: manga.title
                    waterfallRepo.getCached(mTitle)?.posterUrl
                } else null,
                key1 = manga.id,
                key2 = useAnilistArtwork,
                key3 = artworkCoverOrder,
                key4 = artworkWaterfallOrder,
            ) {
                if (useAnilistArtwork) {
                    val mTitle = manga.ogTitle ?: manga.title
                    val cached = waterfallRepo.getCached(mTitle)?.posterUrl
                    if (cached != null) {
                        value = cached
                    } else {
                        val resolved = waterfallRepo.resolveArtwork(mTitle, mangaId = manga.id)
                        value = resolved.posterUrl
                    }
                } else {
                    value = null
                }
            }
            MangaComfortableGridItem(
                isSelected = manga.id in selection,
                title = manga.title,
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    ogUrl = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                coverModel = highResCover,
                coverBadgeStart = {
                    DownloadsBadge(count = libraryItem.downloadCount)
                    UnreadBadge(count = libraryItem.unreadCount)
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = libraryItem.isLocal,
                        sourceLanguage = libraryItem.sourceLanguage,
                        // KMK -->
                        useLangIcon = libraryItem.useLangIcon,
                        // KMK <--
                    )
                    // KMK -->
                    SourceIconBadge(source = libraryItem.source)
                    // KMK <--
                },
                onLongClick = { onLongClick(libraryItem.libraryManga) },
                onClick = { onClick(libraryItem.libraryManga) },
                onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                    { onClickContinueReading(libraryItem.libraryManga) }
                } else {
                    null
                },
                // KMK -->
                usePanoramaCover = usePanoramaCover,
                // KMK <--
            )
        }
    }
}
