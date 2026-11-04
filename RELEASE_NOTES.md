# What's New in Chimanew v2.6.73

### Native Full-Resolution Artwork
* **Lossless MangaDex Covers**: Removed .512.jpg preview downscaling from MangaDex cover URLs, loading native full-resolution original cover art across the application.
* **Original Kitsu Covers**: Kitsu cover repository now serves original full-resolution artwork rather than downscaled large variants.
* **Lossless Artwork Normalization**: The artwork picker automatically strips thumbnail and preview suffixes to persist full-resolution covers.
* **Latest MangaDex Volume Action**: Added a quick action in the Artwork Picker to instantly detect, select, and apply the highest volume number cover available on MangaDex in full resolution.

### Library Grouping Parity
* **Anime Library Grouping**: Added the Group tab to the Anime Library settings dialog, allowing entries to be grouped by Categories, Sources, Status, Tracking Status, or Ungrouped.
* **Novel Library Grouping**: Added the Group tab to the Novel Library settings dialog, enabling grouping by Categories, Sources (Local vs Online Sources), or Ungrouped.

### Update Cooldown Removal
* **Unrestricted Update Checks**: Removed the 15-minute cooldown restriction from LibraryUpdateJob, allowing library updates to execute whenever requested without artificial lockouts.
* **Instant Launch Checks**: Reduced the launch update check debounce to 10 seconds.
