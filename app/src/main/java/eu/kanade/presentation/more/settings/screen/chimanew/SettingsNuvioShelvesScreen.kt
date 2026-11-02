// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioShelvesScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioShelvesScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_nuvio_shelves_section

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember { Injekt.get<ChimanewPreferences>() }

        return listOf(
            Preference.PreferenceGroup(
                title = "Home Rows & Shelves Customizer",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_nuvio_reorder_shelves),
                        subtitle = stringResource(MR.strings.pref_nuvio_reorder_shelves_summary),
                        onClick = {
                            navigator.push(HomeRowsCustomizerScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Standard Shelves Visibility",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showContinueReading(),
                        title = stringResource(MR.strings.pref_nuvio_show_continue_reading),
                        subtitle = "Display quick resume cards for recently read or watched entries",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showRecentlyUpdated(),
                        title = stringResource(MR.strings.pref_nuvio_show_recently_updated),
                        subtitle = "Display shelf of entries with newly released chapters or episodes",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showCategoryShelves(),
                        title = stringResource(MR.strings.pref_nuvio_show_category_shelves),
                        subtitle = "Display individual shelves for your custom library categories",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showTrackingShelves(),
                        title = stringResource(MR.strings.pref_nuvio_show_tracking_shelves),
                        subtitle = stringResource(MR.strings.pref_nuvio_show_tracking_shelves_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Shelf Tap Interactions",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.continueReadingTapAction(),
                        entries = persistentMapOf(
                            1 to stringResource(MR.strings.pref_chimanew_tap_action_reader),
                            0 to stringResource(MR.strings.pref_chimanew_tap_action_details),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_continue_reading_tap_action),
                        subtitle = stringResource(MR.strings.pref_chimanew_continue_reading_tap_action_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.shelfTapAction(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_tap_action_details),
                            1 to stringResource(MR.strings.pref_chimanew_tap_action_reader),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_shelf_tap_action),
                        subtitle = stringResource(MR.strings.pref_chimanew_shelf_tap_action_summary),
                    ),
                ),
            ),
        )
    }
}
