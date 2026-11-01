# What's New in Chimanew v2.6.55

### ⚡ Category Artwork Rules V2: Multi-Source Priority, Granular Fallbacks & Performance Boost
* **Multi-Source Priority per Category**: You can now select and prioritize multiple art sources for both covers and banners independently (e.g. MangaDex → AniList for covers, and AniList → Bangumi for banners). Easily add sources via quick chips and arrange their sequence with one-tap arrow controls.
* **Granular Cover & Banner Fallback Controls**: Separately determine what happens if none of your selected sources have artwork:
  * **Strict Default**: Immediately use the default source cover or keep the banner blank, skipping all other online searches.
  * **Global Art Cascade (Waterfall)**: Fall back to cascading through your global art priority list.
* **Natural Global Inheritance**: Unconfigured categories now naturally and seamlessly inherit your Global Art Sources & Priority settings without requiring duplicate setup.
* **Smooth Category Drag & Drop**: Fixed an index offset issue that prevented dragging categories in the priority list, ensuring fluid and reliable conflict resolution when manga belong to multiple categories.
* **Crash Resolution**: Fixed a `BadParcelableException` (`NotSerializableException`) that could trigger when leaving or pausing the Category Artwork Rules screen.
* **Optimized Network & In-Memory Performance**:
  * Decoded category rule configurations are now cached in memory with atomic timestamp invalidation, reducing CPU overhead to near-zero.
  * Network discovery intelligently filters outgoing requests to only query providers configured in the category rule and its fallback, eliminating unnecessary network calls, saving mobile data, and speeding up load times.
