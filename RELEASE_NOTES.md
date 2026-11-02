# What's New in Chimanew v2.6.64

### Consolidated Settings Hub and Submenus
* **Organized Submenu Architecture**: Restructured the Chimanew settings screen into seven dedicated sub-screens matching the standard app settings hierarchy:
  - Appearance and Posters: Interactive poster size customizer, card corner radius, ambient canvas, navbar auto-hide, badges, and details page backdrops.
  - Home and Hero Banner: Media switcher controls, independent Manga, Anime, and Novel hero carousel configurations, source filtering, and visual banner styles.
  - Shelves and Rows: Home shelf customizer for reordering, visibility switches for standard shelves, and configurable card tap interactions.
  - Artwork Sources and Priority: Separate waterfall priority orders for Manga, Anime, and Novels, per-category artwork override rules, and cache management.
  - Library and Updates Engine: Tracker-assisted updates, automatic check on app launch, series consolidation in updates feed, and media update restrictions.
  - Community Ratings: Public aggregate ratings from AniList, MyAnimeList, and Kitsu with customizable display formats and placement switches.
  - Diagnostics and Logs: Real-time subsystem diagnostic logging, category filtering, log export to clipboard, and integrated live log viewer.

### Media-Isolated Artwork Waterfall and Sources
* **Dedicated Anime and Novel Artwork Waterfall**: Separated artwork priority lists and source exclusions across Manga, Anime, and Light Novels.
* **Accurate Anime Artwork Queries**: AniList GraphQL queries now dynamically pass media type ANIME for anime entries, Kitsu queries the anime endpoint, and Bangumi targets anime type. This permanently stops manga volume covers from appearing on TV anime titles.
* **Namespaced Artwork Cache**: All cached entries now use media-isolated cache keys (manga:, anime:, novel:) to prevent cross-media cover collisions.

### Anime and Novel Manual Artwork Pickers
* **Interactive Bottom Sheets**: Added dedicated artwork pickers for both Anime and Light Novel details screens, accessible directly from cover preview dialogs.
* **Full Source Control**: Browse remote high-resolution covers and banners across all active providers, select custom local image files, preview aspect ratios, or exclude specific providers for individual titles.

### Artwork Cache Reset Confirmation Dialog
* **Accidental Tap Protection**: Tapping Reset Artwork Cache now presents a confirmation dialog with Cancel and Reset actions before clearing downloaded artwork and metadata.

### Updates Feed Uncapped and Frame Rate Polish
* **Uncapped Update Feed**: Raised database update query ceilings from 10,000 to 250,000 rows across Manga, Anime, and Novels, ensuring ongoing titles are never truncated in large libraries.
* **Recomposition Log Throttle**: Wrapped floating dock logging in LaunchedEffect, eliminating high-frequency frame spam during screen animations.
* **Hero Banner Shuffle on Refresh**: Pulling down to refresh on Home now shuffles and rotates featured hero banner entries across all media types.
