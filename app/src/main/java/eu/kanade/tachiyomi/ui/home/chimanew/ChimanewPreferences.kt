// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import tachiyomi.core.common.preference.PreferenceStore

class ChimanewPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showHeroCarousel() = preferenceStore.getBoolean("pref_nuvio_show_hero", true)

    fun heroSource() = preferenceStore.getInt("pref_nuvio_hero_source", 0)

    fun heroStyle() = preferenceStore.getInt("pref_nuvio_hero_style", 0)

    fun useAnilistArtwork() = preferenceStore.getBoolean("pref_nuvio_use_anilist_artwork", true)

    fun homeShelfOrder() = preferenceStore.getString(
        "pref_chimanew_home_shelf_order",
        "continue_reading:true,reading:true,plan_to_read:true,on_hold:true,completed:true,dropped:false,not_tracked:true,categories:true,recently_added:true",
    )

    fun showContinueReading() = preferenceStore.getBoolean("pref_nuvio_show_continue_reading", true)

    fun showRecentlyUpdated() = preferenceStore.getBoolean("pref_nuvio_show_recently_updated", true)

    fun showCategoryShelves() = preferenceStore.getBoolean("pref_nuvio_show_category_shelves", true)

    fun showTrackingShelves() = preferenceStore.getBoolean("pref_nuvio_show_tracking_shelves", true)

    fun showProgressBars() = preferenceStore.getBoolean("pref_nuvio_show_progress", true)

    fun showUnreadBadges() = preferenceStore.getBoolean("pref_nuvio_show_unread_badges", true)

    fun continueReadingTapAction() = preferenceStore.getInt("pref_chimanew_continue_reading_tap_action", 1)

    fun shelfTapAction() = preferenceStore.getInt("pref_chimanew_shelf_tap_action", 0)

    fun heroCategories() = preferenceStore.getStringSet("pref_chimanew_hero_categories", emptySet())

    fun heroArtworkSources() = preferenceStore.getStringSet(
        "pref_chimanew_hero_artwork_sources",
        setOf("banner", "cover", "default"),
    )

    fun heroTrackerStatuses() = preferenceStore.getStringSet(
        "pref_chimanew_hero_tracker_statuses",
        setOf("reading"),
    )

    fun navbarAutoHideOnScroll() = preferenceStore.getBoolean("pref_chimanew_navbar_autohide", true)

    fun useAnilistArtworkInDetails() = preferenceStore.getBoolean("pref_chimanew_details_use_anilist", true)

    fun detailsBackdropStyle() = preferenceStore.getInt("pref_chimanew_details_backdrop_style", 0)

    fun detailsBackdropArtwork() = preferenceStore.getInt("pref_chimanew_details_backdrop_artwork", 0)

    fun artworkWaterfallOrder() = preferenceStore.getString(
        "pref_chimanew_artwork_waterfall_order",
        "ANILIST,KITSU,MANGADEX",
    )

    fun ambientCanvasStyle() = preferenceStore.getInt(
        "pref_chimanew_ambient_canvas_style",
        0, // 0 = Cinematic Blur, 1 = Palette Mesh, 2 = Hybrid Texture
    )

    fun mangaCustomBannerUrl(mangaId: Long) = preferenceStore.getString("pref_chimanew_manga_banner_$mangaId", "")
}
