# Chimahon Custom – Upstream Update Guide

This repository ([`ExZyO/chimahon`](https://github.com/ExZyO/chimahon)) is a personalized fork of [Chimahon](https://github.com/Chimahon/chimahon) containing custom features that official Chimahon does not have.

This guide explains how to update this repository whenever official Chimahon releases a new update (e.g. `v2.4.6`, `v2.5.0`, etc.) while keeping all custom features intact.

---

## ⚡ Quick Way: Just Tell the AI

If you are using Antigravity or any AI assistant, you can simply type:
> *"Chimahon just released version vX.Y.Z. Please update our fork to that version, keep all our custom features, and push to main so GitHub Actions builds the APK."*

The AI will automatically read `AGENTS.md` and this file (`UPDATE_GUIDE.md`), perform the rebase, resolve any conflicts, bump the version, and push.

---

## 📋 Our Custom Features (Do NOT Lose These)

Whenever updating, ensure these 3 features are preserved:

| Feature | Files | What it does / Crucial details |
| :--- | :--- | :--- |
| **1. Webtoon Scroll Acceleration** | `app/.../ui/reader/viewer/webtoon/WebtoonRecyclerView.kt`<br>`app/proguard-rules.pro` | Increases maximum fling velocity to 900,000 px/s with up to 3.2x multiplier for fast webtoon flicks.<br>**CRITICAL:** `app/proguard-rules.pro` must contain `-keepclassmembers class androidx.recyclerview.widget.RecyclerView { int mMaxFlingVelocity; }` or R8 will obfuscate the field and clamp fling speed to standard 8,000 px/s in release builds. |
| **2. Manga Reading Time Estimate** | `app/.../presentation/manga/components/MangaReadingTimeEstimate.kt`<br>`app/.../presentation/manga/components/MangaInfoHeader.kt`<br>`app/.../presentation/manga/MangaScreen.kt`<br>`app/.../ui/manga/MangaScreenModel.kt` | Displays estimated reading time badge on manga details screen based on unread chapters (average ~3 min/chapter). |
| **3. Custom Package & Fast Cloud Build** | `app/build.gradle.kts`<br>`chimahon/build.gradle.kts`<br>`.github/workflows/build_release_apk.yml` | • `applicationId = "app.chimahon.custom"` (allows installing alongside official Chimahon without conflicts).<br>• Uses `debug.signingConfig` for release builds so APK can be installed directly.<br>• `-PtargetAbi=arm64-v8a` cuts build time to ~10-12 mins.<br>• GitHub Actions auto-builds and publishes the APK on push to `main`. |

---

## 🔄 Step-by-Step Manual Update Procedure

If you want to update manually or see what happens behind the scenes:

### 1. Fetch the New Upstream Release
Make sure you have both `origin` (your repo) and `upstream` (official repo) configured:
```bash
# Verify remotes
git remote -v
# If upstream is not added:
# git remote add upstream https://github.com/Chimahon/chimahon.git

# Fetch latest tags and commits from official Chimahon
git fetch upstream --tags
```

### 2. Create a Working Branch and Rebase
Suppose the new upstream tag is `v2.4.6`:
```bash
git checkout -b update/v2.4.6
git rebase v2.4.6
```

### 3. Handle Any Conflicts
If Git pauses for conflicts:
1. Open the conflicted files.
2. Keep our custom code:
   - In `WebtoonRecyclerView.kt`: Keep the scroll acceleration multiplier and reflection logic.
   - In `app/proguard-rules.pro`: Keep the `-keepclassmembers class androidx.recyclerview.widget.RecyclerView` rule.
   - In `MangaScreenModel.kt` & `MangaScreen.kt`: Keep `readingTimeChapterCount`.
   - In `app/build.gradle.kts`: Keep `applicationId = "app.chimahon.custom"` and `signingConfigs.getByName("debug")`.
3. Stage resolved files:
   ```bash
   git add <resolved-file>
   git rebase --continue
   ```

### 4. Bump Version Codes
Update the version codes to match the upstream release in:
1. **`app/build.gradle.kts`**:
   ```kotlin
   versionCode = releaseVersionCode ?: 20406  // e.g. for 2.4.6
   versionName = releaseVersionName ?: "2.4.6"
   ```
2. **`.github/workflows/build_release_apk.yml`**:
   ```yaml
   run: ./gradlew assembleRelease -Penable-updater -PtargetAbi=arm64-v8a -PreleaseVersionName=2.4.6 -PreleaseVersionCode=20406
   ```

### 5. Merge into Main and Push
```bash
git checkout main
git merge update/v2.4.6
git push origin main
```

---

## 📲 Where to Get the Updated APK

Once pushed to `main`, GitHub Actions automatically builds the optimized ARM64 release APK:

1. Go to: **[https://github.com/ExZyO/chimahon/actions](https://github.com/ExZyO/chimahon/actions)**
2. Wait ~10–12 minutes for the "Build Custom Release APK" workflow to complete (green checkmark).
3. Download the APK directly from:
   - **[GitHub Releases](https://github.com/ExZyO/chimahon/releases)** (e.g. `app-arm64-v8a-release.apk`)
   - Or from the Artifacts section at the bottom of the workflow run page.
4. Install or update on your Android device — it updates seamlessly over your existing installation.
