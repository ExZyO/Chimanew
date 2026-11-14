// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.core.net.toUri
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.history.components.HistoryDeleteDialog
import eu.kanade.presentation.more.settings.screen.SettingsNuvioScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import chimahon.novel.ui.browse.globalsearch.GlobalNovelSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioHeroSection
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioMangaShelf
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioQuickTrackerSheet
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.manga.track.TrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object ChimanewHomeTab : Tab {
    private fun readResolve(): Any = ChimanewHomeTab

    private val scrollToTopChannel = Channel<Unit>(Channel.CONFLATED)

    override suspend fun onReselect(navigator: cafe.adriel.voyager.navigator.Navigator) {
        scrollToTopChannel.trySend(Unit)
        HomeScreen.search("")
    }

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_home),
                icon = rememberVectorPainter(Icons.Filled.Home),
            )
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { ChimanewHomeScreenModel() }
        val state by screenModel.state.collectAsState()

        var selectedLongPressItem by remember { mutableStateOf<HomeMangaItem?>(null) }
        var quickTrackerItem by remember { mutableStateOf<HomeMangaItem?>(null) }
        var itemToRemoveFromHistory by remember { mutableStateOf<HomeMangaItem?>(null) }
        var categoryDialogData by remember { mutableStateOf<CategoryDialogData?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val trackerManager = remember { Injekt.get<TrackerManager>() }
        val onTrackerClick: (HomeMangaItem) -> Unit = { item ->
            val hasLoggedInTrackers = trackerManager.loggedInTrackers().any { it !is EnhancedTracker }
            if (!hasLoggedInTrackers) {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
            } else {
                quickTrackerItem = item
            }
        }

        val onResumeClick: (HomeMangaItem) -> Unit = { item ->
            if (item.isDiscovery) {
                when (item.mediaType) {
                    MEDIA_TYPE_ANIME -> navigator.push(GlobalAnimeSearchScreen(item.title))
                    MEDIA_TYPE_NOVEL -> navigator.push(GlobalNovelSearchScreen(item.title))
                    else -> navigator.push(GlobalSearchScreen(item.title))
                }
            } else when (item.mediaType) {
                MEDIA_TYPE_ANIME -> {
                    if (item.lastChapterId != null && !item.isChapterRead) {
                        val intent = PlayerActivity.newIntent(context, item.mangaId, item.lastChapterId)
                        context.startActivity(intent)
                    } else {
                        scope.launch {
                            val ep = screenModel.getResumeEpisode(item.mangaId, item.lastChapterId)
                            if (ep != null) {
                                val intent = PlayerActivity.newIntent(context, item.mangaId, ep.id)
                                context.startActivity(intent)
                            } else {
                                navigator.push(AnimeScreen(item.mangaId))
                            }
                        }
                    }
                }
                MEDIA_TYPE_NOVEL -> {
                    screenModel.resumeNovel(context, item.mangaId) { screen ->
                        navigator.push(screen)
                    }
                }
                else -> {
                    // Manga
                    if (item.lastChapterId != null && !item.isChapterRead) {
                        val intent = ReaderActivity.newIntent(context, item.mangaId, item.lastChapterId)
                        context.startActivity(intent)
                    } else {
                        scope.launch {
                            val chapter = screenModel.getResumeChapter(item.mangaId, item.lastChapterId)
                            if (chapter != null) {
                                val intent = ReaderActivity.newIntent(context, item.mangaId, chapter.id)
                                context.startActivity(intent)
                            } else {
                                navigator.push(MangaScreen(item.mangaId))
                            }
                        }
                    }
                }
            }
        }

        val onDetailsClick: (HomeMangaItem) -> Unit = { item ->
            if (item.isDiscovery) {
                when (item.mediaType) {
                    MEDIA_TYPE_ANIME -> navigator.push(GlobalAnimeSearchScreen(item.title))
                    MEDIA_TYPE_NOVEL -> navigator.push(GlobalNovelSearchScreen(item.title))
                    else -> navigator.push(GlobalSearchScreen(item.title))
                }
            } else when (item.mediaType) {
                MEDIA_TYPE_ANIME -> navigator.push(AnimeScreen(item.mangaId))
                MEDIA_TYPE_NOVEL -> screenModel.openNovelDetails(item.mangaId) { navigator.push(it) }
                else -> navigator.push(MangaScreen(item.mangaId))
            }
        }

        // Card tap action setting toggle (0 = Details page, 1 = Reader/Player)
        val onCardClick: (HomeMangaItem) -> Unit = { item ->
            if (state.shelfTapAction == 1) {
                onResumeClick(item)
            } else {
                onDetailsClick(item)
            }
        }

        val onLongClickItem: (HomeMangaItem) -> Unit = { item ->
            selectedLongPressItem = item
        }

        val listState = rememberLazyListState()

        var hasScrolledToTopOnce by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state.heroItems.size, state.shelves.size, state.activeMedia) {
            if (!hasScrolledToTopOnce && (state.heroItems.isNotEmpty() || state.shelves.isNotEmpty())) {
                hasScrolledToTopOnce = true
                listState.scrollToItem(0, 0)
            }
        }

        LaunchedEffect(Unit) {
            scrollToTopChannel.receiveAsFlow().collectLatest {
                listState.animateScrollToItem(0)
            }
        }

        val scrollAlpha by remember {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (listState.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 1f)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            PullRefresh(
                refreshing = state.isRefreshing,
                enabled = !state.isLoading,
                onRefresh = screenModel::refreshHero,
                indicatorPadding = WindowInsets.statusBars.asPaddingValues(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 1. Top Hero Section
                    if (state.heroItems.isNotEmpty()) {
                        item(key = "hero_carousel_${state.activeMedia}") {
                            NuvioHeroSection(
                                items = state.heroItems,
                                heroStyle = state.heroStyle,
                                heroArtworkSources = state.heroArtworkSources,
                                onItemClick = onCardClick,
                                onResumeClick = onResumeClick,
                                onQuickStatusClick = onTrackerClick,
                            )
                        }
                    }

                    // 2. Dynamic Shelves (ordered and filtered according to user preferences)
                    items(
                        items = state.shelves,
                        key = { "${it.id}_${state.activeMedia}" },
                        contentType = { "nuvio_manga_shelf" },
                    ) { shelf ->
                        NuvioMangaShelf(
                            title = shelf.title,
                            items = shelf.items,
                            showProgressBar = state.showProgressBars,
                            showUnreadBadge = state.showUnreadBadges,
                            onItemClick = if (shelf.isResumeOnTap) onResumeClick else onCardClick,
                            onLongClick = onLongClickItem,
                        )
                    }

                    // 4. Loading State
                    if (state.isLoading && state.heroItems.isEmpty() && state.shelves.isEmpty()) {
                        item(key = "loading_state") {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // 5. Empty State
                    if (state.heroItems.isEmpty() && state.shelves.isEmpty() && !state.isLoading) {
                        item(key = "empty_state_${state.activeMedia}") {
                            val emptyTitle = when (state.activeMedia) {
                                MEDIA_TYPE_ANIME -> "Anime in Chimanew"
                                MEDIA_TYPE_NOVEL -> "Novels in Chimanew"
                                else -> "Welcome to Chimanew"
                            }
                            val emptySummary = when (state.activeMedia) {
                                MEDIA_TYPE_ANIME -> "Start watching or add anime to your library to populate your personalized feed."
                                MEDIA_TYPE_NOVEL -> "Start reading or add novels to your library to populate your personalized feed."
                                else -> "Start reading or add manga to your library to populate your personalized home feed."
                            }
                            val buttonText = when (state.activeMedia) {
                                MEDIA_TYPE_ANIME -> "Browse Anime Extensions"
                                MEDIA_TYPE_NOVEL -> "Browse Novel Extensions"
                                else -> "Browse Extensions"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = emptyTitle,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = emptySummary,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { navigator.push(BrowseTab) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Explore,
                                        contentDescription = null,
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                    Text(text = buttonText)
                                }
                            }
                        }
                    }

                    // Bottom spacer for navigation bar clearance
                    item(key = "bottom_nav_spacer") {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }

            // Sleek Ambient App Bar Header (Smoothly darkens on scroll; no blocking text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = scrollAlpha * 0.95f))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left side: Compact dark translucent pill [ Manga ▾ ]
                var mediaMenuExpanded by remember { mutableStateOf(false) }
                val mediaTitle = when (state.activeMedia) {
                    MEDIA_TYPE_ANIME -> "Anime"
                    MEDIA_TYPE_NOVEL -> "Novels"
                    else -> "Manga"
                }
                val mediaIcon = when (state.activeMedia) {
                    MEDIA_TYPE_ANIME -> Icons.Filled.PlayArrow
                    MEDIA_TYPE_NOVEL -> Icons.Outlined.MenuBook
                    else -> Icons.Outlined.AutoStories
                }

                Box {
                    Surface(
                        onClick = { mediaMenuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = mediaIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mediaTitle,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Switch Media",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = mediaMenuExpanded,
                        onDismissRequest = { mediaMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manga", fontWeight = if (state.activeMedia == MEDIA_TYPE_MANGA) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                            onClick = {
                                mediaMenuExpanded = false
                                screenModel.switchMedia(MEDIA_TYPE_MANGA)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Anime", fontWeight = if (state.activeMedia == MEDIA_TYPE_ANIME) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                            onClick = {
                                mediaMenuExpanded = false
                                screenModel.switchMedia(MEDIA_TYPE_ANIME)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Novels", fontWeight = if (state.activeMedia == MEDIA_TYPE_NOVEL) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                            onClick = {
                                mediaMenuExpanded = false
                                screenModel.switchMedia(MEDIA_TYPE_NOVEL)
                            },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                HomeScreen.search("")
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(MR.strings.action_search),
                            tint = Color.White,
                        )
                    }

                    IconButton(
                        onClick = { navigator.push(SettingsNuvioScreen) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Nuvio Settings",
                            tint = Color.White,
                        )
                    }
                }
            }

            // Long-Press Options Bottom Sheet
            selectedLongPressItem?.let { item ->
                ModalBottomSheet(
                    onDismissRequest = { selectedLongPressItem = null },
                    sheetState = sheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        // Header: Cover + Title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = item.coverData,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 66.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (item.lastChapterNumber != null) {
                                    val unit = if (item.mediaType == MEDIA_TYPE_ANIME) "Episode" else "Chapter"
                                    Text(
                                        text = "$unit ${item.lastChapterNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val detailLabel = when (item.mediaType) {
                            MEDIA_TYPE_ANIME -> "Anime Details"
                            MEDIA_TYPE_NOVEL -> "Novel Details"
                            else -> "Manga Details"
                        }
                        val resumeLabel = when (item.mediaType) {
                            MEDIA_TYPE_ANIME -> "Resume Watching"
                            else -> "Resume Reading"
                        }
                        val markNextLabel = when (item.mediaType) {
                            MEDIA_TYPE_ANIME -> "Mark Next Episode as Watched"
                            else -> "Mark Next Chapter as Read"
                        }
                        val downloadNextLabel = when (item.mediaType) {
                            MEDIA_TYPE_ANIME -> "Download Next Episode"
                            else -> "Download Next Chapter"
                        }

                        // Action 1: Open Details Page
                        ListItem(
                            headlineContent = { Text(detailLabel) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedLongPressItem = null
                                onDetailsClick(item)
                            },
                        )

                        // Action 2: Resume / Start Reading
                        ListItem(
                            headlineContent = { Text(resumeLabel) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedLongPressItem = null
                                onResumeClick(item)
                            },
                        )

                        // Action 3: Mark Next as Read / Watched
                        if (!item.isDiscovery) {
                            ListItem(
                                headlineContent = { Text(markNextLabel) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Done,
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val target = item
                                    selectedLongPressItem = null
                                    screenModel.markNextAsRead(target, context)
                                },
                            )
                        }

                        // Action 4: Download Next Chapter / Episode
                        if (!item.isDiscovery && item.mediaType != MEDIA_TYPE_NOVEL) {
                            ListItem(
                                headlineContent = { Text(downloadNextLabel) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val target = item
                                    selectedLongPressItem = null
                                    screenModel.downloadNext(target, context)
                                },
                            )
                        }

                        // Action 5: Quick Tracking & Status
                        if (!item.isDiscovery) {
                            ListItem(
                                headlineContent = { Text("Update Tracking") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val target = item
                                    selectedLongPressItem = null
                                    onTrackerClick(target)
                                },
                            )
                        }

                        // Action 6: Library Management
                        if (!item.isDiscovery) {
                            if (!item.inLibrary) {
                                ListItem(
                                    headlineContent = { Text("Add to Library") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkBorder,
                                            contentDescription = null,
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val target = item
                                        selectedLongPressItem = null
                                        screenModel.toggleFavorite(target, context) { categories, assigned ->
                                            categoryDialogData = CategoryDialogData(
                                                item = target,
                                                selection = categories.mapAsCheckboxState { it.id in assigned }.toImmutableList(),
                                            )
                                        }
                                    },
                                )
                            } else {
                                ListItem(
                                    headlineContent = { Text(stringResource(MR.strings.action_edit_categories)) },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val target = item
                                        selectedLongPressItem = null
                                        screenModel.openChangeCategories(target) { categories, assigned ->
                                            categoryDialogData = CategoryDialogData(
                                                item = target,
                                                selection = categories.mapAsCheckboxState { it.id in assigned }.toImmutableList(),
                                            )
                                        }
                                    },
                                )
                                ListItem(
                                    headlineContent = { Text("Remove from Library") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkRemove,
                                            contentDescription = null,
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        val target = item
                                        selectedLongPressItem = null
                                        screenModel.toggleFavorite(target, context)
                                    },
                                )
                            }
                        }

                        // Action 7: Open in WebView
                        if (!item.isDiscovery) {
                            ListItem(
                                headlineContent = { Text(stringResource(MR.strings.action_open_in_web_view)) },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val target = item
                                    selectedLongPressItem = null
                                    scope.launch {
                                        val pair = screenModel.getEntryUrl(target)
                                        if (pair != null) {
                                            navigator.push(
                                                WebViewScreen(
                                                    url = pair.first,
                                                    initialTitle = target.title,
                                                    sourceId = pair.second,
                                                ),
                                            )
                                        } else {
                                            context.toast("Source URL not available")
                                        }
                                    }
                                },
                            )
                        }

                        // Action 8: Share
                        ListItem(
                            headlineContent = { Text(stringResource(MR.strings.action_share)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                val target = item
                                selectedLongPressItem = null
                                scope.launch {
                                    val pair = screenModel.getEntryUrl(target)
                                    val url = pair?.first
                                    if (!url.isNullOrBlank()) {
                                        try {
                                            val intent = url.toUri().toShareIntent(context, type = "text/plain")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.toast(e.message)
                                        }
                                    } else {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, target.title)
                                        }
                                        context.startActivity(Intent.createChooser(intent, target.title))
                                    }
                                }
                            },
                        )

                        // Action 9: Remove from History
                        if (item.historyId != null || item.lastChapterId != null || item.lastChapterNumber != null) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(MR.strings.action_remove),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val target = item
                                    selectedLongPressItem = null
                                    itemToRemoveFromHistory = target
                                },
                            )
                        }
                    }
                }
            }

            // Remove from History Confirmation Dialog (exact parity with History tab)
            itemToRemoveFromHistory?.let { item ->
                HistoryDeleteDialog(
                    onDismissRequest = { itemToRemoveFromHistory = null },
                    onDelete = { all ->
                        screenModel.removeFromHistory(item, all)
                        itemToRemoveFromHistory = null
                    },
                )
            }

            // Change Category Dialog
            categoryDialogData?.let { data ->
                ChangeCategoryDialog(
                    initialSelection = data.selection,
                    onDismissRequest = { categoryDialogData = null },
                    onEditCategories = {
                        categoryDialogData = null
                        val tab = when (data.item.mediaType) {
                            MEDIA_TYPE_ANIME -> CategoryScreen.Tab.ANIME
                            MEDIA_TYPE_NOVEL -> CategoryScreen.Tab.NOVELS
                            else -> CategoryScreen.Tab.MANGA
                        }
                        navigator.push(CategoryScreen(tab))
                    },
                    onConfirm = { include, _ ->
                        screenModel.setCategories(data.item, include)
                        categoryDialogData = null
                    },
                )
            }

            // Tracking Modal Sheet
            quickTrackerItem?.let { item ->
                if (item.mediaType == MEDIA_TYPE_ANIME) {
                    val sourceId = (item.coverData as? AnimeCover)?.sourceId ?: 0L
                    NavigatorAdaptiveSheet(
                        screen = AnimeTrackInfoDialogHomeScreen(
                            animeId = item.mangaId,
                            animeTitle = item.title,
                            sourceId = sourceId,
                        ),
                        enableSwipeDismiss = { it.lastItem is AnimeTrackInfoDialogHomeScreen },
                        onDismissRequest = { quickTrackerItem = null },
                    )
                } else {
                    val sourceId = (item.coverData as? MangaCover)?.sourceId ?: 0L
                    NavigatorAdaptiveSheet(
                        screen = TrackInfoDialogHomeScreen(
                            mangaId = item.mangaId,
                            mangaTitle = item.title,
                            sourceId = sourceId,
                        ),
                        enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                        onDismissRequest = { quickTrackerItem = null },
                    )
                }
            }
        }
    }
}

private data class CategoryDialogData(
    val item: HomeMangaItem,
    val selection: ImmutableList<CheckboxState<Category>>,
)
