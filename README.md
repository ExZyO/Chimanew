<div align="center">

<img width="180" height="180" src="./app/src/main/res/drawable/chimahon.png" alt="Chimanew Icon" />
<h1 align="center">Chimanew</h1>

**Unified multimedia immersion, reading, and tracking suite for Manga, Anime, Novels, and Webtoons.**

[![GitHub Release](https://img.shields.io/github/v/release/ExZyO/Chimanew?label=Release&labelColor=27303D&color=0D1111&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/ExZyO/Chimanew/releases/latest)
[![GitHub Downloads](https://img.shields.io/github/downloads/ExZyO/Chimanew/total?label=Downloads&labelColor=27303D&color=0D1111&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/ExZyO/Chimanew/releases)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)

</div>

---

Chimanew (EaZy Nuvio+) is a modern, high-performance multimedia platform built on top of the Mihon, Komikku, and Chimahon foundations. It unifies manga reading, novel reading, anime streaming, language immersion tools, external tracker synchronization, and personalized home recommendations into a single, cohesive Android experience.

---

## Highlights

### Unified Multi-Media System
* **Global Media Mode Switcher**: Seamlessly switch between Manga, Anime, and Novel modes with a single tap. The active mode synchronizes across all navigation tabs, including Home, Library, Updates, History, and Browse.
* **Extensive Source Support**: Access thousands of online sources and local libraries across manga, light novels, web novels, EPUBs, and anime video catalogs.

### Modern Home and Discovery
* **Hero Carousel and Banners**: Dynamic backdrop hero carousels with smooth blur transitions, showcasing currently active, trending, and highlighted series.
* **Personalized Recommendations**: Integrated smart recommendation carousels based on tracker libraries, genres, and reading habits.
* **Continue Reading and Watching**: Quick-resume cards allowing immediate continuation from the exact chapter, page, or timestamp.

### Native Artwork and Cover Picker
* **Multi-Source Cover Picker**: Browse and apply official alternate covers from MangaDex, AniList, Kitsu, Bangumi, and BookWalker, or upload custom local artwork.
* **Latest Volume Auto-Detection**: One-tap action to automatically detect, fetch, and apply the highest numbered volume cover from MangaDex.
* **Native Lossless Resolution**: Strips downscaled thumbnails and compression suffixes (.512.jpg, .256.jpg, /large/) to serve full-resolution, uncompressed artwork across the app.

### Intelligent Tracker and Update Engine
* **Precision Hybrid Tracker Signaling**: Integrates external trackers (AniList, MyAnimeList, Kitsu, Shikimori, Bangumi, Simkl, MangaUpdates) with source scrapers.
* **Plausibility Anomaly Window**: Prevents false-positive chapter alerts caused by trackers indexing web novel chapter counts rather than published manga chapters.
* **Cloudflare Fast-Skip and Resolution**: Automatically detects Cloudflare verification challenges during update checks, skipping blocked titles to prevent freezes and notifying the user with direct WebView verification actions.
* **Granular Queue Restrictions**: Respects user preferences to skip unread backlogs, unstarted titles, or completed series during background checks.
* **Unrestricted Manual Updates**: Cleaned of artificial cooldown timers, allowing on-demand updates whenever requested.

### Library Organization Parity
* **Flexible Grouping Across All Media**: Complete grouping parity across Manga, Anime, and Novel libraries. Group items by Categories, Sources (including separate Local vs Online sources), Status, Tracking Status, or Ungrouped.
* **Display Customization**: Customizable per-category layouts including Compact Grid, Comfortable Grid, Cover-Only Grid, and List views, with adjustable column counts for portrait and landscape orientations.

### Immersion, OCR, and Language Learning
* **Integrated Offline Dictionary**: High-performance dictionary lookups supporting Yomitan, Yomichan, and EPWING formats with recursive definitions and pitch accent visualization.
* **On-Device OCR**: Offline text recognition on manga panels and video subtitles without requiring network access.
* **Anki Integration**: Instant card creation from lookups with captured context sentences, screenshots, definitions, and audio sent directly to AnkiDroid.
* **AI Panel Navigation**: Automatic comic panel boundary detection and guided navigation powered by LiteRT YOLO models.

### Video Player and Subtitle Engine
* **MPV Playback Core**: Hardware-accelerated video playback with multi-audio and soft/hard subtitle track switching.
* **Jimaku Subtitle Fetching**: Search, download, and synchronize external subtitle tracks directly within the player.
* **Frame-Accurate OCR**: Capture and look up text from video frames, on-screen signs, and hardcoded subtitles.

---

## Screenshots

<div align="center">

| Novel Reader | Manga Reader & OCR | Dictionary Lookup |
| :---: | :---: | :---: |
| <img src="./.github/readme-images/novel.png" width="280" alt="Novel Reader" /> | <img src="./.github/readme-images/popup-manga.png" width="280" alt="Manga Reader" /> | <img src="./.github/readme-images/dict-tab.png" width="280" alt="Dictionary Tab" /> |

| Anime Player | Library & Browsing |
| :---: | :---: |
| <img src="./.github/readme-images/anime.jpg" width="420" alt="Anime Player" /> | <img src="./.github/readme-images/screens.png" width="420" alt="Library Screens" /> |

</div>

---

## Download

Get the latest stable release from the Releases page:

* **[Download Latest Release](https://github.com/ExZyO/Chimanew/releases/latest)**

*Requires Android 8.0 (API 26) or higher. Recommended for Android 10+.*

---

## Trackers Supported

* AniList
* MyAnimeList
* Kitsu
* Simkl
* Shikimori
* Bangumi
* MangaUpdates

---

## Building from Source

To build Chimanew locally, ensure you have:
* Android Studio Ladybug or newer
* JDK 17 or higher
* Android SDK 35

```bash
# Clone the repository with submodules
git clone --recursive https://github.com/ExZyO/Chimanew.git
cd Chimanew

# Build the release APK
./gradlew assembleStandardRelease
```

---

## Acknowledgments and Upstream Projects

Chimanew is made possible by the open-source community and builds upon these projects:

* [Chimahon](https://github.com/sohilsayed/chimahon): Base immersion reader fork for novels, anime, and dictionaries.
* [Komikku](https://github.com/komikku-app/komikku): Upstream features, tracking, and UI enhancements.
* [Mihon](https://github.com/mihonapp/mihon): Foundational manga reader core.
* [Yomitan](https://github.com/yomidevs/yomitan): Language-processing workflows and dictionary parsing.
* [owocr](https://github.com/AuroraWright/owocr): OCR merging and text reconstruction logic.
* [hoshidicts](https://github.com/Manhhao/hoshidicts): Native dictionary engine.
* [mpv-android](https://github.com/mpv-android/mpv-android): Video playback core.

---

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](./LICENSE) file for details.
