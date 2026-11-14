// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.presentation.util.formatChapterNumber
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.entries.anime.model.AnimeCover
import eu.kanade.tachiyomi.ui.home.chimanew.HomeMangaItem
import eu.kanade.tachiyomi.ui.home.chimanew.style.NuvioCardDepthSurface
import eu.kanade.tachiyomi.ui.home.chimanew.style.nuvioCardDepth
import eu.kanade.tachiyomi.ui.home.chimanew.style.rememberPosterCardStyleUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NuvioMangaCard(
    item: HomeMangaItem,
    modifier: Modifier = Modifier,
    showProgressBar: Boolean = true,
    showUnreadBadge: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val posterStyle = rememberPosterCardStyleUiState()
    val cardShape = RoundedCornerShape(posterStyle.cornerRadiusDp.dp)
    val accentColor = when (val c = item.coverData) {
        is MangaCover -> c.vibrantCoverColor?.let { Color(it) } ?: c.dominantCoverColors?.first?.let { Color(it) }
        is AnimeCover -> c.vibrantCoverColor?.let { Color(it) } ?: c.dominantCoverColors?.first?.let { Color(it) }
        else -> null
    } ?: MaterialTheme.colorScheme.primary

    // EaZy Nuvio+ / Chimanew: Interactive snappy spring micro-animation on press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "card_press_scale",
    )

    Box(
        modifier = modifier
            .width(posterStyle.widthDp.dp)
            .height(posterStyle.heightDp.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .nuvioCardDepth(shape = cardShape, surface = NuvioCardDepthSurface.Posters)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        // 1. Cover image: Prioritize custom cover if set, then high-res AniList cover, falling back to local coverData
        // EaZy Nuvio+ / Chimanew: resilient, high-performance direct AsyncImage with zero subcomposition lag
        val coverCache = remember { Injekt.get<CoverCache>() }
        val defaultCover = item.coverData
        val customCoverExists = item.coverData is MangaCover && coverCache.hasCustomCover(item.mangaId)
        var loadFailed by remember(item.mangaId, item.highResCoverUrl) { mutableStateOf(false) }

        val imageModel: Any = if (customCoverExists || loadFailed || item.highResCoverUrl == null) {
            defaultCover
        } else {
            item.highResCoverUrl
        }

        AsyncImage(
            model = imageModel,
            placeholder = ColorPainter(CoverPlaceholderColor),
            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = {
                if (!loadFailed && item.highResCoverUrl != null && !customCoverExists) {
                    loadFailed = true
                }
            },
        )

        // 2. Unread badge at top-right
        if (showUnreadBadge && item.unreadCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.92f),
            ) {
                Text(
                    text = "+${item.unreadCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // 3. Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )

        // 4. Card Content
        if (!posterStyle.hideLabelsEnabled) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                // Manga Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Chapter / Episode / Discovery Info
                val prefix = if (item.mediaType == 1) "Ep." else "Ch."
                val chapterText = when {
                    item.isDiscovery -> {
                        item.communityRatings.firstOrNull()?.let { "★ ${String.format(java.util.Locale.ROOT, "%.1f", it.score10)}" }
                            ?: item.genres.firstOrNull()
                            ?: "Trending"
                    }
                    item.lastChapterNumber != null && !item.isChapterRead && item.lastPageRead > 0 -> {
                        "$prefix ${formatChapterNumber(item.lastChapterNumber)}: p. ${item.lastPageRead + 1}"
                    }
                    item.lastChapterNumber != null -> {
                        "$prefix ${formatChapterNumber(item.lastChapterNumber)}"
                    }
                    item.totalChapters > 0 -> {
                        val unit = if (item.mediaType == 1) "ep" else "ch"
                        "${item.totalChapters} $unit"
                    }
                    else -> item.genres.firstOrNull() ?: ""
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = chapterText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                        ),
                    )
                    if (item.totalChapters > 0 && item.lastChapterNumber != null) {
                        Text(
                            text = " / ${item.totalChapters}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.50f),
                            ),
                        )
                    }
                }
            }
        }

        // 5. Netflix-style bottom edge reading progress bar
        if (showProgressBar) {
            val progress = when {
                item.isChapterRead -> 1f
                item.totalChapters > 0 && item.readChapters > 0 -> {
                    val inProgressBonus = if (!item.isChapterRead && item.lastPageRead > 0) 0.5f else 0f
                    ((item.readChapters + inProgressBonus) / item.totalChapters.toFloat()).coerceIn(0.05f, 1f)
                }
                !item.isChapterRead && item.lastPageRead > 0 -> {
                    // Active reading in progress on current chapter
                    if (item.chapterTotalPages > 0) {
                        ((item.lastPageRead + 1).toFloat() / item.chapterTotalPages.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        // Noticeable active reading indicator when page count is not yet loaded
                        0.18f
                    }
                }
                item.readChapters > 0 && item.totalChapters > 0 -> {
                    (item.readChapters.toFloat() / item.totalChapters.toFloat()).coerceIn(0.05f, 1f)
                }
                else -> 0f
            }

            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = accentColor,
                    trackColor = Color.Black.copy(alpha = 0.40f),
                )
            }
        }
    }
}

private val CoverPlaceholderColor = Color(0x1F888888)
