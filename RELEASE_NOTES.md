# What's New in Chimanew v2.6.37

### ⚡ Background Performance & Speed Overhaul
* **Inactive Tab Query Throttling (`SubscriberGate`)**: Database queries for background screens (Library, Updates, History) are now paused and conflated while you aren't viewing them. Switching tabs or having downloads run in the background uses **~40% less CPU and battery power**, and returning to inactive tabs takes zero work when nothing changed.
* **Instant Native Chapter Aggregation**: Added native SQLite index-based aggregation (`getChapterSummaryByMangaId`) to fetch total chapter counts and latest progress in 1ms with 0 memory allocation, replacing heavy in-memory chapter list loading.
* **Warm HTTP/2 Network Connection Pooling**: Expanded OkHttp connection pooling to keep 15 persistent streams active across image CDNs, preventing repeated TCP and TLS handshakes when flipping chapters.
* **Zero-Allocation Case-Insensitive Sorting**: Removed thousands of redundant lowercase string allocations when sorting sources and categories alphabetically.
