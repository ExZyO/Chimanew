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
                        subtitle = "Display colored reading progress bars below cards",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showUnreadBadges(),
                        title = stringResource(MR.strings.pref_nuvio_show_unread_badge),
                        subtitle = "Display unread/unseen chapter counts on poster corners",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Ambient Effects & Navigation",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.ambientCanvasStyle(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_artwork_ambient_cinematic_blur),
                            1 to stringResource(MR.strings.pref_chimanew_artwork_ambient_palette_mesh),
                            2 to stringResource(MR.strings.pref_chimanew_artwork_ambient_hybrid_texture),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_artwork_ambient_style),
                        subtitle = stringResource(MR.strings.pref_chimanew_artwork_ambient_style_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.navbarAutoHideOnScroll(),
                        title = stringResource(MR.strings.pref_chimanew_navbar_autohide),
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
                            0 to stringResource(MR.strings.pref_chimanew_details_page_layout_nuvio),
                            1 to stringResource(MR.strings.pref_chimanew_details_page_layout_stack),
                            2 to stringResource(MR.strings.pref_chimanew_details_page_layout_vignette),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_page_layout),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_page_layout_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.detailsBackdropScale(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_card),
                            1 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_vignette),
                            2 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_crop),
                            3 to stringResource(MR.strings.pref_chimanew_details_backdrop_scale_natural),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_backdrop_scale),
                        subtitle = stringResource(MR.strings.pref_chimanew_details_backdrop_scale_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.detailsBackdropStyle(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_details_backdrop_style_fade),
                            1 to stringResource(MR.strings.pref_chimanew_details_backdrop_style_blur),
                            2 to stringResource(MR.strings.pref_chimanew_details_backdrop_style_crisp),
                            3 to stringResource(MR.strings.pref_chimanew_details_backdrop_style_local),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_details_backdrop_style),
                    ),
                ),
            ),
        )
    }
}
