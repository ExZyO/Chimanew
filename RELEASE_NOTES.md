# What's New in Chimanew v2.6.72

### Precision Tracker Signaling
* **Plausibility Window (1 to 25 Chapters)**: Added a plausibility check for tracker signals. Disproportionately massive chapter counts (such as AniList or MAL tracking hundreds of web novel chapters for titles like Eleceed, Spare Me Great Lord, or Chainsaw Man Colored) are no longer treated as false-positive manga chapter signals.
* **Strict Restriction Enforcement**: Tracker signals no longer bypass user-configured restrictions for unread entries or unstarted series. When "Entries with unread chapters" is active, titles with backlogs are strictly skipped as configured, keeping the update queue lean and fast.
* **Strict Cooldown Respect**: Automatic checks strictly enforce the 15-minute cooldown for all entries regardless of tracker signals, preventing rapid re-checks when titles have already been scraped.
