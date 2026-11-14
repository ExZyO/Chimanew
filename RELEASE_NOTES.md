# What's New in Chimanew v2.6.75

### Uncapped Home Screen Shelves and Categories
* **Removed Shelf Size Limits**: Completely removed artificial item limits (previously capped at 15 items) across all home shelves, including Recently Updated Manga, Recently Added Anime, and Recently Added Novels. Shelves now show all matching library and history entries.
* **Uncapped Hero Item Selection**: Hero carousel candidate selection now accesses the entire reading history and library pools without 15-item caps.

### Zero-Lag Home Screen Performance Architecture
* **Single-Pass AsyncImage Rendering**: Replaced subcomposition-based image loading in cards with direct single-pass AsyncImage rendering and graceful background error fallbacks. This completely stops UI-thread layout thrashing and dropped frames during fast scrolling.
* **Compose Slot Recycling**: Added explicit contentType specifications and composite unique keys across both vertical shelf columns and horizontal card rows, enabling instant Compose layout slot reuse and buttery smooth 60/120fps scrolling on par with the Library, History, and Updates tabs.

### Continue Reading History Management & Parity Dialog
* **Remove from History Action**: Long-pressing any card in Continue Reading (or any read entry on the home screen) surfaces a Remove option matching the History tab.
* **Parity Removal Dialog**: Features the exact History tab confirmation dialog with options to either remove the read date of the current chapter or reset all chapters for the entry.

### Quick Tracker Access in Long-Press Menu
* **Direct Status and Progress Updates**: Added an Update Tracking shortcut to the card long-press sheet, allowing instant tracker status, score, and progress management without opening the full details screen.
