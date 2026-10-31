# What's New in Chimanew v2.6.42

### ⚡ Instant Cover Application & Zero-Lag Switching
* **Instant Sub-Millisecond Cover Switching**: Selecting a cover or backdrop in the Artwork Picker now instantly clones the image directly from Coil's local disk cache. This eliminates redundant network downloads and network lag completely when picking already previewed artwork.
* **Non-Blocking Artwork Application**: Removed background source re-scraping when applying custom artwork so you are returned to your manga details page instantly without freezing or waiting.

### 🖼️ Selected Artwork in Full-Screen Cover Dialog
* **Accurate Full-Screen Cover Viewer**: Tapping the cover on the details page now displays your selected high-resolution artwork (AniList/Kitsu/MAL or custom cover) rather than falling back to the low-resolution source thumbnail.
* **Accurate Saving & Sharing**: Saving or sharing the cover from the full-screen dialog now exports the exact high-resolution cover you are viewing.

### 🏠 Home Page & Hero Section Synchronization
* **Instant Home Refresh on Cover Changes**: Updating or resetting custom covers and banners now immediately invalidates the home feed cache, updating the Hero carousel and all home shelves without needing to restart the app.
* **Custom Artwork Priority in Hero & Cards**: The Hero carousel and shelf cards now correctly prioritize your custom covers and custom banners above online metadata providers.
* **Consistent Source Exclusion on Home**: Manga source exclusions configured in the Artwork Picker are now seamlessly respected on the Home page feed.
