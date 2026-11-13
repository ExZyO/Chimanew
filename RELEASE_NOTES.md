# What's New in Chimanew v2.6.74

### Distinct Chapter Badge Deduplication
* **Multi-Scanlator Deduplication**: Unread chapter badges now compute over distinct chapter numbers rather than raw database rows. Reading a chapter from one scanlator automatically marks that chapter number as read across all scanlators, eliminating inflated ghost badge counts.
* **Volume and Chapter Alignment**: Volume containers and overlapping chapter entries are deduplicated against distinct chapter numbers, resolving inflated unread counts on mixed-release series.

### Enhanced Tracker Update Signaling
* **Backlog Restriction Bypass**: Tracker signals for newly released chapters now bypass user restrictions for unread backlogs (manga_fully_read) and unstarted series (manga_started), ensuring series with confirmed new chapters are checked and updated immediately.
* **Uncapped Tracker Signal Sensitivity**: Removed the 25-chapter plausibility restriction ceiling, allowing any positive tracker chapter increase to trigger priority update signals.

### Reader Engine Performance & Memory Optimization
* **Velocity-Based Lookahead Prefetching**: Dynamic extra layout lookahead in WebtoonViewer automatically scales up to 3x based on scroll velocity, pre-rendering upcoming vertical panels during high-speed flings and eliminating white loading gaps.
* **16-Bit RGB_565 & Hardware Bitmaps**: Configured 16-bit RGB_565 fallback and hardware bitmap decoding for webtoon pages, cutting GPU and heap memory footprint by 50% during long reading sessions.
