// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew

import tachiyomi.core.common.preference.PreferenceStore

class ChimanewPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showHeroCarousel() = preferenceStore.getBoolean("pref_nuvio_show_hero", true)

    fun heroSource() = preferenceStore.getInt("pref_nuvio_hero_source", 0)

    fun heroStyle() = preferenceStore.getInt("pref_nuvio_hero_style", 0)

    fun heroMetadataLayout() = preferenceStore.getInt("pref_chimanew_hero_metadata_layout", 0)

    fun heroDimBanner() = preferenceStore.getBoolean("pref_nuvio_hero_dim_banner", true)

    fun heroAllowCroppedFallback() = preferenceStore.getBoolean("pref_nuvio_hero_allow_cropped_fallback", true)

    fun heroRequireSelectedArtwork() = preferenceStore.getBoolean("pref_chimanew_hero_require_selected_artwork", false)

    fun heroStrictArtworkRequirements() = preferenceStore.getStringSet(
        "pref_chimanew_hero_strict_artwork_requirements",
        setOf("banner", "cover"),
    )

    fun useAnilistArtwork() = preferenceStore.getBoolean("pref_nuvio_use_anilist_artwork", true)

    fun homeShelfOrder() = preferenceStore.getString(
        "pref_chimanew_home_shelf_order",
        "continue_reading:true,reading:true,plan_to_read:true,on_hold:true,completed:true,dropped:false,not_tracked:true,categories:true,recently_added:true",
    )

    fun homeActiveMedia() = preferenceStore.getInt("pref_nuvio_home_active_media", 0) // 0 = Manga, 1 = Anime, 2 = Novels

    fun homeShelfOrderAnime() = preferenceStore.getString(
        "pref_chimanew_home_shelf_order_anime",
        "continue_watching:true,watching:true,plan_to_watch:true,on_hold:true,completed:true,dropped:false,not_tracked:true,categories:true,recently_added:true,trending_anime:true",
    )

    fun homeShelfOrderNovel() = preferenceStore.getString(
        "pref_chimanew_home_shelf_order_novel",
        "continue_reading:true,reading:true,plan_to_read:true,on_hold:true,completed:true,dropped:false,not_tracked:true,categories:true,recently_added:true,trending_novels:true",
    )

    fun showHeroCarouselAnime() = preferenceStore.getBoolean("pref_nuvio_show_hero_anime", true)
    fun heroSourceAnime() = preferenceStore.getInt("pref_nuvio_hero_source_anime", 0) // 0 = History, 1 = Categories, 2 = Trackers, 3 = Trending Anime
    fun heroCategoriesAnime() = preferenceStore.getStringSet("pref_chimanew_hero_categories_anime", emptySet())
    fun heroTrackerStatusesAnime() = preferenceStore.getStringSet(
        "pref_chimanew_hero_tracker_statuses_anime",
        setOf("reading"),
    )

    fun showHeroCarouselNovel() = preferenceStore.getBoolean("pref_nuvio_show_hero_novel", true)
    fun heroSourceNovel() = preferenceStore.getInt("pref_nuvio_hero_source_novel", 0) // 0 = History, 1 = Categories, 2 = Reading, 3 = Trending Novels
    fun heroCategoriesNovel() = preferenceStore.getStringSet("pref_chimanew_hero_categories_novel", emptySet())

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

    // Separate Anime Artwork Orders
    fun animeArtworkBannerOrder() = preferenceStore.getString(
        "pref_chimanew_anime_artwork_banner_order",
        "anilist:true,kitsu:true,bangumi:true,default:true",
    )

    fun animeArtworkCoverOrder() = preferenceStore.getString(
        "pref_chimanew_anime_artwork_cover_order",
        "anilist:true,kitsu:true,bangumi:true,default:true",
    )

    // Separate Novel Artwork Orders
    fun novelArtworkBannerOrder() = preferenceStore.getString(
        "pref_chimanew_novel_artwork_banner_order",
        "bookwalker:true,naver:true,kuaikan:true,bangumi:true,anilist:true,default:true",
    )

    fun novelArtworkCoverOrder() = preferenceStore.getString(
        "pref_chimanew_novel_artwork_cover_order",
        "bookwalker:true,naver:true,kuaikan:true,bangumi:true,anilist:true,default:true",
    )

    fun heroStyleAnime() = preferenceStore.getInt("pref_nuvio_hero_style_anime", 0)
    fun heroStyleNovel() = preferenceStore.getInt("pref_nuvio_hero_style_novel", 0)

    fun animeCustomBannerUrl(animeId: Long) = preferenceStore.getString("pref_chimanew_anime_banner_$animeId", "")
    fun animeExcludedCoverSources(animeId: Long) = preferenceStore.getStringSet("pref_chimanew_anime_excluded_cover_sources_$animeId", emptySet())
    fun animeExcludedBannerSources(animeId: Long) = preferenceStore.getStringSet("pref_chimanew_anime_excluded_banner_sources_$animeId", emptySet())

    fun novelCustomBannerUrl(novelId: Long) = preferenceStore.getString("pref_chimanew_novel_banner_$novelId", "")
    fun novelExcludedCoverSources(novelId: Long) = preferenceStore.getStringSet("pref_chimanew_novel_excluded_cover_sources_$novelId", emptySet())
    fun novelExcludedBannerSources(novelId: Long) = preferenceStore.getStringSet("pref_chimanew_novel_excluded_banner_sources_$novelId", emptySet())

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

    // Per-entry artwork source exclusion (covers and banners separated) // EaZy Nuvio+ / Chimanew
    fun mangaExcludedCoverSources(mangaId: Long) = preferenceStore.getStringSet(
        "pref_chimanew_manga_excluded_cover_sources_$mangaId",
        emptySet(),
    )

    fun mangaExcludedBannerSources(mangaId: Long) = preferenceStore.getStringSet(
        "pref_chimanew_manga_excluded_banner_sources_$mangaId",
        emptySet(),
    )

    fun mangaExcludedArtworkSources(mangaId: Long) = mangaExcludedCoverSources(mangaId)

    // Artwork priority order tracking for smart re-evaluation on reorder // EaZy Nuvio+ / Chimanew
    fun artworkPriorityOrderHash() = preferenceStore.getString("pref_chimanew_artwork_priority_order_hash", "")

    // Media sections toggle // EaZy Nuvio+ / Chimanew
    fun enableAnimeModule() = preferenceStore.getBoolean("pref_chimanew_enable_anime", true)
    fun enableNovelModule() = preferenceStore.getBoolean("pref_chimanew_enable_novels", true)

    // Diagnostics & Logs // EaZy Nuvio+ / Chimanew
    fun enableDiagnostics() = preferenceStore.getBoolean("pref_chimanew_enable_diagnostics", false)
    fun diagnosticCategories() = preferenceStore.getStringSet(
        "pref_chimanew_diagnostic_categories",
        setOf("network", "artwork", "hero", "details", "shelves", "updates"),
    )

    // Category Artwork Rules // EaZy Nuvio+ / Chimanew
    fun categoryArtworkRulesConfig() = preferenceStore.getString(
        "pref_chimanew_category_artwork_rules_config",
        "",
    )

    fun categoryArtworkRulesUpdated() = preferenceStore.getLong(
        "pref_chimanew_category_artwork_rules_updated",
        0L,
    )
}
