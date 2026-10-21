// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = modifier
            .width(132.dp)
            .height(194.dp)
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        // 1. Cover image
        SubcomposeAsyncImage(
            model = item.coverData,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Unread badge at top-right
        if (showUnreadBadge && item.unreadCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.90f),
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
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

            // Chapter Info
            val chapterText = if (item.lastChapterNumber != null) {
                "Ch. ${formatChapterNumber(item.lastChapterNumber)}"
            } else {
                "${item.totalChapters} ch"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
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

            // 2px Progress bar at bottom
            if (showProgressBar) {
                val progress = if (item.totalChapters > 0) {
                    (item.readChapters.toFloat() / item.totalChapters.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.20f),
                    )
                }
            }
        }
    }
}
