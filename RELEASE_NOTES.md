# What's New in Chimanew v2.6.47

### 🖼️ Permanent Cross-Device Cover Sync & On-Demand Fallback
* **Timestamp-Based Artwork Reconciliation**: Fixed an issue in the Google Drive merge service that was dropping custom artwork if the receiving device already had any cover set or a higher database version counter. Now, whichever device selected or updated the cover most recently always wins.
* **Coil On-Demand Cover Fetcher**: If a custom cover image hasn't finished downloading in the background yet, the image loader directly downloads it from the synced URL on-demand and saves it straight to the disk cache.
* **Dynamic Coil Memory Cache Keys**: Integrated the custom cover URL into the Coil image cache key so changing or syncing a cover immediately updates the image on your home screen and library without being blocked by stale memory cache.
* **Lightweight Incremental Restores**: Only changed entries are synced and written, eliminating background database lock lag.
