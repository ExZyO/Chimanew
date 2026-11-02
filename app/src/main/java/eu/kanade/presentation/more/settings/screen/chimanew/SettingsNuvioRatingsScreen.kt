// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioRatingsScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioRatingsScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_chimanew_ratings_category

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<ChimanewPreferences>() }
        val showRatings by preferences.showCommunityRatings().collectPrefAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = "Community Ratings",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showCommunityRatings(),
                        title = stringResource(MR.strings.pref_chimanew_show_community_ratings),
                        subtitle = stringResource(MR.strings.pref_chimanew_show_community_ratings_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.communityRatingFormat(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_chimanew_community_rating_format_star),
                            1 to stringResource(MR.strings.pref_chimanew_community_rating_format_percentage),
                            2 to stringResource(MR.strings.pref_chimanew_community_rating_format_star_source),
                            3 to stringResource(MR.strings.pref_chimanew_community_rating_format_percentage_source),
                            4 to stringResource(MR.strings.pref_chimanew_community_rating_format_multi),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_community_rating_format),
                        enabled = showRatings,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Placement & Visibility",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showRatingsOnHero(),
                        title = stringResource(MR.strings.pref_chimanew_show_ratings_on_hero),
                        subtitle = stringResource(MR.strings.pref_chimanew_show_ratings_on_hero_summary),
                        enabled = showRatings,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showRatingsInDetails(),
                        title = stringResource(MR.strings.pref_chimanew_show_ratings_in_details),
                        subtitle = stringResource(MR.strings.pref_chimanew_show_ratings_in_details_summary),
                        enabled = showRatings,
                    ),
                ),
            ),
        )
    }
}
