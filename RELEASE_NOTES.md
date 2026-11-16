# What's New in Chimanew v2.6.82

### Hero Carousel Precision & Artwork Alignment
* **Keyed Pager State Architecture**: Keyed the hero carousel pager state to the active list of hero items. When refreshing the Home screen or shuffling hero items, the pager resets cleanly at page index 0 without rendering stale slide positions or transient frames from prior entries.
* **Strict Title Validation Across Providers**: Added comprehensive Unicode-safe title validation (`isTitleMatch`) across AniList, Bangumi, Kuaikan, and tracker lookup pipelines. Fuzzy searches and tracker records for unrelated titles (e.g. tracks for different series) are strictly rejected, preventing foreign banners from being linked to an entry.
* **Automated Legacy Cache Sanitization**: Added an automated one-time cache purge (`pref_nuvio_artwork_cache_sanitized_v`) on upgrade to v2.6.82, wiping corrupted legacy disk mappings and ensuring only authentic, verified banners are cached.
