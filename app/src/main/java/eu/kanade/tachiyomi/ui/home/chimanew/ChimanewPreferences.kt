// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import tachiyomi.core.common.preference.PreferenceStore

class ChimanewPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showHeroCarousel() = preferenceStore.getBoolean("pref_nuvio_show_hero", true)

    fun heroSource() = preferenceStore.getInt("pref_nuvio_hero_source", 0)

    fun heroStyle() = preferenceStore.getInt("pref_nuvio_hero_style", 0)

    fun heroDimBanner() = preferenceStore.getBoolean("pref_nuvio_hero_dim_banner", true)

    fun heroAllowCroppedFallback() = preferenceStore.getBoolean("pref_nuvio_hero_allow_cropped_fallback", true)

    fun heroRequireSelectedArtwork() = preferenceStore.getBoolean("pref_chimanew_hero_require_selected_artwork", false)

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

    fun useAnilistArtworkInLibrary() = preferenceStore.getBoolean("pref_chimanew_library_use_anilist", true)

    fun detailsBackdropStyle() = preferenceStore.getInt("pref_chimanew_details_backdrop_style", 0)

    fun detailsBackdropArtwork() = preferenceStore.getInt("pref_chimanew_details_backdrop_artwork", 0)

    fun detailsPageLayout() = preferenceStore.getInt(
        "pref_chimanew_details_page_layout",
        0, // 0 = Nuvio Hero (Unblocked banner, centered title & genres), 1 = Canvas Stack (Full banner top, cover below), 2 = Feathered Vignette (Full edge-to-edge overlay)
    )

    fun heroArtworkPairing() = preferenceStore.getInt("pref_nuvio_hero_artwork_pairing", 0) // 0 = Separate with fallback, 1 = Unified

    fun heroShowUnreadBadges() = preferenceStore.getBoolean("pref_nuvio_hero_show_unread_badges", true)

    fun detailsBackdropScale() = preferenceStore.getInt("pref_chimanew_details_backdrop_scale", 2) // 0 = Ambient Backdrop (Full Art + Blur), 1 = Natural Fit (Compact), 2 = Fill Screen (Crop)

    fun heroFallbackStyle() = preferenceStore.getInt("pref_nuvio_hero_fallback_style", 0) // 0 = Clean Subtle Aura, 1 = Ambient Mesh Canvas, 2 = Cropped Fill

    fun artworkWaterfallOrder() = preferenceStore.getString(
        "pref_chimanew_artwork_waterfall_order",
        "naver:true,kuaikan:true,bookwalker:true,bangumi:true,anilist:true,kitsu:true,mangadex:true,default:true",
    )

    fun artworkBannerOrder() = preferenceStore.getString(
        "pref_chimanew_artwork_banner_order",
        "naver:true,kuaikan:true,bangumi:true,anilist:true,kitsu:true",
    )

    fun artworkCoverOrder() = preferenceStore.getString(
        "pref_chimanew_artwork_cover_order",
        "mangadex:true,bookwalker:true,bangumi:true,anilist:true,naver:true,kuaikan:true,kitsu:true,default:true",
    )

    fun showCommunityRatings() = preferenceStore.getBoolean("pref_chimanew_show_community_ratings", true)

    fun communityRatingFormat() = preferenceStore.getInt(
        "pref_chimanew_community_rating_format",
        0, // 0 = Golden Star, 1 = Percentage, 2 = Star + Source, 3 = Percentage + Source, 4 = Combined Multi-Tracker
    )

    fun showRatingsOnHero() = preferenceStore.getBoolean("pref_chimanew_show_ratings_on_hero", true)

    fun showRatingsInDetails() = preferenceStore.getBoolean("pref_chimanew_show_ratings_in_details", true)

    fun ambientCanvasStyle() = preferenceStore.getInt(
        "pref_chimanew_ambient_canvas_style",
        0, // 0 = Cinematic Blur, 1 = Palette Mesh, 2 = Hybrid Texture
    )

    fun mangaCustomBannerUrl(mangaId: Long) = preferenceStore.getString("pref_chimanew_manga_banner_$mangaId", "")

    // Media sections toggle // EaZy Nuvio+ / Chimanew
    fun enableAnimeModule() = preferenceStore.getBoolean("pref_chimanew_enable_anime", true)
    fun enableNovelModule() = preferenceStore.getBoolean("pref_chimanew_enable_novels", true)
}
