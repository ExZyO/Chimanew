package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.home.chimanew.data.CommunityRating
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.key
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.ui.home.chimanew.data.UnifiedArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.track.interactor.GetTracks
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.copyToClipboard
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import tachiyomi.domain.manga.model.MangaCover as DomainMangaCover

@Composable
fun MangaInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    // KMK -->
    isSourceIncognito: Boolean,
    // KMK <--
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // KMK -->
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (DomainMangaCover) -> Unit,
    coverRatio: MutableFloatState,
    // KMK <--
    readingTime: String? = null,
) {
    // KMK -->
    val usePanoramaCover by Injekt.get<UiPreferences>().usePanoramaCoverMangaInfo().collectAsState()
    val topAlignCover by Injekt.get<UiPreferences>().topAlignCover().collectAsState()
    // KMK <--

    // EaZy Nuvio+ / Chimanew: Settings-controlled AniList artwork & backdrop styling
    val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }
    val useAnilistArtwork by chimanewPreferences.useAnilistArtworkInDetails().collectAsState()
    val detailsBackdropStyle by chimanewPreferences.detailsBackdropStyle().collectAsState()
    val detailsBackdropArtwork by chimanewPreferences.detailsBackdropArtwork().collectAsState()
    val ambientCanvasStyle by chimanewPreferences.ambientCanvasStyle().collectAsState()
    val customBannerUrl by chimanewPreferences.mangaCustomBannerUrl(manga.id).collectAsState()
    val artworkWaterfallOrder by chimanewPreferences.artworkWaterfallOrder().collectAsState()
    val artworkBannerOrder by chimanewPreferences.artworkBannerOrder().collectAsState()
    val artworkCoverOrder by chimanewPreferences.artworkCoverOrder().collectAsState()
    val showCommunityRatings by chimanewPreferences.showCommunityRatings().collectAsState()
    val showRatingsInDetails by chimanewPreferences.showRatingsInDetails().collectAsState()
    val communityRatingFormat by chimanewPreferences.communityRatingFormat().collectAsState()
    val heroAllowCroppedFallback by chimanewPreferences.heroAllowCroppedFallback().collectAsState()

    val waterfallRepo = remember { Injekt.get<ArtworkWaterfallRepository>() }
    val mangaTitle = remember(manga) { manga.ogTitle ?: manga.title }
    val cachedArtwork = remember(mangaTitle, artworkBannerOrder, artworkCoverOrder, artworkWaterfallOrder) {
        waterfallRepo.getCached(mangaTitle)
    }

    val unifiedArtwork by produceState<UnifiedArtwork?>(
        initialValue = if (useAnilistArtwork) cachedArtwork else null,
        manga.id,
        useAnilistArtwork,
        artworkWaterfallOrder,
        artworkBannerOrder,
        artworkCoverOrder,
        detailsBackdropArtwork,
    ) {
        if (!useAnilistArtwork) {
            value = null
            return@produceState
        }
        withContext(Dispatchers.IO) {
            val trackerManager = Injekt.get<TrackerManager>()
            val getTracks = Injekt.get<GetTracks>()
            val tracks = getTracks.await(manga.id)
            val anilistTrack = tracks.firstOrNull { it.trackerId == trackerManager.aniList.id }
            val malTrack = tracks.firstOrNull { it.trackerId == trackerManager.myAnimeList.id }
            val kitsuTrack = tracks.firstOrNull { it.trackerId == trackerManager.kitsu.id }
            val art = waterfallRepo.resolveArtwork(
                rawTitle = mangaTitle,
                anilistRemoteId = anilistTrack?.remoteId?.takeIf { it > 0 },
                kitsuRemoteId = kitsuTrack?.remoteId?.takeIf { it > 0 },
                malRemoteId = malTrack?.remoteId?.takeIf { it > 0 },
            )
            value = art
        }
    }

    val chosenArtworkModel: Any? = when {
        !customBannerUrl.isNullOrBlank() -> customBannerUrl
        !useAnilistArtwork || unifiedArtwork == null -> if (heroAllowCroppedFallback) manga else null
        detailsBackdropArtwork == 0 && !unifiedArtwork?.bannerUrl.isNullOrBlank() -> unifiedArtwork!!.bannerUrl!!
        detailsBackdropArtwork == 1 && !unifiedArtwork?.posterUrl.isNullOrBlank() -> unifiedArtwork!!.posterUrl!!
        detailsBackdropArtwork == 2 -> manga
        !unifiedArtwork?.bannerUrl.isNullOrBlank() -> unifiedArtwork!!.bannerUrl!!
        !unifiedArtwork?.posterUrl.isNullOrBlank() -> if (heroAllowCroppedFallback) unifiedArtwork!!.posterUrl!! else null
        else -> if (heroAllowCroppedFallback) manga else null
    }

    val ambientArtworkModel: Any = if (useAnilistArtwork && !unifiedArtwork?.posterUrl.isNullOrBlank()) {
        unifiedArtwork!!.posterUrl!!
    } else {
        manga
    }

    // Determine if rendering an official wide landscape banner or falling back to ambient canvas
    val isWideBanner = (chosenArtworkModel != manga) && (chosenArtworkModel is String) && (
        chosenArtworkModel == customBannerUrl ||
        (unifiedArtwork != null && chosenArtworkModel == unifiedArtwork?.bannerUrl)
    )

    val activeStyle = detailsBackdropStyle
    val backdropAlpha = when (activeStyle) {
        0 -> 0.55f // Cinematic Fade
        1 -> 0.35f // Soft Blur
        2 -> 0.85f // Full Opacity + Scrims
        else -> 0.20f
    }

    val backdropBlur = when (activeStyle) {
        0 -> 0.dp // Cinematic Fade - Crisp
        1 -> 18.dp // Soft Atmospheric Blur
        2 -> 0.dp // Full Opacity - Crisp
        else -> 7.dp
    }

    val detailsBackdropScale by chimanewPreferences.detailsBackdropScale().collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceTintColor = MaterialTheme.colorScheme.surfaceTint
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier) {
        // Backdrop
        val backdropGradientColors = listOf(
            Color.Transparent,
            backgroundColor,
        )
        // EaZy Nuvio+ / Chimanew: resilient fallback to local manga cover if remote banner/artwork fails to load
        var artworkLoadFailed by remember(chosenArtworkModel) { mutableStateOf(false) }
        val effectiveArtworkModel = if (artworkLoadFailed) {
            if (heroAllowCroppedFallback) manga else null
        } else {
            chosenArtworkModel
        }
        var bannerAspectRatio by remember(effectiveArtworkModel) { mutableFloatStateOf(0f) }

        key(effectiveArtworkModel, detailsBackdropStyle, detailsBackdropScale, detailsBackdropArtwork, activeStyle) {
            when (detailsBackdropScale) {
                0 -> {
                    // MODE 0: Cinema Showcase Card (Widescreen Card + Ambient Aura)
                    // Layer 1: Ambient Glow Underlay behind everything (including status bar)
                    val underlayModel = effectiveArtworkModel ?: ambientArtworkModel
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(underlayModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(32.dp)
                            .alpha(0.40f)
                            .background(surfaceTintColor.copy(alpha = 0.15f)),
                    )

                    // Scrims for status bar and background dissolve
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Transparent,
                                ),
                                endY = (appBarPadding.toPx() + 20.dp.toPx()).coerceAtLeast(140f),
                            ),
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.50f),
                                    backgroundColor,
                                ),
                                startY = size.height * 0.45f,
                                endY = size.height,
                            ),
                        )
                    }

                    // Layer 2: Floating Cinema Card
                    if (effectiveArtworkModel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = appBarPadding + 6.dp,
                                )
                                .height(136.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.40f))
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                    RoundedCornerShape(16.dp),
                                ),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(effectiveArtworkModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                onError = {
                                    if (chosenArtworkModel != manga) artworkLoadFailed = true
                                },
                                onSuccess = { result ->
                                    val image = result.result.image
                                    if (image.width > 0 && image.height > 0) {
                                        bannerAspectRatio = image.width.toFloat() / image.height.toFloat()
                                    }
                                    if (effectiveArtworkModel == manga) {
                                        coverRatio.floatValue = image.height.toFloat() / image.width
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (backdropBlur > 0.dp) it.blur(backdropBlur) else it }
                                    .alpha(backdropAlpha),
                            )
                        }
                    }
                }

                1 -> {
                    // MODE 1: Feathered Cinema Vignette (Soft top and bottom fade below status bar with ambient blur)
                    val topVignetteStart = (appBarPadding * 0.55f).coerceAtLeast(16.dp)

                    // Layer 1: Ambient Glow Underlay behind everything (including status bar)
                    val underlayModel = effectiveArtworkModel ?: ambientArtworkModel
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(underlayModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(32.dp)
                            .alpha(0.40f)
                            .background(surfaceTintColor.copy(alpha = 0.15f)),
                    )

                    // Scrims for status bar and background dissolve
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Transparent,
                                ),
                                endY = (appBarPadding.toPx() + 20.dp.toPx()).coerceAtLeast(140f),
                            ),
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.50f),
                                    backgroundColor,
                                ),
                                startY = size.height * 0.45f,
                                endY = size.height,
                            ),
                        )
                    }

                    // Layer 2: Crisp Foreground Banner
                    if (effectiveArtworkModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(effectiveArtworkModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter,
                            onError = {
                                if (chosenArtworkModel != manga) artworkLoadFailed = true
                            },
                            onSuccess = { result ->
                                val image = result.result.image
                                if (image.width > 0 && image.height > 0) {
                                    bannerAspectRatio = image.width.toFloat() / image.height.toFloat()
                                }
                                if (effectiveArtworkModel == manga) {
                                    coverRatio.floatValue = image.height.toFloat() / image.width
                                }
                            },
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = topVignetteStart)
                                .let { if (backdropBlur > 0.dp) it.blur(backdropBlur) else it }
                                .alpha(backdropAlpha),
                        )
                    }

                    // Layer 3: Soft feathered blending gradients
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val topFadeEnd = (appBarPadding.toPx() + 28.dp.toPx()).coerceAtLeast(150f)
                        // Top feather dissolve so status bar and buttons sit on clean darkened background
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.60f),
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = topFadeEnd,
                            ),
                        )

                        val effectiveBannerHeight: Float = if (bannerAspectRatio > 0f) {
                            (size.width / bannerAspectRatio) + topVignetteStart.toPx()
                        } else {
                            size.height * 0.65f
                        }

                        val fadeStart: Float = (effectiveBannerHeight * 0.45f).coerceAtLeast(topFadeEnd)
                        val fadeEnd: Float = effectiveBannerHeight.coerceAtMost(size.height)

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.35f),
                                    backgroundColor.copy(alpha = 0.80f),
                                    backgroundColor,
                                ),
                                startY = fadeStart,
                                endY = fadeEnd,
                            ),
                        )
                    }
                }

                2 -> {
                    // MODE 2: Deep Scrim Bleed (Fill Screen with Deep Top Shadow)
                    if (effectiveArtworkModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(effectiveArtworkModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            onError = {
                                if (chosenArtworkModel != manga) artworkLoadFailed = true
                            },
                            onSuccess = { result ->
                                val image = result.result.image
                                if (image.width > 0 && image.height > 0) {
                                    bannerAspectRatio = image.width.toFloat() / image.height.toFloat()
                                }
                                if (effectiveArtworkModel == manga) {
                                    coverRatio.floatValue = image.height.toFloat() / image.width
                                }
                            },
                            modifier = Modifier
                                .matchParentSize()
                                .let { if (backdropBlur > 0.dp) it.blur(backdropBlur) else it }
                                .alpha(backdropAlpha),
                        )
                    }

                    Canvas(modifier = Modifier.matchParentSize()) {
                        // Deep top shadow for phone UI legibility
                        val topScrimEnd = (appBarPadding.toPx() + 40.dp.toPx()).coerceAtLeast(200f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                                endY = topScrimEnd,
                            ),
                        )

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.40f),
                                    backgroundColor.copy(alpha = 0.85f),
                                    backgroundColor,
                                ),
                                startY = size.height * 0.35f,
                                endY = size.height * 0.80f,
                            ),
                        )

                        if (size.height * 0.80f < size.height) {
                            drawRect(
                                color = backgroundColor,
                                topLeft = Offset(0f, size.height * 0.80f),
                                size = Size(size.width, size.height * 0.20f),
                            )
                        }
                    }
                }

                else -> {
                    // MODE 3: Natural Fit (Full width edge-to-edge)
                    if (effectiveArtworkModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(effectiveArtworkModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.TopCenter,
                            onError = {
                                if (chosenArtworkModel != manga) artworkLoadFailed = true
                            },
                            onSuccess = { result ->
                                val image = result.result.image
                                if (image.width > 0 && image.height > 0) {
                                    bannerAspectRatio = image.width.toFloat() / image.height.toFloat()
                                }
                                if (effectiveArtworkModel == manga) {
                                    coverRatio.floatValue = image.height.toFloat() / image.width
                                }
                            },
                            modifier = Modifier
                                .matchParentSize()
                                .let { if (backdropBlur > 0.dp) it.blur(backdropBlur) else it }
                                .alpha(backdropAlpha),
                        )
                    }

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val topScrimEnd = (appBarPadding.toPx() + 32.dp.toPx()).coerceAtLeast(180f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                                endY = topScrimEnd,
                            ),
                        )

                        val effectiveBannerHeight: Float = if (bannerAspectRatio > 0f) {
                            size.width / bannerAspectRatio
                        } else {
                            size.height * 0.70f
                        }

                        val fadeStart: Float = (effectiveBannerHeight * 0.35f).coerceAtLeast(40f)
                        val fadeEnd: Float = effectiveBannerHeight.coerceAtMost(size.height)

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.30f),
                                    backgroundColor.copy(alpha = 0.75f),
                                    backgroundColor,
                                ),
                                startY = fadeStart,
                                endY = fadeEnd,
                            ),
                        )

                        if (fadeEnd < size.height) {
                            drawRect(
                                color = backgroundColor,
                                topLeft = Offset(0f, fadeEnd),
                                size = Size(size.width, size.height - fadeEnd),
                            )
                        }
                    }
                }
            }

            if (!isWideBanner) {
                when (ambientCanvasStyle) {
                    1 -> {
                        // Style 1: Dynamic Palette Mesh (Vibrant Colorful Lights)
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.55f),
                                        primaryColor.copy(alpha = 0.15f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.85f, size.height * 0.20f),
                                    radius = size.width * 0.90f,
                                ),
                            )
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        tertiaryColor.copy(alpha = 0.40f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.15f, size.height * 0.70f),
                                    radius = size.width * 0.80f,
                                ),
                            )
                        }
                    }
                    2 -> {
                        // Style 2: Frosted Glass (Subtle Matte Sheen)
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.50f,
                                ),
                            )
                        }
                    }
                    else -> {
                        // Style 0 (Default): Cinematic Blur (Gentle ambient glow)
                        // Handled by the ambient glow underlay with cover artwork
                    }
                }
            }
        }

        val effectiveRatings = if (showCommunityRatings && showRatingsInDetails) {
            unifiedArtwork?.ratings ?: emptyList()
        } else {
            emptyList()
        }

        val effectiveCoverModel: Any = if (useAnilistArtwork && !unifiedArtwork?.posterUrl.isNullOrBlank()) {
            unifiedArtwork!!.posterUrl!!
        } else {
            manga
        }

        // Manga & source info
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            if (!isTabletUi) {
                MangaAndSourceTitlesSmall(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    coverModel = effectiveCoverModel,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    readingTime = readingTime,
                    // KMK -->
                    isSourceIncognito = isSourceIncognito,
                    // KMK <--
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    // KMK -->
                    librarySearch = librarySearch,
                    onSourceClick = onSourceClick,
                    onCoverLoaded = onCoverLoaded,
                    coverRatio = coverRatio,
                    usePanoramaCover = usePanoramaCover,
                    topAlignCover = topAlignCover,
                    // KMK <--
                    communityRatings = effectiveRatings,
                    communityRatingFormat = communityRatingFormat,
                )
            } else {
                MangaAndSourceTitlesLarge(
                    appBarPadding = appBarPadding,
                    manga = manga,
                    coverModel = effectiveCoverModel,
                    sourceName = sourceName,
                    isStubSource = isStubSource,
                    readingTime = readingTime,
                    // KMK -->
                    isSourceIncognito = isSourceIncognito,
                    // KMK <--
                    onCoverClick = onCoverClick,
                    doSearch = doSearch,
                    // KMK -->
                    librarySearch = librarySearch,
                    onSourceClick = onSourceClick,
                    onCoverLoaded = onCoverLoaded,
                    coverRatio = coverRatio,
                    usePanoramaCover = usePanoramaCover,
                    // KMK <--
                    communityRatings = effectiveRatings,
                    communityRatingFormat = communityRatingFormat,
                )
            }
        }
    }
}

@Composable
fun MangaActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    // SY -->
    onMergeClicked: (() -> Unit)?,
    // SY <--
    // KMK -->
    status: Long,
    interval: Int,
    // KMK <--
    modifier: Modifier = Modifier,
) {
    // KMK -->
    val libraryPreferences: LibraryPreferences = Injekt.get()
    val restrictions = libraryPreferences.autoUpdateMangaRestrictions().get()
    val notSkipCompleted = MANGA_NON_COMPLETED !in restrictions || status != SManga.COMPLETED.toLong()
    val selectedInterval by remember(interval) { mutableIntStateOf(if (interval < 0) -interval else 0) }
    // KMK <--
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)

    // TODO: show something better when using custom interval
    val nextUpdateDays = remember(nextUpdate, selectedInterval, notSkipCompleted) {
        return@remember if (nextUpdate != null &&
            // KMK -->
            notSkipCompleted
            // KMK <--
        ) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
                // KMK -->
                .takeIf { selectedInterval != FetchInterval.MANUAL_DISABLE }
                ?: FetchInterval.MANUAL_DISABLE
            // KMK <--
        } else {
            null
        }
    }

    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        MangaActionButton(
            title = if (favorite) {
                stringResource(MR.strings.in_library)
            } else {
                stringResource(MR.strings.add_to_library)
            },
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
            onLongClick = onEditCategory,
        )
        MangaActionButton(
            title = when (nextUpdateDays) {
                null -> stringResource(MR.strings.not_applicable)
                0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                // KMK -->
                FetchInterval.MANUAL_DISABLE -> stringResource(MR.strings.disabled)
                // KMK <--
                else -> pluralStringResource(
                    MR.plurals.day,
                    count = nextUpdateDays,
                    nextUpdateDays,
                )
            },
            icon = Icons.Default.HourglassEmpty
                // KMK -->
                .takeIf { nextUpdateDays != FetchInterval.MANUAL_DISABLE }
                ?: Icons.Default.HourglassDisabled,
            // KMK <--
            color = if (isUserIntervalMode ||
                // KMK -->
                nextUpdateDays?.let { it <= 1 } == true
                // KMK <--
            ) {
                MaterialTheme.colorScheme.primary
            } else {
                defaultActionButtonColor
            },
            onClick = { onEditIntervalClicked?.invoke() },
        )
        MangaActionButton(
            title = if (trackingCount == 0) {
                stringResource(MR.strings.manga_tracking_tab)
            } else {
                pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
            },
            icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
            color = if (trackingCount == 0) defaultActionButtonColor else MaterialTheme.colorScheme.primary,
            onClick = onTrackingClicked,
        )
        if (onWebViewClicked != null) {
            MangaActionButton(
                title = stringResource(MR.strings.action_web_view),
                icon = Icons.Outlined.Public,
                color = MaterialTheme.colorScheme.primary, // KMK: defaultActionButtonColor
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
        // SY -->
        if (onMergeClicked != null) {
            MangaActionButton(
                title = stringResource(SYMR.strings.merge),
                icon = Icons.AutoMirrored.Outlined.CallMerge,
                color = MaterialTheme.colorScheme.primary, // KMK: defaultActionButtonColor
                onClick = onMergeClicked,
            )
        }
        // SY <--
    }
}

@Composable
fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    // SY -->
    searchMetadataChips: SearchMetadataChips?,
    doSearch: (query: String, global: Boolean) -> Unit,
    // SY <--
    modifier: Modifier = Modifier,
) {
    // KMK -->
    val uiPreferences = Injekt.get<UiPreferences>()
    val pureDarkMode = uiPreferences.themeDarkAmoled().get()
    // KMK <--
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        MangaSummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                var showMenu by remember { mutableStateOf(false) }
                var tagSelected by remember { mutableStateOf("") }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_search)) },
                        onClick = {
                            onTagSearch(tagSelected)
                            showMenu = false
                        },
                    )
                    // SY -->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_global_search)) },
                        onClick = {
                            doSearch(tagSelected, true)
                            showMenu = false
                        },
                    )
                    // SY <--
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                        onClick = {
                            onCopyTagToClipboard(tagSelected)
                            showMenu = false
                        },
                    )
                }
                if (expanded) {
                    // SY -->
                    if (searchMetadataChips != null) {
                        NamespaceTags(
                            tags = searchMetadataChips,
                            onClick = {
                                tagSelected = it
                                showMenu = true
                            },
                            // KMK -->
                            pureDarkMode = pureDarkMode,
                            // KMK <--
                        )
                    } else {
                        // SY <--
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                        ) {
                            tags.forEach {
                                TagsChip(
                                    modifier = DefaultTagChipModifier,
                                    text = it,
                                    onClick = {
                                        tagSelected = it
                                        showMenu = true
                                    },
                                    // KMK -->
                                    pureDarkMode = pureDarkMode,
                                    // KMK <--
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        items(items = tags) {
                            TagsChip(
                                modifier = DefaultTagChipModifier,
                                text = it,
                                onClick = {
                                    tagSelected = it
                                    showMenu = true
                                },
                                // KMK -->
                                pureDarkMode = pureDarkMode,
                                // KMK <--
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaAndSourceTitlesLarge(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    readingTime: String?,
    // KMK -->
    isSourceIncognito: Boolean,
    // KMK <--
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    // KMK -->
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (DomainMangaCover) -> Unit,
    coverRatio: MutableFloatState,
    usePanoramaCover: Boolean = false,
    // KMK <--
    communityRatings: List<CommunityRating> = emptyList(),
    communityRatingFormat: Int = 0,
    coverModel: Any = manga,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // KMK -->
        if (usePanoramaCover && coverRatio.floatValue <= RatioSwitchToPanorama) {
            MangaCover.Panorama(
                modifier = Modifier.fillMaxWidth(0.65f),
                data = ImageRequest.Builder(LocalContext.current)
                    .data(coverModel)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                onClick = onCoverClick,
                // KMK -->
                onCoverLoaded = { mangaCover, result ->
                    val image = result.result.image
                    coverRatio.floatValue = image.height.toFloat() / image.width
                    onCoverLoaded(mangaCover)
                },
                // KMK <--
            )
        } else {
            // KMK <--
            MangaCover.Book(
                modifier = Modifier.fillMaxWidth(0.65f),
                data = ImageRequest.Builder(LocalContext.current)
                    .data(coverModel)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                onClick = onCoverClick,
                // KMK -->
                onCoverLoaded = { mangaCover, result ->
                    val image = result.result.image
                    coverRatio.floatValue = image.height.toFloat() / image.width
                    onCoverLoaded(mangaCover)
                },
                // KMK <--
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        MangaContentInfo(
            title = manga.title,
            author = manga.author,
            artist = manga.artist,
            status = manga.status,
            sourceName = sourceName,
            isStubSource = isStubSource,
            readingTime = readingTime,
            // KMK -->
            isSourceIncognito = isSourceIncognito,
            // KMK <--
            doSearch = doSearch,
            textAlign = TextAlign.Center,
            // KMK -->
            librarySearch = librarySearch,
            onSourceClick = onSourceClick,
            // KMK <--
            communityRatings = communityRatings,
            communityRatingFormat = communityRatingFormat,
        )
    }
}

@Composable
private fun MangaAndSourceTitlesSmall(
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    readingTime: String?,
    // KMK -->
    isSourceIncognito: Boolean,
    // KMK <--
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    // KMK -->
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (DomainMangaCover) -> Unit,
    coverRatio: MutableFloatState,
    usePanoramaCover: Boolean = false,
    topAlignCover: Boolean = false,
    // KMK <--
    communityRatings: List<CommunityRating> = emptyList(),
    communityRatingFormat: Int = 0,
    coverModel: Any = manga,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = if (topAlignCover) Alignment.Top else Alignment.CenterVertically,
    ) {
        // KMK -->
        if (usePanoramaCover && coverRatio.floatValue <= RatioSwitchToPanorama) {
            MangaCover.Panorama(
                modifier = Modifier
                    .sizeIn(maxHeight = 100.dp)
                    // KMK -->
                    .align(if (topAlignCover) Alignment.Top else Alignment.CenterVertically),
                // KMK <--
                data = ImageRequest.Builder(LocalContext.current)
                    .data(coverModel)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                onClick = onCoverClick,
                // KMK -->
                onCoverLoaded = { mangaCover, result ->
                    val image = result.result.image
                    coverRatio.floatValue = image.height.toFloat() / image.width
                    onCoverLoaded(mangaCover)
                },
                // KMK <--
            )
        } else {
            // KMK <--
            MangaCover.Book(
                modifier = Modifier
                    .sizeIn(maxWidth = 100.dp)
                    // KMK -->
                    .align(if (topAlignCover) Alignment.Top else Alignment.CenterVertically),
                // KMK <--
                data = ImageRequest.Builder(LocalContext.current)
                    .data(coverModel)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(MR.strings.manga_cover),
                onClick = onCoverClick,
                // KMK -->
                onCoverLoaded = { mangaCover, result ->
                    val image = result.result.image
                    coverRatio.floatValue = image.height.toFloat() / image.width
                    onCoverLoaded(mangaCover)
                },
                // KMK <--
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MangaContentInfo(
                title = manga.title,
                author = manga.author,
                artist = manga.artist,
                status = manga.status,
                sourceName = sourceName,
                isStubSource = isStubSource,
                readingTime = readingTime,
                // KMK -->
                isSourceIncognito = isSourceIncognito,
                // KMK <--
                doSearch = doSearch,
                // KMK -->
                librarySearch = librarySearch,
                onSourceClick = onSourceClick,
                // KMK <--
                communityRatings = communityRatings,
                communityRatingFormat = communityRatingFormat,
            )
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.MangaContentInfo(
    title: String,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
    readingTime: String?,
    // KMK -->
    isSourceIncognito: Boolean,
    // KMK <--
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
    // KMK -->
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    // KMK <--
    communityRatings: List<CommunityRating> = emptyList(),
    communityRatingFormat: Int = 0,
) {
    val context = LocalContext.current
    // KMK -->
    var showMenu by remember { mutableStateOf(false) }
    var tagSelected by remember { mutableStateOf("") }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(KMR.strings.action_library_search)) },
            onClick = {
                librarySearch(tagSelected)
                showMenu = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(MR.strings.action_global_search)) },
            onClick = {
                doSearch(tagSelected, true)
                showMenu = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
            onClick = {
                context.copyToClipboard(
                    tagSelected,
                    tagSelected,
                )
                showMenu = false
            },
        )
    }
    // KMK <--
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clickableNoIndication(
            onLongClick = {
                if (title.isNotBlank()) {
                    // KMK -->
                    tagSelected = title
                    showMenu = true
                    // KMK <--
                }
            },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) {
                            // KMK -->
                            tagSelected = author
                            showMenu = true
                            // KMK <--
                        }
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = textAlign,
        )
    }

    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Brush,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .clickableNoIndication(
                        onLongClick = {
                            // KMK -->
                            tagSelected = artist
                            showMenu = true
                            // KMK <--
                        },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    if (communityRatings.isNotEmpty()) {
        Spacer(modifier = Modifier.height(2.dp))
        CommunityRatingBadge(
            ratings = communityRatings,
            format = communityRatingFormat,
        )
        Spacer(modifier = Modifier.height(2.dp))
    }

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                SManga.ONGOING.toLong() -> Icons.Outlined.Schedule
                SManga.COMPLETED.toLong() -> Icons.Outlined.DoneAll
                SManga.LICENSED.toLong() -> Icons.Outlined.AttachMoney
                SManga.PUBLISHING_FINISHED.toLong() -> Icons.Outlined.Done
                SManga.CANCELLED.toLong() -> Icons.Outlined.Close
                SManga.ON_HIATUS.toLong() -> Icons.Outlined.Pause
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = when (status) {
                    SManga.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
                    SManga.COMPLETED.toLong() -> stringResource(MR.strings.completed)
                    SManga.LICENSED.toLong() -> stringResource(MR.strings.licensed)
                    SManga.PUBLISHING_FINISHED.toLong() -> stringResource(MR.strings.publishing_finished)
                    SManga.CANCELLED.toLong() -> stringResource(MR.strings.cancelled)
                    SManga.ON_HIATUS.toLong() -> stringResource(MR.strings.on_hiatus)
                    else -> stringResource(MR.strings.unknown)
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            // KMK -->
            if (isSourceIncognito) {
                DotSeparatorText()
                Icon(
                    painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_glasses_with_hat_24dp)),
                    contentDescription = stringResource(MR.strings.pref_incognito_mode),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // KMK <--
            DotSeparatorText()
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication(
                    // KMK -->
                    onLongClick = {
                        tagSelected = sourceName
                        showMenu = true
                    },
                    onClick = {
                        onSourceClick()
                    },
                    // KMK <--
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }

    // KMK -->
    if (readingTime != null) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ChromeReaderMode,
                contentDescription = readingTime,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = readingTime,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = textAlign,
            )
        }
    }
    // KMK <--
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

                val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
                        ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
                        ?.getUnescapedTextInNode(content)
                    ?: return@markdownAnnotator false

                val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
                    ?.getUnescapedTextInNode(content).orEmpty()

                withLink(LinkAnnotation.Url(url = url)) {
                    pushStyle(linkStyle)
                    appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
                    append(altText)
                    pop()
                }

                return@markdownAnnotator true
            }

            if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                append(content.substring(child.startOffset, child.endOffset))
                return@markdownAnnotator true
            }

            false
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun MangaSummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription().get() }
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                // shrunk: calculate minimum size when shrunk
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
                    text = if (notes.isBlank()) "\n\n" else "\n\n\n\n\n",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            {
                // actual: the actual displayed content
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        infoHeight = size.height
                    },
                ) {
                    MangaNotesSection(
                        content = notes,
                        expanded = expanded,
                        onEditNotes = onEditNotesClicked,
                    )
                    SelectionContainer {
                        MarkdownRender(
                            content = description,
                            modifier = Modifier.secondaryItemAlpha(),
                            annotator = descriptionAnnotator(
                                loadImages = loadImages,
                                linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                            ),
                            loadImages = loadImages,
                        )
                    }
                }
            },
            {
                // scrim
                val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                Box(
                    modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        // KMK -->
                        tint = MaterialTheme.colorScheme.primary, // KMK: onBackground
                        // KMK <--
                        modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
                    )
                }
            },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.place(0, scrimY)
        }
    }
}

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
internal fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        onLongClick = onLongClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// EaZy Nuvio+ / Chimanew: Nuvio-Style Community Rating Badges with Authentic Logos
@Composable
fun CommunityRatingBadge(
    ratings: List<CommunityRating>,
    format: Int,
    modifier: Modifier = Modifier,
) {
    if (ratings.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (format == 4) {
            ratings.forEach { rating ->
                NuvioRatingBadgeItem(rating = rating, format = format)
            }
        } else {
            val rating = ratings.first()
            NuvioRatingBadgeItem(rating = rating, format = format)
        }
    }
}

@Composable
private fun NuvioRatingBadgeItem(
    rating: CommunityRating,
    format: Int,
) {
    val src = rating.source.uppercase()
    val isPercentage = (format == 1 || format == 3)
    val scoreText = if (isPercentage) {
        "${rating.score100}%"
    } else {
        String.format(java.util.Locale.US, "%.1f", rating.score10)
    }

    val (badgeBg, badgeBorder, badgeText, badgeTextColor, scoreColor) = when {
        src.contains("ANILIST") -> {
            BrandStyle(
                bg = Color(0xFF0B1622),
                border = Color(0xFF02A9FF).copy(alpha = 0.50f),
                text = "AL",
                textColor = Color(0xFF02A9FF),
                scoreColor = Color(0xFF38BDF8),
            )
        }
        src.contains("MAL") || src.contains("MYANIMELIST") -> {
            BrandStyle(
                bg = Color(0xFF2E51A2),
                border = Color(0xFF4E72C4).copy(alpha = 0.50f),
                text = "MAL",
                textColor = Color.White,
                scoreColor = Color(0xFF93C5FD),
            )
        }
        src.contains("KITSU") -> {
            BrandStyle(
                bg = Color(0xFFFD755C),
                border = Color(0xFFFF8B73).copy(alpha = 0.50f),
                text = "kitsu",
                textColor = Color.White,
                scoreColor = Color(0xFFFCA5A5),
            )
        }
        src.contains("MANGADEX") -> {
            BrandStyle(
                bg = Color(0xFFFF6740),
                border = Color(0xFFFF8B73).copy(alpha = 0.50f),
                text = "MD",
                textColor = Color.White,
                scoreColor = Color(0xFFFDBA74),
            )
        }
        else -> {
            BrandStyle(
                bg = Color(0xFF374151),
                border = Color(0xFF6B7280).copy(alpha = 0.50f),
                text = rating.source.take(3).uppercase(),
                textColor = Color.White,
                scoreColor = Color(0xFFF3F4F6),
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeBg,
            border = BorderStroke(1.dp, badgeBorder),
            modifier = Modifier.height(18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                    ),
                )
            }
        }
        Text(
            text = scoreText,
            color = scoreColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private data class BrandStyle(
    val bg: Color,
    val border: Color,
    val text: String,
    val textColor: Color,
    val scoreColor: Color,
)
