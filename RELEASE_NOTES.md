# What's New in Chimanew v2.6.65

### MangaDex Cover and Volume Art Fixes
* **POWBlock Verification Bypass**: Added dedicated network interceptor for MangaDex CDN and API endpoints (`*.mangadex.org`), replacing generic browser User-Agent with application identity and attaching appropriate referers. This permanently resolves the issue where MangaDex served HTML browser verification challenges instead of JPEG image data, leaving volume cover cards blank.
* **MangaDex Content-Type Guard**: Intercepts `uploads.mangadex.org` responses to ensure HTML challenge payloads are never misidentified as successful images or cached in image loader disk storage.
* **Volume Label Formatting**: Cleaned up MangaDex cover subtitle labels in the manual artwork picker, converting language codes to uppercase (for example, Vol. 1 JA, Vol. 2 JA) and hiding empty volume identifiers.
* **Full-Resolution MangaDex Cover Application**: When applying a selected MangaDex cover from the artwork picker, the app automatically fetches the original full-resolution image instead of the 512px preview thumbnail, with automatic fallback if necessary.
* **Comprehensive Cache Invalidation**: Extended the Reset Artwork Cache function to fully clear MangaDex memory and identifier caches alongside AniList and image loader caches.
