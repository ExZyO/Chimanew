# What's New in Chimanew v2.6.67

### CI Build & Compiler Fixes
* **Resolved Screen and Sheet Symbol Imports**: Added missing graphics, utility, and dependency-injection imports across AnimeScreen, NovelDetailScreen, AnimeArtworkPickerBottomSheet, and NovelArtworkPickerBottomSheet.
* **Artwork Repository Fallback Resolution**: Corrected cover fallback reference to defaultCoverUrl when resolving custom banner values in ArtworkWaterfallRepository.

### Hero & Detail Banner System
* **Instant In-Place Hero Banner Updates**: Updating, picking, or clearing a banner now reflects instantly in the Home Hero carousel without re-shuffling or re-rolling active hero entries.
* **Independent Hero and Detail Banners**: Added dedicated preferences and quick actions ("Apply to Details", "Apply to Hero") allowing Hero carousel banners and entry detail page backdrops to be customized separately.
* **Custom Banner File Picker**: Added ability to pick and apply custom banner image files directly from local device storage across Manga, Anime, and Light Novels.
* **Disable Details Banner**: Added option to completely disable top backdrop banners on entry detail pages, cleanly collapsing background imagery without falling back to stretched or blurred covers.
* **Use Cover as Banner**: Added quick action in the Artwork Picker banners tab to set the entry's cover as the landscape backdrop.
* **Restored Hub Settings**: Restored missing "Backdrop Artwork Source", "Allow Cover Fallback for Details Banner", "Allow Cropped Fallback in Hero", and "Strict Artwork Requirements" options under the Nuvio Settings Hub.

### MangaDex Cover and Volume Art
* **Bypass Anti-Bot Verification**: Intercepts `*.mangadex.org` traffic with native application User-Agent to prevent HTML challenges from breaking volume cover loading.
