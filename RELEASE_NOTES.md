# What's New in Chimanew v2.6.63

### Home Media Switcher and Multi-Media Feed
* **Compact Media Switcher**: Added a clean, dark translucent pill in the ambient header allowing seamless switching between Manga, Anime, and Novels feeds without cluttering the screen.
* **Dedicated Anime Shelves**: Added Continue Watching, Watching, Plan to Watch, On Hold, Completed, Anime Categories, Recently Added, and AniList Trending Anime shelves.
* **Dedicated Novel Shelves**: Added Continue Reading, Reading, Plan to Read, On Hold, Completed, Novel Categories, Recently Added, and AniList Trending Light Novels shelves.
* **AniList Discovery Integration**: Added curated discovery shelves for Trending Manga, Trending Manhwa, Trending Anime, and Trending Light Novels backed by a two-hour disk cache for zero-latency startup and minimal network overhead.
* **Universal Multi-Media Routing**: Tapping any card routes directly to its native experience (video player for anime, reader for novels/manga, and global search with source selection for discovery items).

### Multi-Media Hero Banners and Shelf Customizer
* **Per-Media Hero Customization**: Configurable hero banner sources for Manga, Anime, and Novels, allowing users to feature in-progress titles, custom library categories, tracker statuses, or trending discovery selections.
* **Multi-Media Shelf Reorder Screen**: Unified shelf customizer supporting Manga, Anime, and Novel tabs for reordering, enabling, or disabling any shelf or discovery row.

### Official Poster Card Styling and Depth Effects
* **Poster and Card Customization**: Added dedicated styling controls under Settings > Appearance with live interactive preview.
* **Custom Dimensions and Visuals**: Adjustable card width, corner radius, 3D highlight edge glow, sheen coverage, and label visibility toggles.

### Cloudflare Turnstile Bypass Engine
* **Native Touch Event Injection**: Implemented trusted hardware touchscreen motion event dispatching to solve modern Cloudflare Turnstile verification loops on AnimePahe and protected sources.
* **Chromium Metadata and User-Agent Pinning**: Stripped webview fingerprints, spoofed Chromium brand metadata via AndroidX Webkit, and pinned the solving User-Agent to wire requests carrying clearance tokens.

### Anime Migration and Library Update Fixes
* **Migration Actions Restored**: Fixed disabled Migrate All and Copy All buttons, and restored entry action menus in the migration list.
* **Manual Update Bypass**: Ongoing titles are no longer silently skipped during manual library updates when unread or release window restrictions are active.
* **Comprehensive Diagnostic Logging**: Added targeted diagnostic log tags across updates, Cloudflare verification, migration, discovery feeds, and home media switching.
