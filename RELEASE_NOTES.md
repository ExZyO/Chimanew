# What's New in Chimanew v2.6.77

### Accurate Chapter Update Counts & Deduplication
* **Distinct Chapter & Episode Calculation**: Library updates now deduplicate multi-scanlator chapters and translation uploads by chapter number and standardized title. Prevents inflated counts (such as +600 chapters when the series has 120) across update notifications, library badge counters, and background job logs.
* **Smart Notification Summaries**: Fixed notification descriptions to correctly reflect distinct new chapters and avoid erroneous "and X more" counts caused by duplicate scanlator entries.

### Resilient Library Update Queue & Cancellation Memory
* **Instant Cancellation Recovery**: When a library update is cancelled or interrupted, re-running update now remembers recently verified entries (checked within 15 minutes) and updates only the remaining backlog instead of re-fetching the entire library.
* **Tracker Signal Priority**: Entries flagged by external trackers with new chapter releases bypass grace periods to guarantee immediate updates.

### Google Drive Sync OAuth2 & Token Refresh Overhaul
* **Modern OAuth2 Consent Flow**: Migrated authorization requests from the deprecated `approval_prompt=force` to Google's standard `prompt=consent select_account` to ensure reliable offline refresh tokens.
* **Token Preservation & Loop Prevention**: Prevented null refresh tokens from overwriting active credentials during re-authorization. On invalid or revoked grants (HTTP 400), stale tokens are cleanly reset with clear, human-readable guidance rather than infinite refresh failure loops.

### Cloudflare Interception & Immediate Heads-Up Alerts
* **Expanded Verification Interception**: Broadened Cloudflare challenge detection in network interceptors and update jobs to capture 403/503 anti-bot challenges across sources like Mangadot even when server headers are sanitized or omitted.
* **Immediate High-Priority Notifications & In-App Alerts**: Upgraded Cloudflare verification alerts to a dedicated high-importance channel with heads-up notifications, a "Verify in WebView" quick-action button, and foreground toast alerts so challenges can be completed immediately.

### High-Contrast Unread Badges
* **High-Contrast Theme Badges**: Unread badges on manga cards now render with high-contrast theme coloring, an elevation drop shadow, and a subtle crisp outline, ensuring full visibility and readability on light, pastel, or saturated manhwa covers without blending.
