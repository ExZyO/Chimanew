package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavSection
import eu.kanade.domain.ui.model.NavTabLayout
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.BrowseViewMode
import eu.kanade.tachiyomi.ui.dictionary.DictionaryTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.entries.anime.AnimeTab
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewHomeTab
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.LibraryViewMode
import eu.kanade.tachiyomi.ui.library.novels.NovelsTab
import eu.kanade.tachiyomi.ui.libraryUpdateError.LibraryUpdateErrorScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object HomeScreen : Screen() {
    private fun readResolve(): Any = HomeScreen

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 200
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    private val ALL_TABS_MAP = mapOf(
        NavTabLayout.KEY_HOME to ChimanewHomeTab, // EaZy Nuvio+ / Chimanew
        NavTabLayout.KEY_LIBRARY to LibraryTab,
        NavTabLayout.KEY_NOVELS to NovelsTab,
        NavTabLayout.KEY_ANIME to AnimeTab,
        NavTabLayout.KEY_UPDATES to UpdatesTab,
        NavTabLayout.KEY_HISTORY to HistoryTab,
        NavTabLayout.KEY_BROWSE to BrowseTab,
        NavTabLayout.KEY_DICTIONARY to DictionaryTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // SY -->
        val scope = rememberCoroutineScope()
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val alwaysShowLabel by remember {
            uiPreferences.bottomBarLabels().asState(scope)
        }

        val navTabLayoutStr by uiPreferences.navTabLayout().collectAsState()
        val navStartScreenKey by uiPreferences.navStartScreen().collectAsState()
        val useConsolidatedLibrary by uiPreferences.useConsolidatedLibrary().asState(scope)

        val navbarTabs = remember(navTabLayoutStr, useConsolidatedLibrary) {
            val layout = NavTabLayout.parse(navTabLayoutStr)
            val navbarKeys = layout.getKeysForSection(NavSection.NAVBAR)
                .filterNot { useConsolidatedLibrary && (it == NavTabLayout.KEY_NOVELS || it == NavTabLayout.KEY_ANIME) }
            navbarKeys.mapNotNull { ALL_TABS_MAP[it] } + listOf(MoreTab)
        }

        val startTab = remember(navStartScreenKey, navbarTabs) {
            ALL_TABS_MAP[navStartScreenKey]?.takeIf { it in navbarTabs } ?: navbarTabs.first()
        }
        // SY <--

        TabNavigator(
            tab = startTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            // EaZy Nuvio+ / Chimanew: Nuvio Dynamic Island & iOS Liquid Sliding Glass Dock
            val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }
            val navbarAutoHideOnScroll by chimanewPreferences.navbarAutoHideOnScroll().collectAsState()
            var isDockCollapsed by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(tabNavigator.current) {
                isDockCollapsed = false
            }

            // EaZy Nuvio+ / Chimanew: Buffered scroll accumulator to prevent jank & thrashing
            val autoHideScrollConnection = remember(navbarAutoHideOnScroll) {
                object : NestedScrollConnection {
                    private var scrollAccumulator = 0f
                    private val scrollThreshold = 45f

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (!navbarAutoHideOnScroll) {
                            if (isDockCollapsed) isDockCollapsed = false
                            scrollAccumulator = 0f
                            return Offset.Zero
                        }
                        val deltaY = consumed.y + available.y
                        if (deltaY == 0f) return Offset.Zero

                        // Reset accumulator when user reverses scroll direction
                        if (deltaY < 0f && scrollAccumulator > 0f) scrollAccumulator = 0f
                        if (deltaY > 0f && scrollAccumulator < 0f) scrollAccumulator = 0f

                        scrollAccumulator += deltaY

                        if (scrollAccumulator < -scrollThreshold && !isDockCollapsed) {
                            isDockCollapsed = true
                            scrollAccumulator = 0f
                        } else if (scrollAccumulator > scrollThreshold && isDockCollapsed) {
                            isDockCollapsed = false
                            scrollAccumulator = 0f
                        }
                        return Offset.Zero
                    }
                }
            }

            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    modifier = Modifier.nestedScroll(autoHideScrollConnection),
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                navbarTabs
                                    .fastForEach {
                                        NavigationRailItem(it/* SY --> */, alwaysShowLabel/* SY <-- */)
                                    }
                            }
                        }
                    },
                    // EaZy Nuvio+ / Chimanew: Floating dock overlay eliminates the black cutout
                    bottomBar = {},
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentPadding.calculateTopPadding()),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    initialScale = 1f,
                                    durationMillis = TAB_FADE_DURATION,
                                ) togetherWith
                                    materialFadeThroughOut(durationMillis = TAB_FADE_DURATION)
                            },
                            label = "tabContent",
                            contentKey = { it.key },
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }

                        // EaZy Nuvio+ / Chimanew: Nuvio Dynamic Island & iOS Liquid Sliding Glass Dock Overlay
                        if (!isTabletUi()) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                ) + fadeOut(),
                            ) {
                                NuvioLiquidGlassDock(
                                    tabs = navbarTabs,
                                    isCollapsed = isDockCollapsed,
                                    useConsolidatedLibrary = useConsolidatedLibrary,
                                )
                            }
                        }
                    }
                }
            }

            val goToStartTab = { tabNavigator.current = startTab }
            BackHandler(
                enabled = tabNavigator.current != startTab,
                onBack = goToStartTab,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = LibraryTab
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            Tab.Novels -> NovelsTab
                            Tab.Anime -> AnimeTab
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            Tab.Dictionary -> DictionaryTab
                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More) {
                            if (it.toDownloads) {
                                navigator.push(DownloadQueueScreen)
                            } else if (it.toStats) {
                                navigator.push(eu.kanade.tachiyomi.ui.stats.StatsScreen())
                            } else if (it.toLibraryUpdateErrors) {
                                navigator.push(LibraryUpdateErrorScreen())
                            }
                        }
                    }
                }
            }
        }
    }

    // EaZy Nuvio+ / Chimanew: Nuvio Dynamic Island & iOS Liquid Sliding Glass Dock
    @Composable
    private fun NuvioLiquidGlassDock(
        tabs: List<eu.kanade.presentation.util.Tab>,
        isCollapsed: Boolean,
        useConsolidatedLibrary: Boolean,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val isDark = isSystemInDarkTheme()

        val dockWidth by animateDpAsState(
            targetValue = if (isCollapsed) 300.dp else 420.dp,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "nuvio_dock_width",
        )
        val dockHeight by animateDpAsState(
            targetValue = if (isCollapsed) 50.dp else 62.dp,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "nuvio_dock_height",
        )

        // Dynamic surface & border matching Chimanew theme
        val surfaceColor = MaterialTheme.colorScheme.surface
        val dockSurfaceColor = if (isDark) {
            surfaceColor.copy(alpha = 0.82f)
        } else {
            surfaceColor.copy(alpha = 0.88f)
        }
        val dockBorderColor = if (isDark) {
            Color.White.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        }

        Surface(
            modifier = Modifier
                .width(dockWidth)
                .height(dockHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.15f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.75f else 0.25f),
                ),
            shape = RoundedCornerShape(999.dp),
            color = dockSurfaceColor,
            border = BorderStroke(1.dp, dockBorderColor),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top specular catch-light sheen (Nuvio glass depth effect)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.85f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    (if (isDark) Color.White else MaterialTheme.colorScheme.primary).copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                var glideIndex by remember { mutableStateOf<Float?>(null) }
                var glideStretch by remember { mutableFloatStateOf(1.0f) }

                val tabCount = tabs.size.coerceAtLeast(1)
                val activeIdx = tabs.indexOfFirst { it::class == tabNavigator.current::class }.coerceAtLeast(0)
                val targetIndex = glideIndex ?: activeIdx.toFloat()

                val animatedIndex by animateFloatAsState(
                    targetValue = targetIndex,
                    animationSpec = if (glideIndex != null) {
                        // 1:1 Instant finger tracking with no lag
                        spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessHigh)
                    } else {
                        // Critically-damped spring settle when finger is released
                        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    },
                    label = "sliding_pill_index",
                )

                val animatedStretch by animateFloatAsState(
                    targetValue = if (glideIndex != null) glideStretch else 1.0f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
                    label = "sliding_pill_stretch",
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .pointerInput(tabCount, activeIdx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val dockPixelWidth = size.width.toFloat()
                                val tabWidthPx = dockPixelWidth / tabCount

                                var lastX = down.position.x
                                val initialIdx = ((lastX - (tabWidthPx / 2f)) / tabWidthPx).coerceIn(0f, (tabCount - 1).toFloat())
                                glideIndex = initialIdx
                                glideStretch = 1.04f

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        // Released! Snap to closest index
                                        val finalIdx = (glideIndex ?: activeIdx.toFloat()).roundToInt().coerceIn(0, tabCount - 1)
                                        glideIndex = null
                                        glideStretch = 1.0f
                                        if (finalIdx != activeIdx) {
                                            tabNavigator.current = tabs[finalIdx]
                                        } else {
                                            scope.launch { tabs[finalIdx].onReselect(navigator) }
                                        }
                                        change.consume()
                                        break
                                    }
                                    val currentX = change.position.x
                                    val deltaX = abs(currentX - lastX)
                                    lastX = currentX

                                    val continuousIdx = ((currentX - (tabWidthPx / 2f)) / tabWidthPx).coerceIn(0f, (tabCount - 1).toFloat())
                                    glideIndex = continuousIdx
                                    glideStretch = (1.02f + deltaX * 0.005f).coerceIn(1.02f, 1.14f)
                                    change.consume()
                                }
                            }
                        },
                ) {
                    val tabWidth = maxWidth / tabCount

                    // Sliding Liquid Glass Pill Indicator
                    val pillBgColor = if (isDark) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    }
                    val pillBorderColor = if (isDark) {
                        Color.White.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(tabWidth)
                            .offset(x = tabWidth * animatedIndex)
                            .graphicsLayer {
                                scaleX = animatedStretch
                            }
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBgColor)
                            .border(
                                BorderStroke(1.dp, pillBorderColor),
                                shape = RoundedCornerShape(999.dp),
                            ),
                    ) {
                        // Top hairline specular sheen highlight on the sliding pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 1.dp)
                                .fillMaxWidth(0.65f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            (if (isDark) Color.White else MaterialTheme.colorScheme.primary).copy(alpha = 0.70f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }

                    // Tabs Row
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.fastForEach { tab ->
                            val isCurrentTab = tabNavigator.current::class == tab::class
                            val currentHoveredIdx = glideIndex?.roundToInt() ?: activeIdx
                            val isTabActive = currentHoveredIdx == tabs.indexOf(tab)

                            val onClickTab: () -> Unit = {
                                if (currentHoveredIdx != tabs.indexOf(tab)) {
                                    tabNavigator.current = tab
                                } else {
                                    scope.launch { tab.onReselect(navigator) }
                                }
                            }

                            val itemTint by animateColorAsState(
                                targetValue = if (isTabActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "dock_item_tint",
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onClickTab,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    CompositionLocalProvider(LocalContentColor provides itemTint) {
                                        if (tab == LibraryTab && useConsolidatedLibrary) {
                                            LibraryNavigationIcon(
                                                onClick = onClickTab,
                                                onModeSelected = { mode ->
                                                    if (!isCurrentTab) tabNavigator.current = LibraryTab
                                                    scope.launch { LibraryTab.selectLibraryMode(mode) }
                                                },
                                            )
                                        } else if (tab == BrowseTab) {
                                            BrowseNavigationIcon(
                                                onClick = onClickTab,
                                                onModeSelected = { mode ->
                                                    if (!isCurrentTab) tabNavigator.current = BrowseTab
                                                    scope.launch { BrowseTab.selectBrowseMode(mode) }
                                                },
                                            )
                                        } else {
                                            NavigationIconItem(tab)
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = !isCollapsed,
                                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                                expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                                        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                               shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tab.options.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isTabActive) FontWeight.Bold else FontWeight.Medium,
                                                    color = itemTint,
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LibraryNavigationIcon(
        onClick: () -> Unit,
        onModeSelected: (LibraryViewMode) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val selectedMode = LibraryViewMode.entries.getOrElse(
            Injekt.get<LibraryPreferences>().lastUsedLibraryMode().get(),
        ) { LibraryViewMode.Manga }

        Box {
            Box(
                modifier = Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { expanded = true },
                ),
            ) {
                NavigationIconItem(LibraryTab)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                LibraryViewMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(mode.labelRes),
                                fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            expanded = false
                            onModeSelected(mode)
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun BrowseNavigationIcon(
        onClick: () -> Unit,
        onModeSelected: (BrowseViewMode) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val selectedMode = BrowseViewMode.entries.getOrElse(
            Injekt.get<UiPreferences>().lastUsedBrowseMode().get(),
        ) { BrowseViewMode.Sources }

        Box {
            Box(
                modifier = Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { expanded = true },
                ),
            ) {
                NavigationIconItem(BrowseTab)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                BrowseViewMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(mode.labelRes),
                                fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            expanded = false
                            onModeSelected(mode)
                        },
                    )
                }
            }
        }
    }

    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = /* SY --> */alwaysShowLabel, /* SY <-- */
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount().changes(),
                                pref.newUpdatesCount().changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            Injekt.get<SourcePreferences>().extensionUpdatesCount().changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                // TODO: https://issuetracker.google.com/u/0/issues/316327367
                tint = LocalContentColor.current,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data object Novels : Tab
        data object Anime : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data object Dictionary : Tab
        data class More(
            val toDownloads: Boolean,
            val toStats: Boolean = false,
            // KMK -->
            val toLibraryUpdateErrors: Boolean = false,
            // KMK <--
        ) : Tab
    }
}
