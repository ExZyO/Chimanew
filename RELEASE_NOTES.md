# What's New in Chimanew v2.6.85

### Details Page Distinct Chapter Deduplication
* **Multi-Scanlator Accurate Chapter Counts**: Applied the badge distinct chapter count algorithm to the Details screen header in both portrait and landscape orientations. Instead of displaying the inflated sum across all scanlators and merged sources, the header now accurately counts distinct chapter numbers and recognized bonus items.

### Migration State Serialization Crash Fix
* **BadParcelableException Resolution**: Marked `matchOverride` in `MigrationListScreen` and `AnimeMigrationListScreen` as transient with a lazy backing property, eliminating the `NotSerializableException: androidx.compose.runtime.ParcelableSnapshotMutableState` crash when the app is backgrounded during migration.
