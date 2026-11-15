# What's New in Chimanew v2.6.81

### Persistent Library Update Queue Resumption & Grace Window
* **15-Minute Cancellation Recovery**: Fixed library update queue re-evaluation when updates are cancelled and restarted. Verified titles are now persisted to both memory and atomic disk preferences (`library_recent_checks` and `anime_library_recent_checks`), surviving cancellations, background task restarts, and process reloads.
* **Tracker-Assisted Parity**: Removed the exception that exempted tracker-assisted titles from the recent-check grace period. Prevents hundreds of tracker-linked titles from being redundantly re-queried immediately after a check, ensuring true resumption from where the queue left off.
* **Automatic Expiration Pruning**: Automatically prunes check records older than 1 hour upon starting a library update, maintaining a lightweight preference footprint.
