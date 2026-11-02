// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.style

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

const val DefaultCardDepthEdgeStrength = 28
const val DefaultCardDepthSheenStrength = 10
const val DefaultCardDepthEdgeCoverage = 0

enum class NuvioCardDepthSurface {
    Posters,
    ContinueWatching,
    EpisodeCards,
    DetailCards,
}

@Serializable
private data class StoredCardDepthStylePreferences(
    val enabled: Boolean = false,
    val edgeStrength: Int = DefaultCardDepthEdgeStrength,
    val sheenStrength: Int = DefaultCardDepthSheenStrength,
    val edgeCoverage: Int = DefaultCardDepthEdgeCoverage,
    val postersEnabled: Boolean = true,
    val continueWatchingEnabled: Boolean = true,
    val episodeCardsEnabled: Boolean = true,
    val detailCardsEnabled: Boolean = true,
)

data class CardDepthStyleUiState(
    val enabled: Boolean = false,
    val edgeStrength: Int = DefaultCardDepthEdgeStrength,
    val sheenStrength: Int = DefaultCardDepthSheenStrength,
    val edgeCoverage: Int = DefaultCardDepthEdgeCoverage,
    val postersEnabled: Boolean = true,
    val continueWatchingEnabled: Boolean = true,
    val episodeCardsEnabled: Boolean = true,
    val detailCardsEnabled: Boolean = true,
) {
    fun isEnabledFor(surface: NuvioCardDepthSurface): Boolean =
        enabled && when (surface) {
            NuvioCardDepthSurface.Posters -> postersEnabled
            NuvioCardDepthSurface.ContinueWatching -> continueWatchingEnabled
            NuvioCardDepthSurface.EpisodeCards -> episodeCardsEnabled
            NuvioCardDepthSurface.DetailCards -> detailCardsEnabled
        }
}

object CardDepthStyleRepository {
    private const val PREFS_NAME = "nuvio_card_depth_style"
    private const val PAYLOAD_KEY = "card_depth_style_payload"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var preferences: SharedPreferences? = null

    private val _uiState = MutableStateFlow(CardDepthStyleUiState())
    val uiState: StateFlow<CardDepthStyleUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    private fun getPrefs(): SharedPreferences? {
        if (preferences == null) {
            runCatching {
                val app = Injekt.get<Application>()
                preferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
        return preferences
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun setEnabled(enabled: Boolean) {
        update { it.copy(enabled = enabled) }
    }

    fun setEdgeStrength(strength: Int) {
        update { it.copy(edgeStrength = strength.coerceIn(0, 100)) }
    }

    fun setSheenStrength(strength: Int) {
        update { it.copy(sheenStrength = strength.coerceIn(0, 100)) }
    }

    fun setEdgeCoverage(coverage: Int) {
        update { it.copy(edgeCoverage = coverage.coerceIn(0, 100)) }
    }

    fun setSurfaceEnabled(surface: NuvioCardDepthSurface, enabled: Boolean) {
        update {
            when (surface) {
                NuvioCardDepthSurface.Posters -> it.copy(postersEnabled = enabled)
                NuvioCardDepthSurface.ContinueWatching -> it.copy(continueWatchingEnabled = enabled)
                NuvioCardDepthSurface.EpisodeCards -> it.copy(episodeCardsEnabled = enabled)
                NuvioCardDepthSurface.DetailCards -> it.copy(detailCardsEnabled = enabled)
            }
        }
    }

    fun resetToDefaults() {
        ensureLoaded()
        _uiState.value = CardDepthStyleUiState()
        persist()
    }

    private fun update(transform: (CardDepthStyleUiState) -> CardDepthStyleUiState) {
        ensureLoaded()
        val next = transform(_uiState.value)
        if (_uiState.value == next) return
        _uiState.value = next
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = getPrefs()?.getString(PAYLOAD_KEY, null)?.trim().orEmpty()
        if (payload.isEmpty()) {
            _uiState.value = CardDepthStyleUiState()
            return
        }

        val stored = runCatching {
            json.decodeFromString<StoredCardDepthStylePreferences>(payload)
        }.getOrNull()

        _uiState.value = if (stored != null) {
            CardDepthStyleUiState(
                enabled = stored.enabled,
                edgeStrength = stored.edgeStrength.coerceIn(0, 100),
                sheenStrength = stored.sheenStrength.coerceIn(0, 100),
                edgeCoverage = stored.edgeCoverage.coerceIn(0, 100),
                postersEnabled = stored.postersEnabled,
                continueWatchingEnabled = stored.continueWatchingEnabled,
                episodeCardsEnabled = stored.episodeCardsEnabled,
                detailCardsEnabled = stored.detailCardsEnabled,
            )
        } else {
            CardDepthStyleUiState()
        }
    }

    private fun persist() {
        val payload = json.encodeToString(
            StoredCardDepthStylePreferences(
                enabled = _uiState.value.enabled,
                edgeStrength = _uiState.value.edgeStrength,
                sheenStrength = _uiState.value.sheenStrength,
                edgeCoverage = _uiState.value.edgeCoverage,
                postersEnabled = _uiState.value.postersEnabled,
                continueWatchingEnabled = _uiState.value.continueWatchingEnabled,
                episodeCardsEnabled = _uiState.value.episodeCardsEnabled,
                detailCardsEnabled = _uiState.value.detailCardsEnabled,
            ),
        )
        getPrefs()?.edit()?.putString(PAYLOAD_KEY, payload)?.apply()
    }
}

@Composable
fun rememberCardDepthStyleUiState(): CardDepthStyleUiState {
    CardDepthStyleRepository.ensureLoaded()
    val uiState by CardDepthStyleRepository.uiState.collectAsState()
    return uiState
}

@Composable
fun Modifier.nuvioCardDepth(
    shape: Shape,
    surface: NuvioCardDepthSurface,
    fallbackBorderAlpha: Float = 0f,
): Modifier {
    val state = rememberCardDepthStyleUiState()
    if (!state.isEnabledFor(surface)) {
        return if (fallbackBorderAlpha > 0f) {
            border(
                width = 1.dp,
                color = Color.White.copy(alpha = fallbackBorderAlpha),
                shape = shape,
            )
        } else {
            this
        }
    }

    return cardDepthVisual(
        shape = shape,
        edgeStrength = state.edgeStrength.toFloat(),
        sheenStrength = state.sheenStrength.toFloat(),
        edgeCoverage = state.edgeCoverage.toFloat(),
    )
}

fun Modifier.cardDepthVisual(
    shape: Shape,
    edgeStrength: Float,
    sheenStrength: Float,
    edgeCoverage: Float = DefaultCardDepthEdgeCoverage.toFloat(),
): Modifier {
    val edgeTop = edgeStrength.coerceIn(0f, 100f) / 100f
    val sheen = sheenStrength.coerceIn(0f, 100f) / 100f
    val coverage = edgeCoverage.coerceIn(0f, 100f) / 100f

    val withEdge = if (edgeTop > 0f) {
        border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = edgeTop),
                    Color.White.copy(alpha = edgeTop * (0.33f + 0.67f * coverage)),
                    Color.White.copy(alpha = edgeTop * coverage),
                ),
            ),
            shape = shape,
        )
    } else {
        this
    }

    return if (sheen > 0f) {
        withEdge.drawWithContent {
            drawContent()
            val sheenHeight = size.height * 0.22f
            if (sheenHeight > 0f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                val shapePath = outline.toPath()
                clipPath(shapePath) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = sheen),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = sheenHeight,
                        ),
                        size = Size(size.width, sheenHeight),
                    )
                }
            }
        }
    } else {
        withEdge
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
