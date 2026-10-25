// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import eu.kanade.presentation.manga.components.CommunityRatingBadge
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.HomeMangaItem
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.ChimanewDiagnostics
import eu.kanade.tachiyomi.ui.home.chimanew.diagnostics.DiagnosticCategory
import eu.kanade.tachiyomi.ui.home.chimanew.data.CommunityRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object NuvioPaletteColorCache {
    private val cache = ConcurrentHashMap<String, Color>()
    operator fun get(key: String): Color? = cache[key]
    operator fun set(key: String, color: Color) { cache[key] = color }
}

@Composable
fun NuvioHeroSection(
    items: List<HomeMangaItem>,
    modifier: Modifier = Modifier,
    heroStyle: Int = 0,
    heroArtworkSources: Set<String> = setOf("banner", "cover", "default"),
    onItemClick: (HomeMangaItem) -> Unit,
    onResumeClick: (HomeMangaItem) -> Unit,
    onQuickStatusClick: (HomeMangaItem) -> Unit = {},
) {
    if (items.isEmpty()) return

    val preferences = remember { Injekt.get<ChimanewPreferences>() }
    val heroRequireSelectedArtwork by preferences.heroRequireSelectedArtwork().collectPrefAsState()
    val heroStrictArtworkRequirements by preferences.heroStrictArtworkRequirements().collectPrefAsState()
    val heroArtworkPairing by preferences.heroArtworkPairing().collectPrefAsState()

    fun hasAuthenticBanner(item: HomeMangaItem): Boolean {
        if ("banner" !in heroArtworkSources) return false
        val banner = item.highResBannerUrl
        if (banner.isNullOrBlank()) return false
        val cover = item.highResCoverUrl ?: item.coverData.ogUrl
        if (!cover.isNullOrBlank() && banner.equals(cover, ignoreCase = true)) return false
        if (banner.contains("bookwalker", ignoreCase = true)) return false
        val defaultCover = item.coverData.ogUrl
        if (!defaultCover.isNullOrBlank() && banner.equals(defaultCover, ignoreCase = true)) return false
        return true
    }

    fun hasAuthenticCover(item: HomeMangaItem): Boolean {
        val hasHighRes = !item.highResCoverUrl.isNullOrBlank() && "cover" in heroArtworkSources
        val hasDefault = !item.coverData.ogUrl.isNullOrBlank() && "default" in heroArtworkSources
        return hasHighRes || hasDefault
    }

    val displayItems = remember(
        items, heroStyle, heroRequireSelectedArtwork, heroStrictArtworkRequirements,
        heroArtworkSources, heroArtworkPairing,
    ) {
        if (!heroRequireSelectedArtwork) {
            items
        } else {
            val enforceBanner = "banner" in heroStrictArtworkRequirements
            val enforceCover = "cover" in heroStrictArtworkRequirements

            items.filter { item ->
                val hasBanner = hasAuthenticBanner(item)
                val hasCover = hasAuthenticCover(item)
                val bannerPass = if (enforceBanner) hasBanner else true
                val coverPass = if (enforceCover && heroStyle != 1) hasCover else true

                val accepted = if (!enforceBanner && !enforceCover) {
                    hasBanner || hasCover
                } else {
                    bannerPass && coverPass
                }

                ChimanewDiagnostics.log(DiagnosticCategory.HERO, "StrictFilter") {
                    val status = if (accepted) "[ACCEPTED]" else "[REJECTED]"
                    val reason = when {
                        accepted -> "All requirements satisfied (hasBanner=$hasBanner, hasCover=$hasCover)"
                        !bannerPass && !coverPass -> "Missing BOTH authentic banner AND valid cover"
                        !bannerPass -> "Missing authentic wide banner (banner=${item.highResBannerUrl})"
                        !coverPass -> "Missing valid cover from active sources"
                        else -> "Filtered by strict layout constraints"
                    }
                    "$status '${item.title}' (id=${item.mangaId}) -> $reason"
                }

                accepted
            }
        }
    }
    if (displayItems.isEmpty()) return

    // EaZy Nuvio+ / Chimanew: Virtual infinite loop in both directions
    val isLooping = displayItems.size > 1
    val virtualCount = if (isLooping) 100_000 else displayItems.size
    val startIndex = if (isLooping) {
        val middle = 50_000
        middle - (middle % displayItems.size)
    } else 0

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { virtualCount },
    )

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    }

    // Dynamic Ambient Palette Extraction (Option A: Smooth Vertical Gradient)
    val currentSettledIndex = if (isLooping) pagerState.currentPage % displayItems.size else pagerState.currentPage.coerceIn(0, displayItems.lastIndex)
    val currentHeroItem = displayItems.getOrNull(currentSettledIndex)
    val context = LocalContext.current

    val activeBannerUrl = currentHeroItem?.let { item ->
        val hasBanner = hasAuthenticBanner(item)
        if (hasBanner && "banner" in heroArtworkSources) item.highResBannerUrl
        else item.highResCoverUrl ?: item.coverData.ogUrl
    }

    var dynamicExtractedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(activeBannerUrl) {
        if (activeBannerUrl.isNullOrBlank()) {
            dynamicExtractedColor = null
            return@LaunchedEffect
        }
        val cached = NuvioPaletteColorCache[activeBannerUrl]
        if (cached != null) {
            dynamicExtractedColor = cached
            return@LaunchedEffect
        }
        withContext(Dispatchers.Default) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(activeBannerUrl)
                    .allowHardware(false)
                    .size(160)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.image?.toBitmap()
                if (result != null) {
                    val palette = Palette.from(result).generate()
                    val targetRgb = palette.getVibrantColor(
                        palette.getDominantColor(palette.getDarkVibrantColor(0))
                    )
                    if (targetRgb != 0) {
                        val color = Color(targetRgb)
                        NuvioPaletteColorCache[activeBannerUrl] = color
                        dynamicExtractedColor = color
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val ambientTargetColor = dynamicExtractedColor
        ?: currentHeroItem?.coverData?.vibrantCoverColor?.let { Color(it) }
        ?: currentHeroItem?.coverData?.dominantCoverColors?.first?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

    val animatedAmbientColor by animateColorAsState(
        targetValue = ambientTargetColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "HeroAmbientColorAnimation",
    )

    // Auto-advance carousel smoothly forward every 6 seconds unless user is scrolling
    LaunchedEffect(displayItems.size, isLooping) {
        if (!isLooping) return@LaunchedEffect
        while (isActive) {
            delay(6000L)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val bannerHeight = if (heroStyle == 0 || heroStyle == 2) 380.dp else 400.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .background(
                Brush.verticalGradient(
                    0.0f to animatedAmbientColor.copy(alpha = 0.45f),
                    0.65f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    1.0f to MaterialTheme.colorScheme.background,
                ),
            ),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val actualIndex = if (isLooping) page % displayItems.size else page
            val item = displayItems[actualIndex]
            val accentColor = item.coverData.vibrantCoverColor?.let { Color(it) }
                ?: item.coverData.dominantCoverColors?.first?.let { Color(it) }
                ?: MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.background

            val heroArtworkPairing by preferences.heroArtworkPairing().collectPrefAsState()
            val heroShowUnreadBadge by preferences.heroShowUnreadBadges().collectPrefAsState()
            val showCommunityRatings by preferences.showCommunityRatings().collectPrefAsState()
            val communityRatingFormat by preferences.communityRatingFormat().collectPrefAsState()
            val showRatingsOnHero by preferences.showRatingsOnHero().collectPrefAsState()
            val heroDimBanner by preferences.heroDimBanner().collectPrefAsState()
            val heroAllowCroppedFallback by preferences.heroAllowCroppedFallback().collectPrefAsState()
            val ambientCanvasStyle by preferences.ambientCanvasStyle().collectPrefAsState()

            val posterImageModel: Any = when {
                "cover" in heroArtworkSources && !item.highResCoverUrl.isNullOrBlank() -> item.highResCoverUrl
                "default" in heroArtworkSources -> item.coverData
                else -> item.highResCoverUrl ?: item.coverData
            }

            val hasBanner = hasAuthenticBanner(item)
            val enforceBanner = heroRequireSelectedArtwork && "banner" in heroStrictArtworkRequirements
            val bannerImageModel: Any? = when {
                hasBanner && "banner" in heroArtworkSources -> item.highResBannerUrl
                enforceBanner -> null // Strict banner profile: never fall back to blurring cover if official banner is missing!
                heroArtworkPairing == 1 -> posterImageModel // Unified: same cover for both
                else -> posterImageModel // Non-strict fallback: always display cover backdrop so hero is never blank
            }
            ChimanewDiagnostics.log(DiagnosticCategory.HERO, "BannerModel") {
                val bannerType = when {
                    bannerImageModel == null -> "NULL (Strict rejection / no fallback)"
                    bannerImageModel == item.highResBannerUrl -> "Authentic HD Banner"
                    bannerImageModel == posterImageModel -> "Cover Cropped/Blurred Fallback"
                    else -> "Model: $bannerImageModel"
                }
                "'${item.title}' banner set to $bannerType"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onItemClick(item) },
                    ),
            ) {
                when (heroStyle) {
                    0 -> {
                        // -------------------------------------------------------------
                        // Style 0: Netflix / Nuvio Ambient Backdrop (Sharp 2:3 Poster)
                        // -------------------------------------------------------------

                        // 1. Atmospheric Heavily Blurred Cover Backdrop or Fallback Background
                        if (bannerImageModel != null) {
                            val heroBannerAlpha = if (heroDimBanner) 0.35f else 0.85f
                            AsyncImage(
                                model = bannerImageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(22.dp)
                                    .alpha(heroBannerAlpha),
                            )
                        } else {
                            HeroFallbackBackgroundCanvas(
                                posterModel = posterImageModel,
                                ambientStyle = ambientCanvasStyle,
                                accentColor = accentColor,
                                dimBanner = heroDimBanner,
                                allowCoverBlur = heroAllowCroppedFallback && !heroRequireSelectedArtwork,
                            )
                        }

                        // 2. Radial Accent Ambient Glow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.35f),
                                            Color.Transparent,
                                        ),
                                        radius = 800f,
                                    ),
                                ),
                        )

                        // 3. Top Scrim for Status Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.60f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        // 4. Bottom Vignette Fade into Theme Surface
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.45f to Color.Transparent,
                                        0.82f to surfaceColor.copy(alpha = 0.85f),
                                        1.0f to surfaceColor,
                                    ),
                                ),
                        )

                        // 5. Foreground Content: Sharp Portrait Cover Card + Metadata
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(),
                        ) {
                            HeroAmbientForegroundContent(
                                item = item,
                                posterImageModel = posterImageModel,
                                accentColor = accentColor,
                                heroShowUnreadBadge = heroShowUnreadBadge,
                                showRatingsOnHero = showRatingsOnHero && showCommunityRatings,
                                communityRatingFormat = communityRatingFormat,
                                onResumeClick = onResumeClick,
                                onQuickStatusClick = onQuickStatusClick,
                            )
                        }
                    }

                    2 -> {
                        // -------------------------------------------------------------------------
                        // Style 2: Option 3 - Netflix Ambient with Crisp Widescreen Banner Backdrop
                        // -------------------------------------------------------------------------

                        // 1. Crisp Unblurred Widescreen Banner Backdrop or Fallback Background
                        if (bannerImageModel != null) {
                            val heroBannerAlpha = if (heroDimBanner) 0.55f else 1.0f
                            AsyncImage(
                                model = bannerImageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(heroBannerAlpha),
                            )
                        } else {
                            HeroFallbackBackgroundCanvas(
                                posterModel = posterImageModel,
                                ambientStyle = ambientCanvasStyle,
                                accentColor = accentColor,
                                dimBanner = heroDimBanner,
                                allowCoverBlur = heroAllowCroppedFallback && !heroRequireSelectedArtwork,
                            )
                        }

                        // 2. Radial Accent Ambient Glow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.25f),
                                            Color.Transparent,
                                        ),
                                        radius = 800f,
                                    ),
                                ),
                        )

                        // 3. Top Scrim for Status Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.70f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        // 4. Bottom Vignette Fade into Theme Surface
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.35f to Color.Transparent,
                                        0.72f to surfaceColor.copy(alpha = 0.88f),
                                        1.0f to surfaceColor,
                                    ),
                                ),
                        )

                        // 5. Foreground Content: Sharp Portrait Cover Card + Metadata
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(),
                        ) {
                            HeroAmbientForegroundContent(
                                item = item,
                                posterImageModel = posterImageModel,
                                accentColor = accentColor,
                                heroShowUnreadBadge = heroShowUnreadBadge,
                                showRatingsOnHero = showRatingsOnHero && showCommunityRatings,
                                communityRatingFormat = communityRatingFormat,
                                onResumeClick = onResumeClick,
                                onQuickStatusClick = onQuickStatusClick,
                            )
                        }
                    }

                    else -> {
                        // -------------------------------------------------------------
                        // Style 1: Option 1 1:1 Match (Official Wide Banner / High-Res)
                        // -------------------------------------------------------------

                        // 1. Cover / Wide Artwork or Fallback Background
                        if (bannerImageModel != null) {
                            AsyncImage(
                                model = bannerImageModel,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            HeroFallbackBackgroundCanvas(
                                posterModel = posterImageModel,
                                ambientStyle = ambientCanvasStyle,
                                accentColor = accentColor,
                                dimBanner = heroDimBanner,
                                allowCoverBlur = heroAllowCroppedFallback && !heroRequireSelectedArtwork,
                            )
                        }

                    // 2. Ambient Radial Accent Glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.35f),
                                        Color.Transparent,
                                    ),
                                    radius = 900f,
                                ),
                            ),
                    )

                    // 3. Top Scrim for Status Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )

                    // 4. Bottom Vignette Fade into Theme Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.35f to Color.Transparent,
                                    0.70f to surfaceColor.copy(alpha = 0.85f),
                                    1.0f to surfaceColor,
                                ),
                            ),
                    )

                    // 5. Hero Content (Anchored at Bottom) - 1:1 Match with Option 1
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Format / Genre Pill (e.g. MANHWA • ACTION) + Community Rating
                        val genreText = if (item.genres.isNotEmpty()) {
                            item.genres.take(2).joinToString(" • ") { it.uppercase() }
                        } else {
                            "FEATURED"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
                            ) {
                                Text(
                                    text = genreText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = accentColor,
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }

                            if (showRatingsOnHero && showCommunityRatings && item.communityRatings.isNotEmpty()) {
                                CommunityRatingBadge(
                                    ratings = item.communityRatings,
                                    format = communityRatingFormat,
                                )
                            }
                        }

                        // Manga Title with Drop Shadow
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.90f),
                                    blurRadius = 8f,
                                    offset = Offset(0f, 2f),
                                ),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // 2-Line Synopsis Description
                        if (!item.description.isNullOrBlank()) {
                            val cleanDesc = remember(item.description) {
                                item.description.replace(Regex("<[^>]*>"), "").trim()
                            }
                            if (cleanDesc.isNotBlank()) {
                                Text(
                                    text = cleanDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.85f),
                                            blurRadius = 6f,
                                            offset = Offset(0f, 1f),
                                        ),
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Action Row: Resume Button + Ch. X / Y Progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val buttonText = if (item.lastChapterNumber != null) {
                                "Resume Ch. ${formatChapterNumber(item.lastChapterNumber)}"
                            } else {
                                "Start Reading"
                            }

                            Button(
                                onClick = { onResumeClick(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Black,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onQuickStatusClick(item) },
                                modifier = Modifier.size(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.20f),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Quick Status",
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            val progressText = buildString {
                                if (item.lastChapterNumber != null) {
                                    append("Ch. ${formatChapterNumber(item.lastChapterNumber)}")
                                    if (item.totalChapters > 0) {
                                        append(" / ${item.totalChapters}")
                                    }
                                } else if (item.totalChapters > 0) {
                                    append("${item.totalChapters} Chapters")
                                }
                            }
                            if (progressText.isNotBlank()) {
                                Text(
                                    text = progressText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.80f),
                                            blurRadius = 4f,
                                        ),
                                    ),
                                )
                            }

                            val showUnread = heroShowUnreadBadge && item.unreadCount > 0 &&
                                (item.lastChapterNumber != null || item.unreadCount != item.totalChapters)
                            if (showUnread) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                                ) {
                                    Text(
                                        text = "+${item.unreadCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = accentColor,
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // EaZy Nuvio+ / Chimanew: Netflix-style morphing active pill indicator
        if (items.size > 1) {
            val activeIndex = pagerState.currentPage % items.size
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(items.size) { index ->
                    val isSelected = activeIndex == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 5.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                        label = "hero_dot_width",
                    )
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(width)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroAmbientForegroundContent(
    item: HomeMangaItem,
    posterImageModel: Any,
    accentColor: Color,
    heroShowUnreadBadge: Boolean,
    showRatingsOnHero: Boolean = true,
    communityRatingFormat: Int = 0,
    onResumeClick: (HomeMangaItem) -> Unit,
    onQuickStatusClick: (HomeMangaItem) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Sharp 2:3 Uncropped Portrait Poster
        Surface(
            modifier = Modifier
                .width(116.dp)
                .height(172.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            color = Color.Black.copy(alpha = 0.40f),
        ) {
            AsyncImage(
                model = posterImageModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Right details Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Genres chips
            if (item.genres.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    item.genres.take(2).forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.90f),
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        blurRadius = 6f,
                    ),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Progress, Unread & Community Ratings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showRatingsOnHero && item.communityRatings.isNotEmpty()) {
                    CommunityRatingBadge(
                        ratings = item.communityRatings,
                        format = communityRatingFormat,
                    )
                }

                val progressText = buildString {
                    if (item.lastChapterNumber != null) {
                        append("Ch. ${formatChapterNumber(item.lastChapterNumber)}")
                        if (item.totalChapters > 0) {
                            append(" / ${item.totalChapters}")
                        }
                    } else if (item.totalChapters > 0) {
                        append("${item.totalChapters} Chapters")
                    }
                }
                if (progressText.isNotBlank()) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.80f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                }

                val showUnread = heroShowUnreadBadge && item.unreadCount > 0 &&
                    (item.lastChapterNumber != null || item.unreadCount != item.totalChapters)
                if (showUnread) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            text = "+${item.unreadCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = accentColor,
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Row: Resume Button + Quick Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onResumeClick(item) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledTonalIconButton(
                    onClick = { onQuickStatusClick(item) },
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.20f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Quick Status",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun HeroFallbackBackgroundCanvas(
    posterModel: Any,
    ambientStyle: Int,
    accentColor: Color,
    dimBanner: Boolean,
    allowCoverBlur: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (ambientStyle) {
            1 -> {
                // Style 1: Dynamic Palette Mesh (Vibrant Colorful Lights)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFF101014))

                    // Top-right dynamic aura
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (dimBanner) 0.50f else 0.75f),
                                accentColor.copy(alpha = if (dimBanner) 0.18f else 0.35f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.20f),
                            radius = size.width * 0.90f,
                        ),
                    )

                    // Bottom-left complementary aura
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (dimBanner) 0.35f else 0.55f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.80f),
                            radius = size.width * 0.80f,
                        ),
                    )

                    // Center luminous glow
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (dimBanner) 0.08f else 0.18f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.50f, size.height * 0.45f),
                            radius = size.width * 0.55f,
                        ),
                    )
                }
            }
            2 -> {
                // Style 2: Frosted Glass (Subtle Matte Texture)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (allowCoverBlur) {
                        AsyncImage(
                            model = posterModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(48.dp)
                                .alpha(if (dimBanner) 0.25f else 0.45f),
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color(0xFF141416).copy(alpha = if (allowCoverBlur) 0.55f else 1.0f))
                        // Top specular sheen
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = size.height * 0.45f,
                            ),
                        )
                        // Ambient color reflection
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (dimBanner) 0.30f else 0.50f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.75f, size.height * 0.30f),
                                radius = size.width * 0.75f,
                            ),
                        )
                    }
                }
            }
            else -> {
                // Style 0 (Default): Clean Subtle Aura (or Gentle ambient glow)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (allowCoverBlur) {
                        AsyncImage(
                            model = posterModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(36.dp)
                                .alpha(if (dimBanner) 0.45f else 0.80f),
                        )
                    }
                    // Color ambient wash
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = if (dimBanner) 0.40f else 0.60f),
                                        accentColor.copy(alpha = 0.15f),
                                        if (allowCoverBlur) Color.Transparent else Color(0xFF121215),
                                    ),
                                    center = Offset(250f, 200f),
                                    radius = 800f,
                                ),
                            ),
                    )
                }
            }
        }
    }
}
