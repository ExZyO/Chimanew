// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
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
        val chimanewPreferences = remember { Injekt.get<ChimanewPreferences>() }

        return listOf(
            getHeroGroup(chimanewPreferences),
            getShelvesGroup(chimanewPreferences),
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
            ),
        )
    }

    @Composable
    private fun getShelvesGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_nuvio_shelves_section),
            preferenceItems = persistentListOf(
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
