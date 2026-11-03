# What's New in Chimanew v2.6.71

### Unified Global Media Mode
* **Universal Tab Synchronization**: Switching the media category (Manga, Anime, or Novels) in any tab now synchronizes all tabs across the application in real time, including Home, Library, Updates, History, and Browse.
* **Seamless Bidirectional Switching**: Changing modes via the top dropdown in Library or Browse, swiping pages in Updates or History, or toggling shelves in Home immediately aligns every other screen to the same active media type.

### Cloudflare Protection & Fast-Skip
* **Per-Source Fast-Skip**: When a source encounters an interactive Cloudflare challenge during a library update run, the engine registers the blocked source and instantly skips all subsequent titles from that source without waiting for the 30-second latch timeout each time.
* **Direct WebView Action Notification**: Posts an immediate actionable notification ("Cloudflare verification required: [Source Name] - Tap to open WebView") whenever a source is blocked. Tapping opens the source directly in WebView so verification can be completed with a single tap. Notifications are strictly deduplicated to appear once per source per update pass.

### Library Update Restriction Integrity
* **Honored Unread Manga Restriction**: Fixed an issue where manual launch updates bypassed the user's "Entries with unread chapters" restriction. Titles with unread chapters are now properly skipped unless an active tracker signal reliably indicates a new release, reducing general update passes from hundreds of unnecessary requests down to caught-up titles in seconds.
* **Expanded Tracker Live Probe**: Increased the uncached candidate tracker probe quota from 5 to 20 titles, ordered by most recently read, with optimized 400ms pacing that stays comfortably within external API rate limits.
