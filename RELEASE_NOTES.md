# What's New in Chimanew v2.6.46

### ⚡ True Lightweight Incremental Sync & Zero-Lag Restores
* **Smart Content-First Difference Detection**: Sync now compares actual user state (read chapters, last read pages, bookmarks, categories, and custom artwork) instead of internal database version counters that drift whenever entries are viewed or refreshed.
* **Restore Only Changed Entries**: If you only changed 1 cover or read 1 chapter, sync now restores only that 1 entry instead of re-restoring your entire 300+ library from scratch. If nothing changed, sync completes immediately in milliseconds without running a background restore.
* **Eliminate Database Lock Lag**: Because unchanged entries are no longer hammered with database updates, opening manga detail pages during or after sync is completely smooth and instantaneous.
