# What's New in Chimanew v2.6.36

### ⚡ Background Updates & Details Page Fix
* **Instant Details Loading During Library Updates**: Fixed an issue where opening a series details page while a library update was running would get stuck spinning or loading indefinitely. Background update events are now smoothly sampled, and chapter processing is decoupled so the details page and chapter list open **instantly**, even during massive 1,000+ chapter library updates.
* **Smart Long-Strip Page Memory Optimization**: Added Mihon's `TallImageSplitCalculator` to avoid unnecessarily recompressing and splitting tall webtoon/manhwa images that already fit comfortably in optimal memory dimensions, improving image fidelity and reader scrolling speed.
* **Instant Library Download Filter**: Eliminated thousands of redundant disk lookups when filtering your library by downloaded entries.
