# What's New in Chimanew v2.6.61

### Consolidated Updates Feed & Anti-Lag Optimization
* **Consolidated Series Cards**: Updates are now consolidated into a single card per title, displaying the latest chapter name, a relative timestamp ("15m ago"), and a +X pill badge for extra chapters.
* **Bump-to-Top Sorting**: When a series receives a new chapter or episode, it immediately moves to the very top of your feed above earlier entries.
* **Smart Tap & Expandable Cards**:
  - **Tap Title / Row**: Instantly begins reading or watching the earliest unread chapter/episode.
  - **Tap Chevron (v)**: Smoothly expands the card in-place to reveal individual child chapters.
  - **Tap Cover**: Directly opens the entry details screen.
* **Full Group Gestures & Downloads**:
  - **Long-Press**: Selects all new chapters in that card for bulk download, marking as read, or deleting.
  - **Swipe**: Swiping the card marks, bookmarks, or downloads all new chapters in that series group.
  - **Download Button**: One-tap downloads all new chapters in the card.
  - **When Expanded**: Individual chapters can still be tapped, swiped, or selected independently.
* **Silky Smooth 120 FPS Scrolling**: Applied Library performance parity to the Updates feed. Collapsed child chapters are omitted from lazy composition, dropping active list items from 1,500 down to ~100 for lag-free scrolling.
* **Feed Preference Toggle**: Easily switch between Consolidated Mode and Classic List anytime in Settings or the Updates filter sheet.

### 1500-Item Truncation Ceiling & Multi-Scanlator Tracker Precision
* **Anti-Truncation Limit**: Raised the SQL update buffer ceiling from 500 to 1,500 across Manga, Anime, and Novels, ensuring large bulk releases never push your older updates off the screen.
* **Tracker Update Precision**: Solved duplicate scanlators and decimal chapters (10.5, extras) by comparing tracker totals against latestChapterNumber (GetChapterSummaryByMangaId).
* **ComicK Chapter Count Sync**: Automatically retrieves total chapters from ComicK metadata (/titles/{hid}) on refresh.

### Bug Fixes & Diagnostics
* **Fixed Startup Crash Loop**: Shielded MainActivity from restoring orphaned extension settings dialogs after process recreation, and implemented SafeEditTextPreferenceDialogFragment to gracefully dismiss without crashing.
* **Zero 24/7 Background Logging**: Restored system logcat *:E and routed all tracker update engine diagnostics through Chimanew's in-memory ChimanewDiagnostics under a toggleable UPDATES category.
* **Instant App Update Check**: Removed the app update check cooldown so GitHub releases are checked immediately on launch.
