# What's New in Chimanew v2.6.52

### 🖼️ High-Reliability MangaDex Cover Resolution
* **Fixed Missing & Empty MangaDex Covers**: Fixed an issue where MangaDex covers frequently appeared blank or failed to load in the cover picker even after pressing refresh.
* **Smart Candidate Ranking & Doujinshi Filtering**: MangaDex search results now accurately prioritize official series over fan-made doujinshis, spinoffs, and fan-cuts by ranking candidates with intelligent title matching and metadata scoring.
* **Tracker ID Cross-Referencing**: When entries are linked to AniList, MyAnimeList, or MangaDex/MDList, MangaDex covers are matched directly by remote ID for 100% precision.
* **MangaDex Source & Direct UUID Detection**: Series read from MangaDex or tracked on MDList now fetch covers directly by their exact MangaDex UUID rather than relying solely on fuzzy title searching.
* **Expanded Content Ratings & Limits**: MangaDex search now queries across safe, suggestive, and erotica ratings with an expanded candidate pool (`limit=15`) and embedded primary covers, ensuring titles are never silently dropped.
* **Intelligent Title Fallbacks & Cleaner Token Matching**: Improved token matching to retain essential short words (e.g. *Pick Me Up*, *Re:Zero*, *Oshi no Ko*, *Kaiju No. 8*) and automatically fall back to alternative titles if the primary title returns no covers.
* **250ms Request Throttling & 4s Backoff**: Introduced an internal request pacer (maximum 4 requests/sec) to prevent hitting MangaDex's 5 req/s rate limit, and reduced 429 lockout from 60 seconds to a transient 4-second backoff that clears instantly on manual refresh.
