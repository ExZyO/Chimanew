# What's New in Chimanew v2.6.79

### Upstream Chimahon v2.5.0 Synchronization

#### High-Performance Native Dictionary Renderer
* **Native Compose Glossary Renderer**: Added an optional, native Jetpack Compose dictionary glossary renderer with a C++ rendering backend, delivering instant term card displays and eliminating WebView loading overhead.
* **Custom Dictionary Names & Ordering**: Correctly applies custom dictionary display names and priority ordering in popup dictionary views.

#### Anime Player & Mining Enhancements
* **Subtitle Font Import**: Support for importing custom fonts for anime subtitles directly within the player.
* **Adjustable Audio Padding**: Fine-grained subtitle audio padding controls for mining sentence audio and flashcard generation.
* **Stream Screenshots & Subtitle Persistence**: Captures screenshots from image-wrapped anime video streams and reliably restores Jimaku subtitles when reopening episodes.
* **Library Filter Access**: Fixed unreachable anime library filter dialogue when all entries in a category are filtered out.

#### Novel Reader & Browse Overhaul
* **Clean Text Copying**: Furigana is now excluded when copying text in the novel reader.
* **Ruby Hit Testing**: Improved character hit-testing precision around ruby annotations and Unicode surrogate pairs.
* **Incognito Mode & Header Integrity**: Fully respects incognito reading in the novel reader and sends standard browser headers when downloading LNReader files.
* **Browse Actions & Search Stability**: Enhanced novel browse library actions, source navigation, and serialized LNReader JavaScript execution across sources.

#### Anki Integration & Reader OCR Fixes
* **Expanded Anki Client Support**: Added package selection for AnkiDroid-compatible applications, including Kuma3 and custom forks.
* **Clean Glossary Exports**: Fixed Anki `{glossary-plain}` and `{glossary-plain-no-dict}` card exports to produce clean definitions without HTML tags or repeated style blocks.
* **Webtoon Seam OCR**: Seamlessly combines OCR character lookup across webtoon page boundaries.
* **Mokuro & Line Rotation**: Draws OCR bounding boxes per line following text rotation, and maps Mokuro OCR correctly by index on downloaded chapters.
