# What's New in Chimanew v2.6.39

### 🛡️ Smart Artwork Discovery & AniList Rate-Limiting Defense
* **Proactive Gas Gauge & Rate Limiting**: Added a background Token Bucket rate limiter (30 permits/min) and dynamic header monitor for AniList. When remaining requests drop below 15, background queries smoothly pause to protect your account from 429 lockouts while keeping sync and browsing responsive.
* **Persistent Disk Caching with Smart Invalidation**: Discovered artwork IDs and clean title mappings now persist across app restarts in an atomic file. Cached results are reused instantly and only re-evaluated when clearing the cache, updating priority hierarchy, or tapping manual refresh.
* **Granular Source Exclusions**: Added an Exclusions tab in the Artwork Picker dialog with separate multiple-choice controls for Volume Covers/Posters and Landscape Banners. Exclude any provider or lock entries strictly to the default source cover.
* **Offline Memory & Heap Optimization**: Internal artwork metadata is normalized into canonical reference tables, eliminating duplicate allocations and cutting disk cache footprint by over 70%.
* **Updater & Reader Reliability**: Resolved in-app update download triggers across diverse device vendors and fixed reader dimension resolution.
