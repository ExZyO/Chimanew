// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.chimanew.ArtworkPriorityCustomizerScreen
import eu.kanade.presentation.more.settings.screen.chimanew.HomeRowsCustomizerScreen
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
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
            getArtworkGroup(chimanewPreferences, navigator),
            getRatingsGroup(chimanewPreferences),
            getDetailsPageGroup(chimanewPreferences),
            getNavbarGroup(chimanewPreferences),
        )
    }

    @Composable
    private fun getHeroGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val categories by getCategories.subscribe().collectAsState(initial = emptyList())
        val categoryEntries = remember(categories) {
            categories.associate { it.id.toString() to it.name.ifBlank { "Default" } }.toPersistentMap()
        }
        val heroSource by preferences.heroSource().collectPrefAsState()
        val heroStyle by preferences.heroStyle().collectPrefAsState()
        val useAnilistArtwork by preferences.useAnilistArtwork().collectPrefAsState()

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
                        2 to stringResource(MR.strings.pref_nuvio_hero_source_trackers),
                    ),
                    title = stringResource(MR.strings.pref_nuvio_hero_source),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = preferences.heroCategories(),
                    entries = categoryEntries,
                    title = stringResource(MR.strings.pref_chimanew_hero_categories),
                    subtitle = stringResource(MR.strings.pref_chimanew_hero_categories_summary),
                    enabled = heroSource == 1,
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = preferences.heroTrackerStatuses(),
                    entries = persistentMapOf(
                        "reading" to stringResource(MR.strings.pref_tracker_reading),
                        "plan_to_read" to stringResource(MR.strings.pref_tracker_plan_to_read),
                        "on_hold" to stringResource(MR.strings.pref_tracker_on_hold),
                        "completed" to stringResource(MR.strings.pref_tracker_completed),
                        "dropped" to stringResource(MR.strings.pref_tracker_dropped),
                    ),
                    title = stringResource(MR.strings.pref_chimanew_hero_tracker_statuses),
                    subtitle = stringResource(MR.strings.pref_chimanew_hero_tracker_statuses_summary),
                    enabled = heroSource == 2,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = preferences.heroStyle(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.pref_nuvio_hero_style_ambient),
                        1 to stringResource(MR.strings.pref_nuvio_hero_style_fullbleed),
                        2 to stringResource(MR.strings.pref_nuvio_hero_style_crisp_ambient),
                    ),
                    title = stringResource(MR.strings.pref_nuvio_hero_style),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.heroDimBanner(),
                    title = stringResource(MR.strings.pref_nuvio_hero_dim_banner),
                    subtitle = stringResource(MR.strings.pref_nuvio_hero_dim_banner_summary),
                    enabled = heroStyle != 1,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.heroAllowCroppedFallback(),
                    title = stringResource(MR.strings.pref_nuvio_hero_allow_cropped_fallback),
                    subtitle = stringResource(MR.strings.pref_nuvio_hero_allow_cropped_fallback_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = preferences.heroArtworkPairing(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.pref_nuvio_hero_artwork_pairing_separate),
                        1 to stringResource(MR.strings.pref_nuvio_hero_artwork_pairing_unified),
                    ),
                    title = stringResource(MR.strings.pref_nuvio_hero_artwork_pairing),
                    subtitle = stringResource(MR.strings.pref_nuvio_hero_artwork_pairing_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.heroShowUnreadBadges(),
                    title = stringResource(MR.strings.pref_nuvio_hero_show_unread_badges),
                    subtitle = stringResource(MR.strings.pref_nuvio_hero_show_unread_badges_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.useAnilistArtwork(),
                    title = stringResource(MR.strings.pref_nuvio_use_anilist_artwork),
                    subtitle = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = preferences.heroArtworkSources(),
                    entries = persistentMapOf(
                        "banner" to stringResource(MR.strings.pref_chimanew_hero_artwork_banner),
                        "cover" to stringResource(MR.strings.pref_chimanew_hero_artwork_cover),
                        "default" to stringResource(MR.strings.pref_chimanew_hero_artwork_default),
                    ),
                    title = stringResource(MR.strings.pref_chimanew_hero_artwork_sources),
                    subtitle = stringResource(MR.strings.pref_chimanew_hero_artwork_sources_summary),
                    enabled = useAnilistArtwork,
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

    @Composable
    private fun getDetailsPageGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        val useAnilistInDetails by preferences.useAnilistArtworkInDetails().collectPrefAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chimanew_details_category),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.useAnilistArtworkInDetails(),
                    title = stringResource(MR.strings.pref_chimanew_details_use_anilist),
                    subtitle = stringResource(MR.strings.pref_chimanew_details_use_anilist_summary),
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
                    enabled = useAnilistInDetails,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = preferences.detailsBackdropArtwork(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.pref_chimanew_details_backdrop_artwork_banner),
                        1 to stringResource(MR.strings.pref_chimanew_details_backdrop_artwork_cover),
                        2 to stringResource(MR.strings.pref_chimanew_details_backdrop_artwork_local),
                    ),
                    title = stringResource(MR.strings.pref_chimanew_details_backdrop_artwork),
                    enabled = useAnilistInDetails,
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
                    enabled = useAnilistInDetails,
                ),
            ),
        )
    }

    @Composable
    private fun getNavbarGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chimanew_navbar_category),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.navbarAutoHideOnScroll(),
                    title = stringResource(MR.strings.pref_chimanew_navbar_autohide),
                    subtitle = stringResource(MR.strings.pref_chimanew_navbar_autohide_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getArtworkGroup(preferences: ChimanewPreferences, navigator: Navigator): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chimanew_artwork_category),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = preferences.useAnilistArtworkInLibrary(),
                    title = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_in_library),
                    subtitle = stringResource(MR.strings.pref_nuvio_use_anilist_artwork_in_library_summary),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_chimanew_artwork_waterfall),
                    subtitle = stringResource(MR.strings.pref_chimanew_artwork_waterfall_summary),
                    onClick = {
                        navigator.push(ArtworkPriorityCustomizerScreen())
                    },
                ),
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
            ),
        )
    }

    @Composable
    private fun getRatingsGroup(preferences: ChimanewPreferences): Preference.PreferenceGroup {
        val showRatings by preferences.showCommunityRatings().collectPrefAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_chimanew_ratings_category),
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
        )
    }
}
