// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.chimanew.style.CardDepthStyleRepository
import eu.kanade.tachiyomi.ui.home.chimanew.style.NuvioCardDepthSurface
import eu.kanade.tachiyomi.ui.home.chimanew.style.PosterCardStyleRepository
import eu.kanade.tachiyomi.ui.home.chimanew.style.cardDepthVisual
import kotlin.math.roundToInt

class PosterCustomizationSettingsScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val posterState by PosterCardStyleRepository.uiState.collectAsState()
        val depthState by CardDepthStyleRepository.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Poster & Card Styling") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                PosterCardStyleRepository.resetToDefaults()
                                CardDepthStyleRepository.resetToDefaults()
                            },
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = "Reset to Defaults")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Live Card Preview
                Text(
                    text = "Live Preview",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(8.dp))

                val previewShape = RoundedCornerShape(posterState.cornerRadiusDp.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(posterState.widthDp.dp)
                            .height(posterState.heightDp.dp)
                            .clip(previewShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2C3E50),
                                        Color(0xFF000000),
                                    ),
                                ),
                            )
                            .then(
                                if (depthState.enabled) {
                                    Modifier.cardDepthVisual(
                                        shape = previewShape,
                                        edgeStrength = depthState.edgeStrength.toFloat(),
                                        sheenStrength = depthState.sheenStrength.toFloat(),
                                        edgeCoverage = depthState.edgeCoverage.toFloat(),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (!posterState.hideLabelsEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Sample Title",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${posterState.widthDp} dp × ${posterState.heightDp} dp • ${posterState.cornerRadiusDp} dp radius",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Dimensions Controls
                Text(
                    text = "Poster Dimensions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Card Width")
                    Text("${posterState.widthDp} dp", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = posterState.widthDp.toFloat(),
                    onValueChange = { PosterCardStyleRepository.setWidthDp(it.roundToInt()) },
                    valueRange = 100f..180f,
                    steps = 15,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Corner Radius")
                    Text("${posterState.cornerRadiusDp} dp", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = posterState.cornerRadiusDp.toFloat(),
                    onValueChange = { PosterCardStyleRepository.setCornerRadiusDp(it.roundToInt()) },
                    valueRange = 0f..24f,
                    steps = 12,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hide Labels on Cards")
                    Switch(
                        checked = posterState.hideLabelsEnabled,
                        onCheckedChange = { PosterCardStyleRepository.setHideLabelsEnabled(it) },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Card Depth Effect Controls
                Text(
                    text = "Card Depth & 3D Sheen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable Card Depth Effect")
                    Switch(
                        checked = depthState.enabled,
                        onCheckedChange = { CardDepthStyleRepository.setEnabled(it) },
                    )
                }

                if (depthState.enabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Edge Highlight Strength")
                        Text("${depthState.edgeStrength}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = depthState.edgeStrength.toFloat(),
                        onValueChange = { CardDepthStyleRepository.setEdgeStrength(it.roundToInt()) },
                        valueRange = 0f..100f,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Top Sheen Glow")
                        Text("${depthState.sheenStrength}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = depthState.sheenStrength.toFloat(),
                        onValueChange = { CardDepthStyleRepository.setSheenStrength(it.roundToInt()) },
                        valueRange = 0f..100f,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Edge Coverage")
                        Text("${depthState.edgeCoverage}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = depthState.edgeCoverage.toFloat(),
                        onValueChange = { CardDepthStyleRepository.setEdgeCoverage(it.roundToInt()) },
                        valueRange = 0f..100f,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Surfaces",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = depthState.postersEnabled,
                            onCheckedChange = { CardDepthStyleRepository.setSurfaceEnabled(NuvioCardDepthSurface.Posters, it) },
                        )
                        Text("Home & Grid Posters")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = depthState.continueWatchingEnabled,
                            onCheckedChange = { CardDepthStyleRepository.setSurfaceEnabled(NuvioCardDepthSurface.ContinueWatching, it) },
                        )
                        Text("Continue Reading / Watching Cards")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        PosterCardStyleRepository.resetToDefaults()
                        CardDepthStyleRepository.resetToDefaults()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
    }
}
