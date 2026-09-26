# Chimahon (Custom) – AI Agent Guide & Upstream Update Instructions

This repository is a customized fork of **Chimahon** (`https://github.com/Chimahon/chimahon`) hosted at `https://github.com/ExZyO/chimahon`.

---

## ⚠️ Mandatory Rules for Any AI Agent Working Here

1. **Package Name**:
   - ALWAYS preserve `applicationId = "app.chimahon.custom"` in `app/build.gradle.kts`.
   - Never revert to `app.chimahon` (this allows side-by-side installation with official Chimahon).
2. **Custom Features**:
   - NEVER drop or overwrite the custom features during rebases, merges, or updates:
     1. **Webtoon Scroll Acceleration** (`WebtoonRecyclerView.kt` + `app/proguard-rules.pro`)
     2. **Manga Reading Time Estimate** (`MangaReadingTimeEstimate.kt`, `MangaInfoHeader.kt`, `MangaScreen.kt`, `MangaScreenModel.kt`)
     3. **Release Signing & ABI optimization** (`app/build.gradle.kts`, `chimahon/build.gradle.kts`, `.github/workflows/build_release_apk.yml`)
3. **Branching & Pushing**:
   - When updating to a new upstream version, do the rebase on a branch first (`update/vX.Y.Z`), verify, then fast-forward `main` and push to `origin main`.
   - Do NOT push to `upstream`. Only push to `origin` (`ExZyO/chimahon`).

---

## 🛠️ Ported Custom Features Reference

### 1. Webtoon Scroll Acceleration
* **Files**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonRecyclerView.kt`
  - `app/proguard-rules.pro`
* **Implementation Details**:
  - Max velocity: `WEBTOON_MAX_FLING_VELOCITY = 900000`
  - Speed zones: `6500f` (1.1x), `10000f` (1.5x), `16000f` (3.2x)
  - `applyWebtoonMaxFlingVelocity()` sets reflection on `RecyclerView.mMaxFlingVelocity` and includes a dynamic fallback scanning for `ViewConfiguration.get(context).scaledMaximumFlingVelocity` in case of obfuscation.
  - `app/proguard-rules.pro`:
    ```proguard
    -keepclassmembers class androidx.recyclerview.widget.RecyclerView {
        int mMaxFlingVelocity;
    }
    ```
    *CRITICAL*: Without this ProGuard rule, R8 in `assembleRelease` renames `mMaxFlingVelocity`, causing `RecyclerView.fling` to clamp all velocities down to Android's default 8,000 px/s!

### 2. Manga Reading Time Estimate
* **Files**:
  - `app/src/main/java/eu/kanade/presentation/manga/components/MangaReadingTimeEstimate.kt` (formats reading time using average 182,800 ms per chapter)
  - `app/src/main/java/eu/kanade/presentation/manga/components/MangaInfoHeader.kt` (displays `ChromeReaderMode` icon + estimate text)
  - `app/src/main/java/eu/kanade/presentation/manga/MangaScreen.kt` (passes `chapterCount = state.readingTimeChapterCount` to `MangaInfoBox` in small & tablet layouts)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaScreenModel.kt` (calculates and subscribes to `readingTimeChapterCount`, deduplicating chapters by number/name)

### 3. Build & Release Automation
* **Files**:
  - `app/build.gradle.kts`:
    - `applicationId = "app.chimahon.custom"`
    - `signingConfig = debug.signingConfig` in `release` build type (enables direct installation of R8-optimized release APKs without custom keystore secrets).
    - `targetAbi` property in `splits.abi`
  - `chimahon/build.gradle.kts`:
    - `targetAbi` property in `ndk.abiFilters`
  - `.github/workflows/build_release_apk.yml`:
    - Automatically builds release APK on GitHub Actions runner with `./gradlew assembleRelease -Penable-updater -PtargetAbi=arm64-v8a -PreleaseVersionName=... -PreleaseVersionCode=...`
    - Uploads `app-arm64-v8a-release.apk` artifact and creates a GitHub Release for direct download.

---

## 🔄 Procedure to Update to New Upstream Releases

When upstream (`Chimahon/chimahon`) releases a new update (e.g. tag `v2.4.6`):

### Step 1: Fetch Upstream
```bash
git fetch upstream --tags
```

### Step 2: Create Update Branch and Rebase
```bash
git checkout -b update/v2.4.6
git rebase v2.4.6
```

### Step 3: Resolve Conflicts (if any)
If conflicts occur during rebase:
- Keep the `WebtoonRecyclerView.kt` fling multiplier logic and ProGuard rules.
- Keep `readingTimeChapterCount` in `MangaScreenModel.kt` and `MangaScreen.kt`.
- Keep `applicationId = "app.chimahon.custom"` in `app/build.gradle.kts`.
- Finish rebase: `git rebase --continue`.

### Step 4: Update Version Information
In `app/build.gradle.kts`:
```kotlin
versionCode = releaseVersionCode ?: <NEW_VERSION_CODE> # e.g. 20406
versionName = releaseVersionName ?: "<NEW_VERSION_NAME>" # e.g. "2.4.6"
```
In `.github/workflows/build_release_apk.yml`:
```yaml
run: ./gradlew assembleRelease -Penable-updater -PtargetAbi=arm64-v8a -PreleaseVersionName=<NEW_VERSION_NAME> -PreleaseVersionCode=<NEW_VERSION_CODE>
```

### Step 5: Merge into Main and Push
```bash
git checkout main
git merge update/v2.4.6
git push origin main
```

### Step 6: Download the New APK
GitHub Actions will automatically build and publish the release APK in ~10-15 minutes at:
`https://github.com/ExZyO/chimahon/releases`
