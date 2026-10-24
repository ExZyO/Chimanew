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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.home.chimanew.HomeMangaItem

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
    val cardShape = RoundedCornerShape(12.dp)
    val accentColor = item.coverData.vibrantCoverColor?.let { Color(it) }
        ?: item.coverData.dominantCoverColors?.first?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

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
            .width(132.dp)
            .height(194.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
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
        // 1. Cover image: Use high-res AniList cover when available, falling back to local coverData
        // EaZy Nuvio+ / Chimanew: resilient cover fallback
        val imageModel: Any = item.highResCoverUrl ?: item.coverData
        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = {
                if (item.highResCoverUrl != null) {
                    SubcomposeAsyncImage(
                        model = item.coverData,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
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

            // Chapter / In-Progress Info
            val chapterText = when {
                item.lastChapterNumber != null && !item.isChapterRead && item.lastPageRead > 0 -> {
                    "Ch. ${formatChapterNumber(item.lastChapterNumber)}: p. ${item.lastPageRead + 1}"
                }
                item.lastChapterNumber != null -> {
                    "Ch. ${formatChapterNumber(item.lastChapterNumber)}"
                }
                else -> {
                    "${item.totalChapters} ch"
                }
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
