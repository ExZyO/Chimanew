// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.style

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

const val DefaultPosterCardWidthDp = 132
const val DefaultPosterCardHeightDp = 194
const val DefaultPosterCardCornerRadiusDp = 12

@Serializable
private data class StoredPosterCardStylePreferences(
    val widthDp: Int = DefaultPosterCardWidthDp,
    val heightDp: Int = DefaultPosterCardHeightDp,
    val cornerRadiusDp: Int = DefaultPosterCardCornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = false,
    val hideLabelsEnabled: Boolean = false,
    val alwaysShowLandscapeClearlogo: Boolean = false,
)

data class PosterCardStyleUiState(
    val widthDp: Int = DefaultPosterCardWidthDp,
    val heightDp: Int = DefaultPosterCardHeightDp,
    val cornerRadiusDp: Int = DefaultPosterCardCornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = false,
    val hideLabelsEnabled: Boolean = false,
    val alwaysShowLandscapeClearlogo: Boolean = false,
)

object PosterCardStyleRepository {
    private const val PREFS_NAME = "nuvio_poster_card_style"
    private const val PAYLOAD_KEY = "poster_card_style_payload"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var preferences: SharedPreferences? = null

    private val _uiState = MutableStateFlow(PosterCardStyleUiState())
    val uiState: StateFlow<PosterCardStyleUiState> = _uiState.asStateFlow()

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

    fun setWidthDp(widthDp: Int) {
        ensureLoaded()
        val nextWidth = widthDp.coerceIn(90, 200)
        val nextHeight = (nextWidth * 3) / 2
        if (_uiState.value.widthDp == nextWidth && _uiState.value.heightDp == nextHeight) return
        _uiState.value = _uiState.value.copy(
            widthDp = nextWidth,
            heightDp = nextHeight,
        )
        persist()
    }

    fun setCornerRadiusDp(cornerRadiusDp: Int) {
        ensureLoaded()
        val clamped = cornerRadiusDp.coerceIn(0, 28)
        if (_uiState.value.cornerRadiusDp == clamped) return
        _uiState.value = _uiState.value.copy(cornerRadiusDp = clamped)
        persist()
    }

    fun setCatalogLandscapeModeEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.catalogLandscapeModeEnabled == enabled) return
        _uiState.value = _uiState.value.copy(catalogLandscapeModeEnabled = enabled)
        persist()
    }

    fun setHideLabelsEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.hideLabelsEnabled == enabled) return
        _uiState.value = _uiState.value.copy(hideLabelsEnabled = enabled)
        persist()
    }

    fun resetToDefaults() {
        ensureLoaded()
        _uiState.value = PosterCardStyleUiState()
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = getPrefs()?.getString(PAYLOAD_KEY, null)?.trim().orEmpty()
        if (payload.isEmpty()) {
            _uiState.value = PosterCardStyleUiState()
            return
        }

        val stored = runCatching {
            json.decodeFromString<StoredPosterCardStylePreferences>(payload)
        }.getOrNull()

        _uiState.value = if (stored != null) {
            val widthDp = stored.widthDp.takeIf { it in 90..200 } ?: DefaultPosterCardWidthDp
            val heightDp = stored.heightDp.takeIf { it > 0 } ?: ((widthDp * 3) / 2)
            val cornerRadiusDp = stored.cornerRadiusDp.coerceIn(0, 28)
            PosterCardStyleUiState(
                widthDp = widthDp,
                heightDp = heightDp,
                cornerRadiusDp = cornerRadiusDp,
                catalogLandscapeModeEnabled = stored.catalogLandscapeModeEnabled,
                hideLabelsEnabled = stored.hideLabelsEnabled,
                alwaysShowLandscapeClearlogo = stored.alwaysShowLandscapeClearlogo,
            )
        } else {
            PosterCardStyleUiState()
        }
    }

    private fun persist() {
        val payload = json.encodeToString(
            StoredPosterCardStylePreferences(
                widthDp = _uiState.value.widthDp,
                heightDp = _uiState.value.heightDp,
                cornerRadiusDp = _uiState.value.cornerRadiusDp,
                catalogLandscapeModeEnabled = _uiState.value.catalogLandscapeModeEnabled,
                hideLabelsEnabled = _uiState.value.hideLabelsEnabled,
                alwaysShowLandscapeClearlogo = _uiState.value.alwaysShowLandscapeClearlogo,
            ),
        )
        getPrefs()?.edit()?.putString(PAYLOAD_KEY, payload)?.apply()
    }
}

@Composable
fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    PosterCardStyleRepository.ensureLoaded()
    val uiState by PosterCardStyleRepository.uiState.collectAsState()
    return uiState
}
