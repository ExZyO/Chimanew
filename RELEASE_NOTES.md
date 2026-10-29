# Chimanew Release Notes

Welcome to the latest release of **Chimanew**! Here is a summary of all optimizations, visual improvements, and performance upgrades in plain English:

---

### ⚡ Background Updates & Details Page Fix (v2.6.36)
* **Instant Details Loading During Library Updates**: Fixed an issue where opening a series details page while a library update was running would get stuck spinning or loading indefinitely. Background update events are now smoothly sampled, and chapter processing is decoupled so the details page and chapter list open **instantly**, even during massive 1,000+ chapter library updates.
* **Smart Long-Strip Page Memory Optimization**: Added Mihon's `TallImageSplitCalculator` to avoid unnecessarily recompressing and splitting tall webtoon/manhwa images that already fit comfortably in optimal memory dimensions, improving image fidelity and reader scrolling speed.
* **Instant Library Download Filter**: Eliminated thousands of redundant disk lookups when filtering your library by downloaded entries.

---

### 🛠️ UI Fixes & Navigation Polish (v2.6.35)
* **Library Add Button Position**: Fixed the floating `+` button in your Manga and Novel libraries being partially hidden behind the floating bottom dock. It is now elevated comfortably above the navigation bar so you can always tap it effortlessly.
* **Full Scrolling in 'More' Tab**: Added extra bottom scroll padding so the *"Star me on GitHub"* button and bottom options are completely visible and never trapped behind the bottom navigation dock.
* **Home Hero Tracker Button**: The tracker button on the Home screen's featured Hero banner now opens the full native tracking menu (the exact same bottom sheet from the series details page) where you can update your AniList, MyAnimeList, Kitsu, and MangaUpdates reading progress with one tap.

---

### 🚀 Speed & Reader Performance Overhaul
* **Parallel Rolling-Chunk Loading**: The reader no longer downloads pages one-by-one in a slow line. It now pulls rolling batches of upcoming pages simultaneously so your reading is never interrupted by loading spinners.
* **Predictive Next-Chapter Buffering**: When you reach the last few pages of a chapter, the app quietly begins loading the next chapter in the background so page 1 displays the second you swipe.
* **Mobile Data & Wi-Fi Speed Parity**: Removed cellular speed constraints so mobile data preloads and streams pages at 100% full speed, identical to Wi-Fi.
* **Lockless Chapter Cache**: Replaced the synchronized disk lock with direct, non-blocking file storage so parallel page downloads never wait in line behind each other.

---

### 🛡️ System Stability & Background Optimization
* **Physical Database Index on Merged Titles**: Added permanent SQLite indexes on merged manga (`merged(manga_id)`), eliminating temporary in-memory index rebuilding, drastically speeding up library updates and keeping your phone cooler.
* **Cloudflare Queue Deduplication**: When parallel requests hit Cloudflare anti-bot security at the same time, only one verification prompt opens per domain; all other pages wait quietly and load smoothly once solved.
* **Adaptive Update Scheduling Fix**: Fixed a scheduling bug where series on hiatus or completed series were being checked too frequently, saving battery and data for actively releasing titles.
* **Manhwa & Webtoon Number Recognition**: Upgraded title parsing so names like `Episode 45`, `Chap 45`, or `S2 Ep. 12` are recognized accurately instead of accidentally grabbing release years (like `2024`) as the chapter number.
* **Flow Invariant Crash Fix**: Fixed a background thread crash that happened when updating large libraries while actively using the app.

---

### 🎨 Netflix / Nuvio Streaming Experience
* **Ambient Hero Carousel**: Added the Netflix-style top banner featuring your in-progress manga with glowing ambient color backgrounds and a 1-tap **"Resume"** button.
* **Hero Banner Stabilization**: Fixed the top carousel banner violently refreshing every second during background updates by adding a smooth debounce buffer.
* **Instant Home Tab Loading**: Eliminated the 2-to-4-second blank gray screen when tapping the Home tab. Shelves and banners now open **instantly with 0ms delay** thanks to memory state caching.
* **Horizontal Shelves**: Added fluid horizontal shelves (*Continue Reading*, *Recently Updated*, and your custom categories) with bouncy spring scrolling.
* **Modern Media Cards**: Replaced clunky black boxes with sleek artwork cards featuring subtle gradients, ultra-thin 2px progress bars, and clean `+N` unread badges.
* **Human-Friendly Settings**: Removed all confusing engineering jargon (like *"Tier 4 28dp blur"* or *"specular sheen"*) and replaced them with plain-language visual descriptions of what you actually see on your screen.
