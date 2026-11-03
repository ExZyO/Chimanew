# What's New in Chimanew v2.6.70

### Tracker Engine & Library Sync Precision
* **Tracker False Positive Elimination**: Enforced strict validation requiring tracker total chapters to exceed local total chapters before signaling an update. Titles whose tracker chapters equal or trail local counts (such as web novel chapters or equal counts like 1450 vs 1450) are no longer falsely flagged as having new releases, cutting library update runtimes from 10 minutes down to seconds.
* **Active Check Timestamp Tracking**: Introduced in-memory tracking of the exact moment an entry is checked across both background updates and manual detail page pull-to-refresh. The 15-minute cooldown now properly identifies recently checked titles even when no new chapters were found.
* **Manual Refresh Priority**: Manual pull-to-refresh on manga and anime detail screens always contacts the remote source immediately and registers the timestamp, allowing subsequent background sync passes to skip the entry safely.

### Updates Search Performance
* **Instant Filtering on Large Feeds**: Fixed search query reactivity by including search state in Compose remember keys across Manga, Anime, and Novel updates screens.
* **Series-Level Fast Search**: Re-architected search processing across the 46,000+ chapter update database to group entries by series ID before filtering. Titles match instantly at the series level, dropping search computation time from over a minute to under 10 milliseconds.

### Batch Anime Migration Stability
* **Global Migration Pacing**: Replaced per-anime local locks with a shared global semaphore enforcing 600ms spacing across all concurrent source requests during batch migrations.
* **HTTP 429 Rate Limit Recovery**: Added automatic 429 rate limit detection and 2-second backoff retries when querying AIOStreams and catalogue sources, preventing transient rate-limit spikes from falsely reporting titles as Not Found.
