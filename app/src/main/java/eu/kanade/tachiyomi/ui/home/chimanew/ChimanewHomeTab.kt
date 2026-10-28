// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.saveable.rememberSaveable
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
import eu.kanade.presentation.more.settings.screen.SettingsNuvioScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioHeroSection
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioMangaShelf
import eu.kanade.tachiyomi.ui.home.chimanew.components.NuvioQuickTrackerSheet
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object ChimanewHomeTab : Tab {
    private fun readResolve(): Any = ChimanewHomeTab

    private val scrollToTopChannel = Channel<Unit>(Channel.CONFLATED)

    override suspend fun onReselect(navigator: cafe.adriel.voyager.navigator.Navigator) {
        scrollToTopChannel.send(Unit)
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val onResumeClick: (HomeMangaItem) -> Unit = { item ->
            // EaZy Nuvio+ / Chimanew: Instant launch if chapter ID is in progress; zero DB query delay
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

        val onDetailsClick: (HomeMangaItem) -> Unit = { item ->
            navigator.push(MangaScreen(item.mangaId))
        }

        // EaZy Nuvio+ / Chimanew: Card tap action setting toggle (0 = Details page, 1 = Reader)
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
        LaunchedEffect(state.heroItems.size, state.shelves.size) {
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                // 1. Top Hero Section
                if (state.heroItems.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        NuvioHeroSection(
                            items = state.heroItems,
                            heroStyle = state.heroStyle,
                            heroArtworkSources = state.heroArtworkSources,
                            onItemClick = onCardClick,
                            onResumeClick = onResumeClick,
                            onQuickStatusClick = { item -> quickTrackerItem = item },
                        )
                    }
                }

                // 2. Dynamic Shelves (ordered and filtered according to user preferences)
                items(
                    items = state.shelves,
                    key = { it.id },
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
                if (state.heroItems.isEmpty() && !state.isLoading) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Welcome to Chimanew",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start reading or add manga to your library to populate your personalized home feed.",
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
                                Text(text = "Browse Extensions")
                            }
                        }
                    }
                }

                // Bottom spacer for navigation bar clearance
                item(key = "bottom_nav_spacer") {
                    Spacer(modifier = Modifier.height(96.dp))
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
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = { navigator.push(GlobalSearchScreen()) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
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

            // Long-Press Options Bottom Sheet
            selectedLongPressItem?.let { item ->
                ModalBottomSheet(
                    onDismissRequest = { selectedLongPressItem = null },
                    sheetState = sheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    Text(
                                        text = "Chapter ${item.lastChapterNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Action 1: Open Details Page
                        ListItem(
                            headlineContent = { Text("Manga Details") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedLongPressItem = null
                                navigator.push(MangaScreen(item.mangaId))
                            },
                        )

                        // Action 2: Resume / Start Reading
                        ListItem(
                            headlineContent = { Text("Resume Reading") },
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
                    }
                }
            }

            // EaZy Nuvio+ / Chimanew: Quick-Action Tracker Modal Bottom Sheet
            quickTrackerItem?.let { item ->
                NuvioQuickTrackerSheet(
                    item = item,
                    onDismissRequest = { quickTrackerItem = null },
                    onStatusSelected = { status ->
                        screenModel.updateMangaTrackStatus(item.mangaId, status)
                    },
                    onToggleLibrary = {
                        screenModel.toggleLibraryBookmark(item.mangaId)
                    },
                    onOpenFullDetails = {
                        quickTrackerItem = null
                        onDetailsClick(item)
                    },
                )
            }
        }
    }
}
