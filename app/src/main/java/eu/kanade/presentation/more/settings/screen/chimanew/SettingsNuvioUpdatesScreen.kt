// EaZy Nuvio+ / Chimanew
package eu.kanade.presentation.more.settings.screen.chimanew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_NON_VIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.NOVEL_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.NOVEL_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.NOVEL_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.NOVEL_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNuvioUpdatesScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsNuvioUpdatesScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.label_recent_updates

    @Composable
    override fun getPreferences(): List<Preference> {
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val updatesPreferences = remember { Injekt.get<UpdatesPreferences>() }

        return listOf(
            Preference.PreferenceGroup(
                title = "Smart Updates & Tracker Sync",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = libraryPreferences.trackerAssistedUpdates(),
                        title = "Tracker-Assisted Updates",
                        subtitle = "Skip checking source extensions when local chapter or episode count matches tracker data (AniList, MyAnimeList, MangaUpdates, Kitsu). Dramatically speeds up library updates and saves bandwidth.",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = libraryPreferences.autoCheckUpdatesOnLaunch(),
                        title = "Check Updates on App Launch",
                        subtitle = "Automatically trigger a library update when opening the app. Includes an automatic 15-minute cooldown to prevent excessive requests on quick app switches.",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Updates Feed Presentation",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = updatesPreferences.consolidateUpdates(),
                        title = "Consolidate Updates by Series",
                        subtitle = "Group new chapters and episodes into expandable series cards in the Updates tab instead of a long flat list.",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Network & Device Conditions",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = libraryPreferences.autoUpdateDeviceRestrictions(),
                        entries = persistentMapOf(
                            DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                            DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                            DEVICE_CHARGING to stringResource(MR.strings.charging),
                        ),
                        title = stringResource(MR.strings.pref_library_update_restriction),
                        subtitle = stringResource(MR.strings.restrictions),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Media Update Restrictions",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = libraryPreferences.autoUpdateMangaRestrictions(),
                        entries = persistentMapOf(
                            MANGA_HAS_UNREAD to stringResource(MR.strings.pref_update_only_completely_read),
                            MANGA_NON_READ to stringResource(MR.strings.pref_update_only_started),
                            MANGA_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                            MANGA_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                        ),
                        title = stringResource(MR.strings.pref_library_update_smart_update),
                        subtitle = "Skip manga series matching selected criteria during updates",
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = libraryPreferences.autoUpdateAnimeRestrictions(),
                        entries = persistentMapOf(
                            ANIME_HAS_UNVIEWED to stringResource(MR.strings.pref_update_only_completely_seen),
                            ANIME_NON_VIEWED to stringResource(MR.strings.pref_update_only_started),
                            ANIME_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                            ANIME_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                        ),
                        title = stringResource(MR.strings.pref_anime_library_update_smart_update),
                        subtitle = "Skip anime series matching selected criteria during updates",
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = libraryPreferences.autoUpdateNovelRestrictions(),
                        entries = persistentMapOf(
                            NOVEL_HAS_UNREAD to stringResource(MR.strings.pref_update_only_completely_read),
                            NOVEL_NON_READ to stringResource(MR.strings.pref_update_only_started),
                            NOVEL_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                            NOVEL_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                        ),
                        title = stringResource(MR.strings.label_novels) + " " + stringResource(MR.strings.pref_library_update_smart_update),
                        subtitle = "Skip novel series matching selected criteria during updates",
                    ),
                ),
            ),
        )
    }
}
