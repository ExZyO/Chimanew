# What's New in Chimanew v2.6.62

### Global Floating Dock Inset Fix
* **Unobstructed Bottom Content**: Added a global 96dp dock padding provider to Tachiyomi's core Scaffold architecture and Novel Updates. The last items in Updates, Browse (Sources, Extensions, Migration, Feed), History, and More now scroll completely clear of the floating dynamic liquid glass navbar dock.

### 10,000-Item Updates Buffer Ceiling
* **Multi-Scanlator Truncation Prevention**: Increased the updates SQL query buffer from 1,500 to 10,000 across Manga, Anime, and Novels. When titles have multiple scanlation groups contributing hundreds of individual chapter entries, older series updates are preserved and never pushed off your feed.

### Tracker Update Engine Precision & Rate-Limit Shield
* **Strict Completed Series Enforcement**: Completed titles are never bypassed or repeatedly queried when the "Only update ongoing" restriction is active, preventing redundant network queries for finished series.
* **Scanlator Inflation Protection**: Refined chapter comparison logic to evaluate true latest chapter numbers alongside local chapter counts, eliminating false-positive updates on titles with multiple scanlators.
* **Rate-Limit & Timeout Shielding**: Restricted pre-flight online probes to un-cached titles, capped candidate queries at 5 items with concurrency control and pacing delays, fully eliminating AniList HTTP 429 rate-limit errors and MyAnimeList network timeouts.
