// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.home.chimanew.HomeMangaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun NuvioHeroSection(
    items: List<HomeMangaItem>,
    modifier: Modifier = Modifier,
    onItemClick: (HomeMangaItem) -> Unit,
    onResumeClick: (HomeMangaItem) -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-advance carousel every 6 seconds unless scrolled
    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(6000L)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            val accentColor = item.coverData.vibrantCoverColor?.let { Color(it) }
                ?: item.coverData.dominantCoverColors?.first?.let { Color(it) }
                ?: MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.background

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onItemClick(item) },
                    ),
            ) {
                // 1. Cover Artwork
                SubcomposeAsyncImage(
                    model = item.coverData,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // 2. Ambient Accent Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.40f),
                                    Color.Transparent,
                                ),
                                radius = 900f,
                            ),
                        ),
                )

                // 3. Top scrim for status bar readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
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
                                0.40f to Color.Transparent,
                                0.72f to surfaceColor.copy(alpha = 0.75f),
                                1.0f to surfaceColor,
                            ),
                        ),
                )

                // 5. Hero Content (Anchored at Bottom)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 22.dp),
                ) {
                    // Genres chips
                    if (item.genres.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp),
                        ) {
                            item.genres.take(3).forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.50f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.90f),
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Manga Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress info: Ch. X / Total • +N new
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val progressText = buildString {
                            if (item.lastChapterNumber != null) {
                                append("Ch. ${formatChapterNumber(item.lastChapterNumber)}")
                            }
                            if (item.totalChapters > 0) {
                                append(" / ${item.totalChapters}")
                            }
                        }
                        if (progressText.isNotBlank()) {
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f),
                                ),
                            )
                        }

                        if (item.unreadCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                            ) {
                                Text(
                                    text = "+${item.unreadCount} new",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1-Tap Resume Button
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
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            ),
                        )
                    }
                }
            }
        }

        // Carousel Indicator Dots
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}
