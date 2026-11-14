# What's New in Chimanew v2.6.78

### Media-Adaptive Progress Terminology
* **Dynamic Media Type Formatting**: The Nuvio Hero section now dynamically evaluates `item.mediaType` to format progress indicators and chapter/episode tags. Anime entries are displayed cleanly with `"Ep."` and `"Episodes"` (e.g., "Ep. 12 / 24" and "24 Episodes") rather than showing confusing reading terminology like "12 Chapters". Manga and novels continue to display `"Ch."` and `"Chapters"`.

### Defensive String Sanitization
* **Clean API Data Filtering**: Implemented strict defensive string parsing across cover repositories (Kitsu and AniList). Raw strings containing `"null"`, `"N/A"`, or empty values returned by upstream JSON responses are cleanly rejected, eliminating broken UI subtitle fragments, badges, and metadata labels.

### Architectural Performance & Navigation Hardening
* **Nested Scroll Measurement Protections**: Standardized vertical scrolling container boundaries to avoid unconstrained height measurement exceptions during high-velocity scrolling.
* **Hierarchical Navigation & Master Optimization Index**: Preserved backstack integrity across multi-hop media navigation, and formalized the comprehensive master performance index in `OPTIMIZATIONS.md`.
