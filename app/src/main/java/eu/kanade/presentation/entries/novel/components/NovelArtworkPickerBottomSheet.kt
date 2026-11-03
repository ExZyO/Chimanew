// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.entries.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import chimahon.novel.interactor.UpdateNovel
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewHomeScreenModel
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkCoverOption
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.UnifiedArtwork
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.novel.model.Novel
import tachiyomi.domain.novel.model.NovelUpdate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun NovelArtworkPickerBottomSheet(
    novel: Novel,
    onDismissRequest: () -> Unit,
    onCustomCoverFilePick: () -> Unit,
    onCustomBannerFilePick: () -> Unit = {},
    onCoverUpdated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { Injekt.get<ChimanewPreferences>() }
    val waterfallRepo = remember { Injekt.get<ArtworkWaterfallRepository>() }
    val updateNovel = remember { Injekt.get<UpdateNovel>() }

    val customBannerPref = remember(novel.id) { preferences.novelCustomBannerUrl(novel.id) }
    val customHeroBannerPref = remember(novel.id) { preferences.novelCustomHeroBannerUrl(novel.id) }
    val currentCustomBanner by customBannerPref.collectPrefAsState()
    val currentCustomHeroBanner by customHeroBannerPref.collectPrefAsState()

    val excludedCoversPref = remember(novel.id) { preferences.novelExcludedCoverSources(novel.id) }
    val excludedBannersPref = remember(novel.id) { preferences.novelExcludedBannerSources(novel.id) }
    val excludedCovers by excludedCoversPref.collectPrefAsState()
    val excludedBanners by excludedBannersPref.collectPrefAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var artwork by remember { mutableStateOf<UnifiedArtwork?>(null) }

    val fetchArtwork: (Boolean) -> Unit = { force ->
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val res = waterfallRepo.resolveArtwork(
                    rawTitle = novel.title,
                    mangaId = novel.id,
                    defaultCoverUrl = novel.thumbnailUrl,
                    forceRefresh = force,
                    mediaType = MEDIA_TYPE_NOVEL,
                )
                artwork = res
            }
            isLoading = false
            if (force) {
                context.toast("Artwork refreshed")
            }
        }
    }

    LaunchedEffect(novel.id) {
        fetchArtwork(false)
    }

    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Novel Artwork Selection",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { fetchArtwork(true) },
                    enabled = !isLoading,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh artwork",
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_close),
                    )
                }
            }

            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Covers",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Banners",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Exclusions",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedCovers = remember(artwork, excludedCovers) {
                val all = artwork?.allAvailableCovers ?: emptyList()
                if (excludedCovers.contains("ALL")) {
                    all.filter { it.source.equals("Default", ignoreCase = true) }
                } else {
                    all.filterNot { it.source.uppercase() in excludedCovers }
                }
            }

            val displayedBanners = remember(artwork, excludedBanners) {
                if (excludedBanners.contains("ALL")) {
                    emptyList()
                } else {
                    val filteredOpts = artwork?.bannerOptions?.filterNot { it.source.uppercase() in excludedBanners }
                    if (!filteredOpts.isNullOrEmpty()) {
                        filteredOpts.map { it.url }
                    } else {
                        artwork?.allAvailableBanners ?: emptyList()
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Fetching novel artwork...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (selectedTab != 2 && (artwork == null || (selectedTab == 0 && displayedCovers.isEmpty()) || (selectedTab == 1 && displayedBanners.isEmpty()))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = if (selectedTab == 0 && excludedCovers.contains("ALL")) {
                                "All remote novel cover sources are excluded for this entry."
                            } else if (selectedTab == 1 && excludedBanners.contains("ALL")) {
                                "All remote novel banner sources are excluded for this entry."
                            } else {
                                "No novel artwork found for this title"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onCustomCoverFilePick) {
                                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Gallery")
                            }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // Covers Tab
                        NovelCoversTabContent(
                            novel = novel,
                            covers = displayedCovers,
                            isApplying = isApplying,
                            onApplyCover = { coverUrl ->
                                scope.launch {
                                    isApplying = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            updateNovel.await(NovelUpdate(id = novel.id, thumbnailUrl = coverUrl))
                                        }
                                        context.imageLoader.memoryCache?.clear()
                                        ChimanewHomeScreenModel.triggerRefresh()
                                        context.toast("Cover applied")
                                        onCoverUpdated()
                                    } catch (e: Exception) {
                                        logcat(throwable = e) { "Failed to set custom novel cover: $coverUrl" }
                                    } finally {
                                        isApplying = false
                                    }
                                }
                            },
                            onResetCover = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        updateNovel.await(NovelUpdate(id = novel.id, thumbnailUrl = null))
                                    }
                                    context.imageLoader.memoryCache?.clear()
                                    ChimanewHomeScreenModel.triggerRefresh()
                                    context.toast("Cover reset to default")
                                    onCoverUpdated()
                                }
                            },
                            onCustomCoverFilePick = onCustomCoverFilePick,
                        )
                    }
                    1 -> {
                        // Banners Tab
                        NovelBannersTabContent(
                            banners = displayedBanners,
                            currentCustomBanner = currentCustomBanner,
                            currentCustomHeroBanner = currentCustomHeroBanner,
                            onSelectBanner = { bannerUrl ->
                                customBannerPref.set(bannerUrl)
                                context.imageLoader.memoryCache?.clear()
                                ChimanewHomeScreenModel.notifyBannerUpdated(novel.id, bannerUrl, eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL)
                                context.toast("Banner applied")
                                onCoverUpdated()
                            },
                            onSelectHeroBanner = { bannerUrl ->
                                customHeroBannerPref.set(bannerUrl)
                                context.imageLoader.memoryCache?.clear()
                                ChimanewHomeScreenModel.notifyBannerUpdated(novel.id, bannerUrl, eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL)
                                context.toast("Hero banner applied")
                                onCoverUpdated()
                            },
                            onResetBanner = {
                                customBannerPref.delete()
                                customHeroBannerPref.delete()
                                context.imageLoader.memoryCache?.clear()
                                ChimanewHomeScreenModel.notifyBannerUpdated(novel.id, null, eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL)
                                context.toast("Banner reset")
                                onCoverUpdated()
                            },
                            onCustomBannerFilePick = onCustomBannerFilePick,
                            onToggleDisableBanner = {
                                if (currentCustomBanner == "NONE" || excludedBanners.contains("ALL")) {
                                    if (currentCustomBanner == "NONE") customBannerPref.delete()
                                    val current = excludedBanners.toMutableSet().apply { remove("ALL") }
                                    excludedBannersPref.set(current)
                                    context.toast("Banner enabled")
                                } else {
                                    customBannerPref.set("NONE")
                                    context.toast("Banner disabled for this novel")
                                }
                                context.imageLoader.memoryCache?.clear()
                                ChimanewHomeScreenModel.notifyBannerUpdated(novel.id, null, eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL)
                                onCoverUpdated()
                            },
                            isBannerDisabled = currentCustomBanner == "NONE" || excludedBanners.contains("ALL"),
                        )
                    }
                    2 -> {
                        // Excluded Sources Tab
                        NovelArtworkExclusionsTabContent(
                            novelId = novel.id,
                            preferences = preferences,
                            onExclusionsChanged = {
                                fetchArtwork(false)
                                context.imageLoader.memoryCache?.clear()
                                ChimanewHomeScreenModel.triggerRefresh()
                                onCoverUpdated()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelCoversTabContent(
    novel: Novel,
    covers: List<ArtworkCoverOption>,
    isApplying: Boolean,
    onApplyCover: (String) -> Unit,
    onResetCover: () -> Unit,
    onCustomCoverFilePick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onCustomCoverFilePick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick File", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onResetCover,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
        ) {
            items(covers, key = { it.url }) { cover ->
                NovelCoverGridItem(
                    cover = cover,
                    isSelected = cover.url == novel.thumbnailUrl,
                    isApplying = isApplying,
                    onClick = { onApplyCover(cover.url) },
                )
            }
        }
    }
}

@Composable
private fun NovelCoverGridItem(
    cover: ArtworkCoverOption,
    isSelected: Boolean,
    isApplying: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isApplying, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cover.url)
                    .crossfade(true)
                    .build(),
                contentDescription = "${cover.source} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cover.source,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!cover.subtitle.isNullOrBlank()) {
            Text(
                text = cover.subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NovelBannersTabContent(
    banners: List<String>,
    currentCustomBanner: String,
    currentCustomHeroBanner: String,
    onSelectBanner: (String) -> Unit,
    onSelectHeroBanner: (String) -> Unit,
    onResetBanner: () -> Unit,
    onCustomBannerFilePick: () -> Unit,
    onToggleDisableBanner: () -> Unit,
    isBannerDisabled: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onCustomBannerFilePick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick File", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { onSelectBanner("COVER") },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (currentCustomBanner == "COVER") "Cover Active" else "Use Cover", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onToggleDisableBanner,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isBannerDisabled) "Enable Banner" else "Disable Banner", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onResetBanner,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Auto", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
        ) {
            items(banners, key = { it }) { bannerUrl ->
                val isDetails = bannerUrl == currentCustomBanner
                val isHero = bannerUrl == currentCustomHeroBanner
                val isSelected = isDetails || isHero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 7f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelectBanner(bannerUrl) },
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bannerUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Novel banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isDetails || isHero) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(999.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .align(Alignment.TopEnd),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active Banner",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isDetails && isHero -> "Details & Hero"
                                        isDetails -> "Details"
                                        else -> "Hero Carousel"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }

                    // Bottom action buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                ),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { onSelectBanner(bannerUrl) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                            ) {
                                Text("Apply to Details", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { onSelectHeroBanner(bannerUrl) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                            ) {
                                Text("Apply to Hero", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelArtworkExclusionsTabContent(
    novelId: Long,
    preferences: ChimanewPreferences,
    onExclusionsChanged: () -> Unit,
) {
    val coverExclusionsPref = remember(novelId) { preferences.novelExcludedCoverSources(novelId) }
    val bannerExclusionsPref = remember(novelId) { preferences.novelExcludedBannerSources(novelId) }
    val excludedCovers by coverExclusionsPref.collectPrefAsState()
    val excludedBanners by bannerExclusionsPref.collectPrefAsState()

    val coverProviders = listOf(
        "ALL" to "All Remote Sources (Default Only)",
        "BOOKWALKER" to "BookWalker Japan",
        "NAVER" to "Naver Webnovel",
        "KUAIKAN" to "Kuaikan",
        "BANGUMI" to "Bangumi",
        "ANILIST" to "AniList",
        "KITSU" to "Kitsu",
    )

    val bannerProviders = listOf(
        "ALL" to "All Remote Banners",
        "BOOKWALKER" to "BookWalker Japan",
        "NAVER" to "Naver Webnovel",
        "KUAIKAN" to "Kuaikan",
        "BANGUMI" to "Bangumi",
        "ANILIST" to "AniList",
        "KITSU" to "Kitsu",
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    ) {
        item {
            Text(
                text = "Exclude Novel Cover Sources",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = "Checked sources will not be queried for this novel's cover.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        items(coverProviders, key = { "cover_${it.first}" }) { (id, label) ->
            val isChecked = if (id == "ALL") excludedCovers.contains("ALL") else excludedCovers.contains("ALL") || excludedCovers.contains(id)
            val isIndividuallyDisabled = id != "ALL" && excludedCovers.contains("ALL")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isIndividuallyDisabled) {
                        val current = excludedCovers.toMutableSet()
                        if (id == "ALL") {
                            if (current.contains("ALL")) current.remove("ALL") else { current.clear(); current.add("ALL") }
                        } else {
                            if (current.contains(id)) current.remove(id) else current.add(id)
                        }
                        coverExclusionsPref.set(current)
                        onExclusionsChanged()
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isChecked, onCheckedChange = null, enabled = !isIndividuallyDisabled)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isIndividuallyDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Exclude Novel Banner Sources",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = "Checked sources will not be queried for this novel's banner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        items(bannerProviders, key = { "banner_${it.first}" }) { (id, label) ->
            val isChecked = if (id == "ALL") excludedBanners.contains("ALL") else excludedBanners.contains("ALL") || excludedBanners.contains(id)
            val isIndividuallyDisabled = id != "ALL" && excludedBanners.contains("ALL")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !isIndividuallyDisabled) {
                        val current = excludedBanners.toMutableSet()
                        if (id == "ALL") {
                            if (current.contains("ALL")) current.remove("ALL") else { current.clear(); current.add("ALL") }
                        } else {
                            if (current.contains(id)) current.remove(id) else current.add(id)
                        }
                        bannerExclusionsPref.set(current)
                        onExclusionsChanged()
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isChecked, onCheckedChange = null, enabled = !isIndividuallyDisabled)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isIndividuallyDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
