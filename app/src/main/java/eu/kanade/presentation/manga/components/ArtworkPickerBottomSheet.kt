// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.manga.components

import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.i18n.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkCoverOption
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.UnifiedArtwork
import eu.kanade.tachiyomi.util.editCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.UpdateManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.image.LocalCoverManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ArtworkPickerBottomSheet(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onCustomCoverFilePick: () -> Unit,
    onCoverUpdated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { Injekt.get<ChimanewPreferences>() }
    val waterfallRepo = remember { Injekt.get<ArtworkWaterfallRepository>() }
    val customBannerPref = remember(manga.id) { preferences.mangaCustomBannerUrl(manga.id) }
    val currentCustomBanner by customBannerPref.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var artwork by remember { mutableStateOf<UnifiedArtwork?>(null) }

    LaunchedEffect(manga.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val trackerManager = Injekt.get<TrackerManager>()
            val getTracks = Injekt.get<GetTracks>()
            val tracks = getTracks.await(manga.id)
            val anilistTrack = tracks.firstOrNull { it.trackerId == trackerManager.aniList.id }
            val kitsuTrack = tracks.firstOrNull { it.trackerId == trackerManager.kitsu.id }
            val title = manga.ogTitle ?: manga.title
            val res = waterfallRepo.resolveArtwork(
                rawTitle = title,
                anilistRemoteId = anilistTrack?.remoteId?.takeIf { it > 0 },
                kitsuRemoteId = kitsuTrack?.remoteId?.takeIf { it > 0 },
            )
            artwork = res
        }
        isLoading = false
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
                    text = stringResource(MR.strings.pref_chimanew_artwork_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
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
                            text = stringResource(MR.strings.pref_chimanew_artwork_picker_covers_tab),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(MR.strings.pref_chimanew_artwork_picker_banners_tab),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            text = stringResource(MR.strings.pref_chimanew_artwork_picker_fetching),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (artwork == null || (selectedTab == 0 && artwork!!.allAvailableCovers.isEmpty()) || (selectedTab == 1 && artwork!!.allAvailableBanners.isEmpty())) {
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
                            text = stringResource(MR.strings.pref_chimanew_artwork_picker_no_art),
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
                        CoversTabContent(
                            manga = manga,
                            covers = artwork!!.allAvailableCovers,
                            isApplying = isApplying,
                            onApplyCover = { coverUrl ->
                                scope.launch {
                                    isApplying = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val client = Injekt.get<NetworkHelper>().client
                                            val response = client.newCall(GET(coverUrl)).awaitSuccess()
                                            response.body.byteStream().use { stream ->
                                                manga.editCover(
                                                    coverManager = Injekt.get<LocalCoverManager>(),
                                                    stream = stream,
                                                    updateManga = Injekt.get<UpdateManga>(),
                                                    coverCache = Injekt.get<CoverCache>(),
                                                )
                                            }
                                        }
                                        context.toast(context.stringResource(MR.strings.pref_chimanew_artwork_picker_cover_applied))
                                        onCoverUpdated()
                                    } catch (e: Exception) {
                                        logcat(throwable = e) { "Failed to set custom cover from URL: $coverUrl" }
                                    } finally {
                                        isApplying = false
                                    }
                                }
                            },
                            onResetCover = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val coverCache = Injekt.get<CoverCache>()
                                        val updateManga = Injekt.get<UpdateManga>()
                                        coverCache.deleteCustomCover(manga.id)
                                        updateManga.awaitUpdateCoverLastModified(manga.id)
                                    }
                                    context.toast(context.stringResource(MR.strings.pref_chimanew_artwork_picker_reset_cover))
                                    onCoverUpdated()
                                }
                            },
                            onCustomCoverFilePick = onCustomCoverFilePick,
                        )
                    }
                    1 -> {
                        // Banners Tab
                        BannersTabContent(
                            banners = artwork!!.allAvailableBanners,
                            currentCustomBanner = currentCustomBanner,
                            onSelectBanner = { bannerUrl ->
                                customBannerPref.set(bannerUrl)
                                context.toast(context.stringResource(MR.strings.pref_chimanew_artwork_picker_banner_applied))
                            },
                            onResetBanner = {
                                customBannerPref.delete()
                                context.toast(context.stringResource(MR.strings.pref_chimanew_artwork_picker_reset_banner))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoversTabContent(
    manga: Manga,
    covers: List<ArtworkCoverOption>,
    isApplying: Boolean,
    onApplyCover: (String) -> Unit,
    onResetCover: () -> Unit,
    onCustomCoverFilePick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onCustomCoverFilePick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick File", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onResetCover,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Cover", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of covers
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        ) {
            items(covers, key = { it.url }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isApplying) { onApplyCover(item.url) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            // Pill badge showing source
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.TopStart)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = item.source,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannersTabContent(
    banners: List<String>,
    currentCustomBanner: String,
    onSelectBanner: (String) -> Unit,
    onResetBanner: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = onResetBanner,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset to Auto Waterfall", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        ) {
            items(banners, key = { it }) { bannerUrl ->
                val isSelected = currentCustomBanner == bannerUrl
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBanner(bannerUrl) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        CardDefaults.outlinedCardBorder()
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Top gradient overlay for badge readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent),
                                    ),
                                ),
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
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
