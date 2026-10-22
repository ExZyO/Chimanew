// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.chimanew.HomeRowsCustomizerScreen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_nuvio

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }

        return listOf(
            getHeroGroup(chimanewPreferences),
            getShelvesGroup(chimanewPreferences, navigator),
            getCardsGroup(chimanewPreferences),
        )
    }

    @Composable
    private fun getHeroGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_nuvio_hero_section),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showHeroCarousel(),
                    title = stringResource(MR.strings.pref_nuvio_show_hero),
                    subtitle = stringResource(MR.strings.pref_nuvio_show_hero_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = preferences.heroSource(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.pref_nuvio_hero_source_history),
                        1 to stringResource(MR.strings.pref_nuvio_hero_source_favorites),
                    ),
                    title = stringResource(MR.strings.pref_nuvio_hero_source),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = preferences.heroStyle(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.pref_nuvio_hero_style_ambient),
                        1 to stringResource(MR.strings.pref_nuvio_hero_style_fullbleed),
                    ),
                    title = stringResource(MR.strings.pref_nuvio_hero_style),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.useAnilistArtwork(),
                    title = stringResource(MR.strings.pref_nuvio_use_anilist_artwork),
                    subtitle = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getShelvesGroup(preferences: ChimanewPreferences, navigator: Navigator): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_nuvio_shelves_section),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_nuvio_reorder_shelves),
                    subtitle = stringResource(MR.strings.pref_nuvio_reorder_shelves_summary),
                    onClick = {
                        navigator.push(HomeRowsCustomizerScreen())
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showContinueReading(),
                    title = stringResource(MR.strings.pref_nuvio_show_continue_reading),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showRecentlyUpdated(),
                    title = stringResource(MR.strings.pref_nuvio_show_recently_updated),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showCategoryShelves(),
                    title = stringResource(MR.strings.pref_nuvio_show_category_shelves),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showTrackingShelves(),
                    title = stringResource(MR.strings.pref_nuvio_show_tracking_shelves),
                    subtitle = stringResource(MR.strings.pref_nuvio_show_tracking_shelves_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getCardsGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_nuvio_cards_section),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showProgressBars(),
                    title = stringResource(MR.strings.pref_nuvio_show_progress),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.showUnreadBadges(),
                    title = stringResource(MR.strings.pref_nuvio_show_unread_badge),
                ),
            ),
        )
    }
}
