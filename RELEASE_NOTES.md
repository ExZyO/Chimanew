# What's New in Chimanew v2.6.38

### ⚡ Reader Pre-Decoding & Performance Optimizations
* **Webtoon Background Pre-Decoding & Cache Expansion**: Expanded the webtoon reader view cache to 8 offscreen holders and enabled instant bitmap decoding for cached offscreen panels. Fast scrolling through webtoons and manhwa no longer drops frames, stutters, or flashes blank placeholders.
* **Atomic Download Queue Batching**: Batch-enqueuing chapters across multiple series or library selections now processes in a single atomic update. Eliminates redundant WorkManager triggers, notification spam, and UI state recalculations when downloading full series or updates.
* **Local Source ComicInfo.xml Date Parsing & Preservation**: Local manga chapters now extract Year, Month, and Day directly from embedded `ComicInfo.xml` archives, ensuring local entries sort by actual release date rather than file modification dates. Downloaded chapters automatically write standard ComicInfo date metadata.
