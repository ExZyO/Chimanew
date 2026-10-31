# What's New in Chimanew v2.6.45

### 🖼️ Cross-Device Custom Cover Synchronization Fix
* **Automatic Cover Downloading on Receiving Device**: When you choose a custom cover or artwork from any source (BookWalker, MangaDex, AniList, etc.) on your phone and sync via Google Drive, your laptop now immediately downloads the chosen cover image directly to its local cover cache on sync.
* **Preserve Custom Covers Over Waterfall Defaults**: Fixed an issue where the laptop's library and details view fell back to default automatic waterfall artwork because local disk files were missing. `hasCustomCover` now recognizes custom cover metadata immediately upon sync.
* **100% Library Sync Scope**: Fixed a sync filtering limitation where entries with null favorite timestamps were omitted from Google Drive sync backups. All library favorites are now guaranteed to be included.
* **Rock-Solid Manga Matching**: Changed composite synchronization keys to strictly match by source ID and manga URL (`source|url`), preventing title or author discrepancies from blocking cover sync.
