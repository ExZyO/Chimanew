// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import tachiyomi.core.common.preference.PreferenceStore

class ChimanewPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showHeroCarousel() = preferenceStore.getBoolean("pref_nuvio_show_hero", true)

    fun heroSource() = preferenceStore.getInt("pref_nuvio_hero_source", 0)

    fun heroStyle() = preferenceStore.getInt("pref_nuvio_hero_style", 0)

    fun showContinueReading() = preferenceStore.getBoolean("pref_nuvio_show_continue_reading", true)

    fun showRecentlyUpdated() = preferenceStore.getBoolean("pref_nuvio_show_recently_updated", true)

    fun showCategoryShelves() = preferenceStore.getBoolean("pref_nuvio_show_category_shelves", true)

    fun showTrackingShelves() = preferenceStore.getBoolean("pref_nuvio_show_tracking_shelves", true)

    fun showProgressBars() = preferenceStore.getBoolean("pref_nuvio_show_progress", true)

    fun showUnreadBadges() = preferenceStore.getBoolean("pref_nuvio_show_unread_badges", true)
}
