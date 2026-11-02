// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_ANIME
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_MANGA
import eu.kanade.tachiyomi.ui.home.chimanew.MEDIA_TYPE_NOVEL
import eu.kanade.tachiyomi.ui.home.chimanew.data.ArtworkWaterfallRepository
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioArtworkScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioArtworkScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_chimanew_artwork_category

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val preferences = remember { Injekt.get<ChimanewPreferences>() }
        var showResetDialog by rememberSaveable { mutableStateOf(false) }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(text = stringResource(MR.strings.pref_chimanew_reset_artwork_cache)) },
                text = {
                    Text(text = "Are you sure you want to reset the artwork cache? This will clear all downloaded covers and cached remote artwork across Manga, Anime, and Novels. New artwork will be fetched automatically when needed.")
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetDialog = false
                            val repo = Injekt.get<ArtworkWaterfallRepository>()
                            repo.clearCache()
                            context.toast(MR.strings.pref_chimanew_artwork_cache_cleared)
                        },
                    ) {
                        Text(text = "Reset")
                    }
                },
            )
        }

        return listOf(
            Preference.PreferenceGroup(
                title = "Artwork Priority & Waterfall",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = "Manga Artwork Priority & Waterfall",
                        subtitle = "Configure source waterfall order and toggle sources (MangaDex, AniList, Kitsu, Bangumi, BookWalker, Naver, Kuaikan)",
                        onClick = {
                            navigator.push(ArtworkPriorityCustomizerScreen(initialMediaType = MEDIA_TYPE_MANGA))
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Anime Artwork Priority & Waterfall",
                        subtitle = "Configure anime-specific source waterfall order and toggle sources (AniList Anime, Kitsu Anime, Bangumi Anime)",
                        onClick = {
                            navigator.push(ArtworkPriorityCustomizerScreen(initialMediaType = MEDIA_TYPE_ANIME))
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Novel Artwork Priority & Waterfall",
                        subtitle = "Configure novel-specific source waterfall order and toggle sources (BookWalker, Bangumi, AniList, Naver)",
                        onClick = {
                            navigator.push(ArtworkPriorityCustomizerScreen(initialMediaType = MEDIA_TYPE_NOVEL))
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Category Rules",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_chimanew_category_artwork_rules_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_category_artwork_rules_summary),
                        onClick = {
                            navigator.push(CategoryArtworkRulesScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Display Settings",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.useAnilistArtworkInLibrary(),
                        title = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_in_library),
                        subtitle = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_in_library_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.useAnilistArtworkInDetails(),
                        title = stringResource(MR.strings.pref_chimanew_details_use_anilist),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_use_anilist_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.ambientCanvasStyle(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_cinematic),
                            1 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_palette),
                            2 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_hybrid),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_artwork_ambient_style),
                        subtitle = stringResource(MR.strings.pref_chimanew_artwork_ambient_style_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Artwork Cache",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_chimanew_reset_artwork_cache),
                        subtitle = stringResource(MR.strings.pref_chimanew_reset_artwork_cache_summary),
                        onClick = {
                            showResetDialog = true
                        },
                    ),
                ),
            ),
        )
    }
}
