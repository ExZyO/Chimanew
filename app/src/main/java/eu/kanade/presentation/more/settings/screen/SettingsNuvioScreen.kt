// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioAppearanceScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioArtworkScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioDiagnosticsScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioHeroScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioRatingsScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioShelvesScreen
import eu.kanade.presentation.more.settings.screen.chimanew.SettingsNuvioUpdatesScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR

object SettingsNuvioScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_nuvio

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            Preference.PreferenceGroup(
                title = "Configuration Hub",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = "Appearance & Posters",
                        subtitle = "Poster dimensions, corner radius, card badges, ambient canvas, and details layout",
                        onClick = { navigator.push(SettingsNuvioAppearanceScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Home & Hero Banner",
                        subtitle = "Hero banner carousel for Manga, Anime, and Novels, source filtering, and styles",
                        onClick = { navigator.push(SettingsNuvioHeroScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Shelves & Rows",
                        subtitle = "Home shelf ordering, visibility toggles, and tap behaviors across all media feeds",
                        onClick = { navigator.push(SettingsNuvioShelvesScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Artwork Sources & Priority",
                        subtitle = "Separate Manga, Anime, and Novel waterfall orders, category rules, and cache reset",
                        onClick = { navigator.push(SettingsNuvioArtworkScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Library & Updates Engine",
                        subtitle = "Tracker-assisted updates, app launch checks, series consolidation, and restrictions",
                        onClick = { navigator.push(SettingsNuvioUpdatesScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Community Ratings",
                        subtitle = "Aggregated ratings from AniList, MyAnimeList, and Kitsu, formats, and placement",
                        onClick = { navigator.push(SettingsNuvioRatingsScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = "Diagnostics & Logs",
                        subtitle = "Real-time logging, subsystem diagnostics, export logs, and log viewer",
                        onClick = { navigator.push(SettingsNuvioDiagnosticsScreen) },
                    ),
                ),
            ),
        )
    }
}
