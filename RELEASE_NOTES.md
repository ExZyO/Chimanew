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
