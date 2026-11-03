# What's New in Chimanew v2.6.69

### CI & Compiler Fixes
* **Resolved State Property Delegates**: Added missing `setValue` import in UpdatesTab for search query state delegation and switched to `tachiyomi.presentation.core.util.collectAsState` in NovelInfoHeader.
* **Atomic Property Access**: Updated progress counter access to use `AtomicInt.load()` in LibraryUpdateJob.
* **Smart-Cast Safety**: Ensured delegated custom banner properties are bound locally prior to null checks in NovelInfoHeader and AnimeInfoHeader.

### Visual Parity: Anime & Novel Nuvio Hero Mode
* **Modern Centered Layout**: Brought the official Nuvio Hero Mode 0 design to both Anime and Novel detail pages, featuring centered portrait cover styling, elevated drop shadow, rounded corners, and clean typography.
* **Widescreen Backdrops & 7-Stop Silk Dissolve**: Anime and Novel headers now render custom widescreen banners with the signature 7-stop silk bottom gradient dissolve into the background.
* **Banner Toggle Integrity**: Respects the "Allow Cover Fallback for Details Banner" setting across Manga, Anime, and Novels, ensuring clean backdrops without unwanted cover blurring or stretching when banners are disabled or unavailable.

### Updates Feed Search & Deduplication
* **Working Updates Search**: Fixed SearchToolbar query state binding so typing immediately filters recent updates across Manga, Anime, and Light Novels.
* **Entry-Level Deduplication**: Updates feed now groups entries by their unique entry ID anchored to their newest chapter update date. All updates for an entry are consolidated into a single card at the top date, eliminating fragmented duplicate cards between 2025 and today.

### Library Sync & Performance Optimization
* **Fast-Skip Cooldown (< 15m)**: Entries successfully updated within the last 15 minutes are skipped in milliseconds during library updates unless a new chapter is signaled by a tracker or initiated via targeted pull-to-refresh.
* **Real-Time Progress & Diagnostics**: Transparent logging now reports every entry being checked, new chapter counts, and skip reasons in real time.
* **Database Contention Reduction**: Lowered concurrent source workers from 5 to 3 and added coroutine yields between database updates, preventing SQLite write lock starvation when opening entry detail pages during background sync.

### Multi-Season Anime Migration (AIOStream)
* **Direct Season Selection on Tap**: Tapping a multi-season mismatched entry in the Anime Migration list now directly opens the Season Select screen, enabling immediate season choice instead of showing a blocking error.
* **Migration Rate-Limiting Delay**: Added rate-limit delays to prevent AIOStream API flooding and fast failure timeouts.
