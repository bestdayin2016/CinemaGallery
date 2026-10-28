# Handover Document & Prompt for Claude AI

## 📌 Project Overview
- **Project Name**: CinemaGallery (`com.cinemagallery`)
- **Repository**: [github.com/bestdayin2016/CinemaGallery](https://github.com/bestdayin2016/CinemaGallery) (Main branch, latest tag `v1.0.0`)
- **Goal**: Personal-use Android movie and TV series catalogue application built with Jetpack Compose Material 3, Room Database, Retrofit, and Clean Architecture.

---

## 🛠️ Tech Stack & Dependencies
- **Language**: Kotlin `2.0.21`
- **Gradle**: `8.9` | **AGP**: `8.7.3` | **JDK**: Java 17 (`jbr-17`)
- **UI**: Jetpack Compose + Material 3 (Dark Theme)
- **Dependency Injection**: Hilt (Dagger) `2.52`
- **Local Database**: Room `2.6.1`
- **Networking**: Retrofit `2.11.0` + `kotlinx.serialization` `1.7.3` + OkHttp `4.12.0`
- **Image Loading**: Coil Compose `2.7.0`
- **Background Tasks**: WorkManager `2.9.1`
- **Preferences**: DataStore Preferences `1.1.1`
- **Navigation**: Navigation Compose `2.8.3`

---

## 🏗️ Architecture & Module Structure

```text
CinemaGalleryApp/
├── :domain   Pure Kotlin module containing enterprise domain models,
│             repository contracts, use cases, and parser interfaces.
│
├── :data     Android Library module handling SAF folder scanning, regex filename
│             parsing, Room Database entities/DAOs, TMDB API, and Repositories.
│
└── :app      UI application module with ViewModels, Compose screens,
              Navigation graphs, and Hilt DI module bindings.
```

*Strict Rule Enforcement:*
1. `:app` UI and ViewModels depend **exclusively** on `:domain` models and use cases. No imports from `com.cinemagallery.data.*` exist in `:app` UI/ViewModels (only wired in Hilt DI in `DataModule.kt`).
2. **Branding Rule**: The app name (`CinemaGallery`) **must never appear anywhere in the UI** (app bars display `"Library"`, `"Unmatched Files"`, `"Settings"`, or media title names).

---

## 🌟 Core Features & Implementation Details

1. **Storage Access Framework (SAF) Folder Access**:
   - Selected via `ActivityResultContracts.OpenDocumentTree()` in [`SettingsScreen.kt`](file:///C:/Users/BARCELO/Desktop/CinemaGalleryApp/app/src/main/java/com/cinemagallery/ui/settings/SettingsScreen.kt).
   - URI permissions persisted using `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)`.
   - Saved as `FolderEntity` in Room DB.

2. **Native SAF Directory Scanner ([`SafFolderScanner.kt`](file:///C:/Users/BARCELO/Desktop/CinemaGalleryApp/data/src/main/java/com/cinemagallery/data/scanner/SafFolderScanner.kt))**:
   - Executed asynchronously on `Dispatchers.IO` via Kotlin Coroutines `Flow<ScanProgress>`.
   - **3-Tier Traversal Pipeline**:
     - **Tier 1 (Native SAF)**: Uses `DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)` and direct `contentResolver.query()` for fast, native OS directory traversal.
     - **Tier 2 (DocumentFile)**: Fallback using `DocumentFile.fromTreeUri(context, rootUri)`.
     - **Tier 3 (java.io.File)**: Fallback for raw file paths.
   - **Network Timeout Fast-Fail**: If TMDB network times out (e.g. ISP DNS block), the scanner instantly switches remaining files to fast local insertion mode so scanning completes in under 1 second.

3. **Release Filename Parser ([`RegexFileNameParser.kt`](file:///C:/Users/BARCELO/Desktop/CinemaGalleryApp/data/src/main/java/com/cinemagallery/data/parser/RegexFileNameParser.kt))**:
   - Cleans release tags (`1080p`, `2160p`, `x264`, `x265`, `BluRay`, `WEBRip`, `GRP`).
   - Parses season/episode numbers (`S01E02`, `1x02`).

4. **Persisted Manual Override Matching**:
   - Unmatched release files go to **Unmatched Files** screen.
   - Tapping **"Link Fix"** saves the mapping to `manual_overrides` Room table (persisting across future rescans), fetches TMDB poster artwork, inserts `MovieEntity`/`TvShowEntity`/`SeasonEntity`/`EpisodeEntity` into Room DB, and removes the item from Unmatched list.

5. **TMDB Online Metadata Provider ([`TmdbRepositoryImpl.kt`](file:///C:/Users/BARCELO/Desktop/CinemaGalleryApp/data/src/main/java/com/cinemagallery/data/repository/TmdbRepositoryImpl.kt))**:
   - API key loaded from `local.properties` (`tmdb.api.key`).
   - Fetches live poster artwork (`w500`), backdrop images (`w1280`), overviews, ratings, release years, and genre tags.
   - Includes an interactive **TMDB API Tester Card** in Settings to test queries and preview online poster artwork live.

6. **External Video Player Support ([`PlayerUtils.kt`](file:///C:/Users/BARCELO/Desktop/CinemaGalleryApp/app/src/main/java/com/cinemagallery/util/PlayerUtils.kt))**:
   - Triggers `Intent.ACTION_VIEW` for `video/*` via `FileProvider` (opens VLC, MX Player, or default Android video player).

7. **TV Series Depth**:
   - Season tabs, episode lists, episode watched checkboxes, and season watched progress bars.

---

## 🐞 Critical Bug Fixes Already Resolved

1. **Android ICU Regex Exception**: Fixed `PatternSyntaxException` (`U_REGEX_INVALID_CAPTURE_GROUP_NAME`) in `RegexFileNameParser.kt` where duplicate named capture groups across OR branches threw exceptions on Android 10+.
2. **Main Thread Room DB Crash**: Moved all folder scanning and Room DB operations to `Dispatchers.IO` (`.flowOn(Dispatchers.IO)`).
3. **Manual Fix Library Insertion**: Updated `UnmatchedFilesRepositoryImpl.kt` to insert resolved movies/series into Room database when "Link Fix" is clicked.
4. **SAF Permission & Traversal**: Refactored `SafFolderScanner.kt` to strictly pass original `treeUri` and added `READ_MEDIA_VIDEO` permission for Android 13+.

---

## 📋 Copy & Paste Prompt for Claude AI

> "I am handing over the **CinemaGalleryApp** Android project to you.
> 
> **Project Context**:
> - **Repo**: `CinemaGalleryApp` (`com.cinemagallery`).
> - **Stack**: Kotlin 2.0.21, Jetpack Compose Material 3, Hilt 2.52, Room 2.6.1, Retrofit 2.11.0, Coil 2.7.0, WorkManager 2.9.1, DataStore.
> - **Architecture**: Multi-module Clean Architecture (`:domain`, `:data`, `:app`).
> 
> **Key Rules**:
> 1. `:app` UI and ViewModels must depend ONLY on `:domain` models and use cases. Never import `:data` in `:app` UI/ViewModels.
> 2. The app name (`CinemaGallery`) MUST NOT appear anywhere in the user interface.
> 3. `local.properties` contains `tmdb.api.key` and is in `.gitignore` — never commit API keys.
> 4. Always confirm new semantic version numbers (e.g. `v1.0.1`) before creating Git release tags.
> 
> Please read `HANDOVER_PROMPT.md` in the root folder for full details. What would you like to work on next?"
