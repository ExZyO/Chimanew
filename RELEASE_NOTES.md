# What's New in Chimanew v2.6.61

### 📑 Consolidated Updates Feed & Anti-Lag Optimization
* **Consolidated Series Cards**: Say goodbye to 100s of duplicate rows when a series drops multiple chapters! Updates are now consolidated into a single card per title, displaying the latest chapter name, a relative timestamp ("15m ago"), and a `+X` pill badge for extra chapters.
* **Bump-to-Top Sorting**: When a series receives a new chapter or episode, it immediately moves to the very top of your feed above earlier entries.
* **Smart Tap & Expandable Cards**:
  - **Tap Title / Row**: Instantly begins reading or watching the earliest unread chapter/episode.
  - **Tap Chevron (`v`)**: Smoothly expands the card in-place to reveal individual child chapters.
  - **Tap Cover**: Directly opens the entry details screen.
* **Full Group Gestures & Downloads**:
  - **Long-Press**: Selects all new chapters in that card for bulk download, marking as read, or deleting.
  - **Swipe**: Swiping the card marks, bookmarks, or downloads all new chapters in that series group.
  - **Download Button**: One-tap downloads all new chapters in the card.
  - **When Expanded**: Individual chapters can still be tapped, swiped, or selected independently.
* **Silky Smooth 120 FPS Scrolling**: Applied Library performance parity to the Updates feed. Collapsed child chapters are omitted from lazy composition, dropping active list items from 1,500 down to ~100 for lag-free scrolling.
* **Feed Preference Toggle**: Easily switch between Consolidated Mode and Classic List anytime in Settings or the Updates filter sheet.

### 🛡️ 1500-Item Truncation Ceiling & Multi-Scanlator Tracker Precision
* **Anti-Truncation Limit**: Raised the SQL update buffer ceiling from 500 to 1,500 across Manga, Anime, and Novels, ensuring large bulk releases never push your older updates off the screen.
* **Tracker Update Precision**: Solved duplicate scanlators and decimal chapters (`10.5`, extras) by comparing tracker totals against `latestChapterNumber` (`GetChapterSummaryByMangaId`).
* **ComicK Chapter Count Sync**: Automatically retrieves total chapters from ComicK metadata (`/titles/{hid}`) on refresh.

### 🐛 Bug Fixes & Diagnostics
* **Fixed Startup Crash Loop**: Shielded `MainActivity` from restoring orphaned extension settings dialogs after process recreation, and implemented `SafeEditTextPreferenceDialogFragment` to gracefully dismiss without crashing.
* **Zero 24/7 Background Logging**: Restored system `logcat *:E` and routed all tracker update engine diagnostics through Chimanew's in-memory `ChimanewDiagnostics` under a toggleable `UPDATES` category.
* **Instant App Update Check**: Removed the app update check cooldown so GitHub releases are checked immediately on launch.

# What's New in Chimanew v2.6.60

### 🔍 Tracker Engine Logging & Restriction Bypass Polish
* **High-Visibility Logging**: Added detailed `[TrackerUpdateEngine]` and `[LaunchUpdate]` log messages recording every single step of the update lifecycle, including candidate probing, chapter differentials, restriction bypasses, and skipped breakdowns.
* **Completed Status Bypass**: If a series was marked as Completed on the source, but ComicK, AniList, or another tracker signals that new chapters or a sequel released, `MANGA_NON_COMPLETED` is now bypassed automatically!
* **Transparent Skipped Breakdown**: The logs now explicitly record the reason for every single skipped title so you can verify exactly why any manga wasn't checked.

# What's New in Chimanew v2.6.59

### 🚀 Check for Updates on App Launch
* **Auto-Check on Launch Toggle**: Added a new setting under **Settings → Library → Global update** to automatically check your library for new chapters whenever you open the app.
* **Smart 15-Minute Cooldown**: To protect your battery and data, launch updates include an intelligent 15-minute cooldown so briefly switching apps or replying to messages won't trigger unnecessary re-checks.

# What's New in Chimanew v2.6.58

### ⚡ Phase 2: Tracker-Assisted Update Engine
* **Tracker-Assisted Updates**: Connected linked trackers (ComicK, MangaDex, MangaUpdates, AniList) directly into the library update engine.
* **Smart Interval Bypass**: Series whose release intervals pushed them outside the normal check window (`MANGA_OUTSIDE_RELEASE_PERIOD`) are no longer skipped for weeks if their linked trackers signal that new chapters were published.
* **Active Tracker Pre-Flight**: For series on hiatus or irregular schedules, the engine performs a lightweight pre-flight check across logged-in trackers to detect unannounced releases.
* **Priority Queue Elevation**: Titles with confirmed tracker update signals are moved to the very front of the update queue so newly published chapters download first.
* **Polite Source Throttling**: Added gentle 250ms spacing between sequential requests to the same source domain, eliminating Cloudflare 403 blocks and 429 rate limit errors during large library updates.
* **Global Update Toggle**: Added a toggle under **Settings → Library → Global update** to control tracker-assisted updates.

# What's New in Chimanew v2.6.57

### 🛠️ ComicK Tracker Search Fix
* **Fixed ComicK Title Search**: Fixed a JSON parsing mismatch (`Unexpected JSON token`) when searching ComicK for manga entries by aligning the search endpoint with ComicK's authenticated integrations API (`/integrations/v1/titles`).
* **Query Safeguards**: Added automatic 100-character query capping to prevent ComicK API 400 Bad Request errors.
* **Resilient Model Fallbacks**: Added safe default fallbacks for missing metadata fields from search responses.

# What's New in Chimanew v2.6.56

### 📚 Official ComicK Tracker Integration
* **Full ComicK Tracker Support**: Track your reading progress, library statuses, and chapter numbers directly with ComicK.
* **OAuth 2.0 PKCE Authentication**: Secure, one-tap browser login with PKCE S256 verification and automatic token refresh.
* **Smart Progress Synchronization**: Automatically syncs your last read chapter with two-decimal precision, preventing floating-point number artifacts.
* **Manga Search & Quick HID Linking**: Search titles across ComicK's library or link directly using ComicK IDs (`id:xyz123`).
* **Complete Status Management**: Seamlessly manage your ComicK reading statuses (`Reading`, `Completed`, `On Hold`, `Dropped`, `Plan to Read`) and ratings directly within Chimanew.
* **Branded Visual Experience**: Official ComicK branding across **Settings → Tracking** and the series tracking sheet.
