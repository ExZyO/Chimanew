// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.appearance.PosterCustomizationSettingsScreen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import eu.kanade.tachiyomi.ui.home.chimanew.style.PosterCardStyleRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioAppearanceScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioAppearanceScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember { Injekt.get<ChimanewPreferences>() }
        val posterState by PosterCardStyleRepository.uiState.collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = "Poster & Card Styling",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = "Poster Customization & Sizing",
                        subtitle = "Width: ${posterState.widthDp}dp, Corners: ${posterState.cornerRadiusDp}dp, Shadows & Elevation",
                        onClick = { navigator.push(PosterCustomizationSettingsScreen()) },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showProgressBars(),
                        title = stringResource(MR.strings.pref_nuvio_show_progress),
                        subtitle = stringResource(MR.strings.pref_nuvio_show_progress_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showUnreadBadges(),
                        title = stringResource(MR.strings.pref_nuvio_show_unread_badges),
                        subtitle = stringResource(MR.strings.pref_nuvio_show_unread_badges_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Ambient Effects & Navigation",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.ambientCanvasStyle(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_cinematic),
                            1 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_palette),
                            2 to stringResource(MR.strings.pref_chimanew_ambient_canvas_style_hybrid),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_ambient_canvas_style_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_ambient_canvas_style_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.navbarAutoHideOnScroll(),
                        title = stringResource(MR.strings.pref_chimanew_navbar_autohide_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_navbar_autohide_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Details Backdrop & Header",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.detailsPageLayout(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_details_layout_hero),
                            1 to stringResource(MR.strings.pref_chimanew_details_layout_canvas),
                            2 to stringResource(MR.strings.pref_chimanew_details_layout_vignette),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_layout_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_layout_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.detailsBackdropScale(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_ambient),
                            1 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_natural),
                            2 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_fill),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_backdrop_scale_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_backdrop_scale_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.detailsBackdropStyle(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_details_backdrop_cinematic),
                            1 to stringResource(MR.strings.pref_chimanew_details_backdrop_mesh),
                            2 to stringResource(MR.strings.pref_chimanew_details_backdrop_flat),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_backdrop_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_backdrop_summary),
                    ),
                ),
            ),
        )
    }
}
