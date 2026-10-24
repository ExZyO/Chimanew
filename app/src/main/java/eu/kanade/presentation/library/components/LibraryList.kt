package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LibraryList(
    items: List<LibraryItem>,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    val preferences = remember { Injekt.get<ChimanewPreferences>() }
    val waterfallRepo = remember { Injekt.get<ArtworkWaterfallRepository>() }
    val useAnilistArtwork by preferences.useAnilistArtworkInLibrary().collectPrefAsState()
    val artworkWaterfallOrder by preferences.artworkWaterfallOrder().collectPrefAsState()

    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (!searchQuery.isNullOrEmpty()) {
                GlobalSearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                )
            }
        }

        items(
            items = items,
            contentType = { "library_list_item" },
        ) { libraryItem ->
            val manga = libraryItem.libraryManga.manga
            val highResCover by produceState<String?>(
                initialValue = if (useAnilistArtwork) {
                    val mTitle = manga.ogTitle ?: manga.title
                    waterfallRepo.getCached(mTitle)?.posterUrl
                } else null,
                key1 = manga.id,
                key2 = useAnilistArtwork,
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
            MangaListItem(
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
                badge = {
                    DownloadsBadge(count = libraryItem.downloadCount)
                    UnreadBadge(count = libraryItem.unreadCount)
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
            )
        }
    }
}
