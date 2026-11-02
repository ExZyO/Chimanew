// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.domain.category.interactor.GetAnimeCategories
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.novel.repository.NovelCategoryRepository
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioHeroScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioHeroScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_nuvio_hero_section

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<ChimanewPreferences>() }

        val getCategories = remember { Injekt.get<GetCategories>() }
        val categories by getCategories.subscribe().collectAsState(initial = emptyList())
        val categoryEntries = remember(categories) {
            categories.associate { it.id.toString() to it.name.ifBlank { "Default" } }.toPersistentMap()
        }

        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val animeCategories by getAnimeCategories.subscribe().collectAsState(initial = emptyList())
        val animeCategoryEntries = remember(animeCategories) {
            animeCategories.associate { it.id.toString() to it.name.ifBlank { "Default" } }.toPersistentMap()
        }

        val novelCategoryRepository = remember { Injekt.get<NovelCategoryRepository>() }
        val novelCategories by novelCategoryRepository.subscribe().collectAsState(initial = emptyList())
        val novelCategoryEntries = remember(novelCategories) {
            novelCategories.associate { it.id.toString() to it.name.ifBlank { "Default" } }.toPersistentMap()
        }

        val heroSource by preferences.heroSource().collectPrefAsState()
        val heroSourceAnime by preferences.heroSourceAnime().collectPrefAsState()
        val heroSourceNovel by preferences.heroSourceNovel().collectPrefAsState()
        val showHeroManga by preferences.showHeroCarousel().collectPrefAsState()
        val showHeroAnime by preferences.showHeroCarouselAnime().collectPrefAsState()
        val showHeroNovel by preferences.showHeroCarouselNovel().collectPrefAsState()

        val styleEntries = persistentMapOf(
            0 to stringResource(MR.strings.pref_nuvio_hero_style_cinematic),
            1 to stringResource(MR.strings.pref_nuvio_hero_style_framed),
            2 to stringResource(MR.strings.pref_nuvio_hero_style_minimal),
            3 to stringResource(MR.strings.pref_nuvio_hero_style_ambient),
            4 to stringResource(MR.strings.pref_nuvio_hero_style_glassmorphic),
            5 to stringResource(MR.strings.pref_nuvio_hero_style_magazine),
            6 to stringResource(MR.strings.pref_nuvio_hero_style_split),
        )

        return listOf(
            Preference.PreferenceGroup(
                title = "Home Media Switcher",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.homeActiveMedia(),
                        entries = persistentMapOf(
                            0 to "Manga & Comics",
                            1 to "Anime",
                            2 to "Light Novels",
                        ),
                        title = "Active Media Feed",
                        subtitle = "Select which media library is active by default on the Home screen",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.enableAnimeModule(),
                        title = "Enable Anime Feed",
                        subtitle = "Show Anime tab in Home Media Switcher",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.enableNovelModule(),
                        title = "Enable Novel Feed",
                        subtitle = "Show Novels tab in Home Media Switcher",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Manga Hero Banner",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showHeroCarousel(),
                        title = "Show Manga Hero Banner",
                        subtitle = stringResource(MR.strings.pref_nuvio_show_hero_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroSource(),
                        entries = persistentMapOf(
                            0 to stringResource(MR.strings.pref_nuvio_hero_source_history),
                            1 to stringResource(MR.strings.pref_nuvio_hero_source_favorites),
                            2 to stringResource(MR.strings.pref_nuvio_hero_source_trackers),
                            3 to "Trending Manga (AniList Discovery)",
                            4 to "Trending Manhwa (AniList Discovery)",
                        ),
                        title = stringResource(MR.strings.pref_nuvio_hero_source),
                        subtitle = "%s",
                        enabled = showHeroManga,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroStyle(),
                        entries = styleEntries,
                        title = "Manga Hero Visual Style",
                        subtitle = "%s",
                        enabled = showHeroManga,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.heroCategories(),
                        entries = categoryEntries,
                        title = stringResource(MR.strings.pref_chimanew_hero_categories_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_hero_categories_summary),
                        enabled = showHeroManga && heroSource == 1,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.heroTrackerStatuses(),
                        entries = persistentMapOf(
                            "reading" to stringResource(MR.strings.pref_nuvio_shelf_reading),
                            "plan_to_read" to stringResource(MR.strings.pref_nuvio_shelf_plan_to_read),
                            "on_hold" to stringResource(MR.strings.pref_nuvio_shelf_on_hold),
                            "completed" to stringResource(MR.strings.pref_nuvio_shelf_completed),
                        ),
                        title = stringResource(MR.strings.pref_chimanew_hero_tracker_statuses_title),
                        subtitle = stringResource(MR.strings.pref_chimanew_hero_tracker_statuses_summary),
                        enabled = showHeroManga && heroSource == 2,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Anime Hero Banner",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showHeroCarouselAnime(),
                        title = "Show Anime Hero Banner",
                        subtitle = "Display featured anime in a hero banner",
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroSourceAnime(),
                        entries = persistentMapOf(
                            0 to "Continue Watching (History)",
                            1 to "Anime Categories",
                            2 to "Tracker Statuses",
                            3 to "Trending Anime (AniList Discovery)",
                        ),
                        title = "Anime Hero Source",
                        subtitle = "%s",
                        enabled = showHeroAnime,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroStyleAnime(),
                        entries = styleEntries,
                        title = "Anime Hero Visual Style",
                        subtitle = "%s",
                        enabled = showHeroAnime,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.heroCategoriesAnime(),
                        entries = animeCategoryEntries,
                        title = "Anime Hero Categories",
                        subtitle = "Select categories to feature in Anime Hero",
                        enabled = showHeroAnime && heroSourceAnime == 1,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.heroTrackerStatusesAnime(),
                        entries = persistentMapOf(
                            "reading" to "Watching",
                            "plan_to_read" to "Plan to Watch",
                            "on_hold" to "On Hold",
                            "completed" to "Completed",
                        ),
                        title = "Anime Hero Tracker Statuses",
                        subtitle = "Select tracker statuses to feature in Anime Hero",
                        enabled = showHeroAnime && heroSourceAnime == 2,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Novel Hero Banner",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.showHeroCarouselNovel(),
                        title = "Show Novel Hero Banner",
                        subtitle = "Display featured light novels in a hero banner",
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroSourceNovel(),
                        entries = persistentMapOf(
                            0 to "Continue Reading (History)",
                            1 to "Novel Categories",
                            2 to "Library Novels",
                            3 to "Trending Light Novels (AniList Discovery)",
                        ),
                        title = "Novel Hero Source",
                        subtitle = "%s",
                        enabled = showHeroNovel,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.heroStyleNovel(),
                        entries = styleEntries,
                        title = "Novel Hero Visual Style",
                        subtitle = "%s",
                        enabled = showHeroNovel,
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = preferences.heroCategoriesNovel(),
                        entries = novelCategoryEntries,
                        title = "Novel Hero Categories",
                        subtitle = "Select categories to feature in Novel Hero",
                        enabled = showHeroNovel && heroSourceNovel == 1,
                    ),
                ),
            ),
        )
    }
}
