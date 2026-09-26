# Chimahon Custom – Agent Instructions for Upstream Updates

This rule applies whenever an agent is working in this repository or asked to update the app.

## Mandatory Fork Constraints
1. **Application ID**: Must always remain `app.chimahon.custom` in `app/build.gradle.kts`. Never revert to `app.chimahon`.
2. **Webtoon Scroll Acceleration**:
   - `WebtoonRecyclerView.kt` must retain the max fling velocity (900,000 px/s) and tiered multiplier logic (`6500f`, `10000f`, `16000f`).
   - `app/proguard-rules.pro` MUST retain:
     ```proguard
     -keepclassmembers class androidx.recyclerview.widget.RecyclerView {
         int mMaxFlingVelocity;
     }
     ```
     Without this, release builds will clamp fling velocity to standard 8,000 px/s.
3. **Manga Reading Time Estimate**:
   - Keep `MangaReadingTimeEstimate.kt` and references in `MangaInfoHeader.kt`, `MangaScreen.kt`, `MangaScreenModel.kt`.
4. **Cloud Release Builds**:
   - `.github/workflows/build_release_apk.yml` handles automated arm64 release builds.
   - Pushing to `main` automatically triggers build and publishes APK to GitHub Releases.

## Upstream Update Procedure
1. `git fetch upstream --tags`
2. `git checkout -b update/<tag>`
3. `git rebase <tag>`
4. Resolve conflicts preserving custom logic in the 4 features listed above.
5. Update `versionCode` and `versionName` in `app/build.gradle.kts` and `.github/workflows/build_release_apk.yml`.
6. Fast-forward merge to `main` and `git push origin main`.
