
# CinemaGallery

A personal-use Android movie and TV series catalogue application built with modern **Android Architecture Components**, **Jetpack Compose**, and **Clean Architecture**.



<img width="1774" height="887" alt="CinemaGallery README Branding Sheet" src="https://github.com/user-attachments/assets/63c0ac70-01fc-430f-ae4d-f0e2a73343c7" />


---

## Features

- **Local Folder Access (SAF)**: Select local directories containing movies and TV shows using Android's Storage Access Framework (`DocumentFile`) with persisted tree permissions.
- **Robust Release Filename Parser**: Automatically extracts clean titles, release years, and episode tags (`S01E02` / `1x02`) from messy video release filenames (e.g. `Movie.2010.1080p.BluRay.x264-GRP.mkv`).
- **Persisted Manual Override Matching**: Unmatched release files go to a dedicated **Unmatched Files** screen. Manual title/TMDB fixes are saved in a local database and **persist across future folder rescans**.
- **TMDB Online Metadata & Artwork**: Fetches poster art, backdrop images, overview plots, genre tags, and ratings online via free TMDB API.
- **External Video Player Integration**: Play movies or episodes directly in external video players (VLC, MX Player, or default Android player) via `FileProvider` intents.
- **TV Series Depth**: Full tracking with season tabs, episode lists, watched toggles per episode/season, and season watched progress bars.
- **Full Offline Caching**: All metadata, seasons, and artwork paths are cached locally in a Room database for seamless offline operation.
- **Strict UI Branding Rule**: The app name never appears anywhere in the user interface.

---

## Tech Stack & Architecture

- **Language**: Kotlin `2.0.21`
- **UI Toolkit**: Jetpack Compose with Material 3 Dark Theme
- **Dependency Injection**: Hilt (Dagger) `2.52`
- **Database**: Room `2.6.1`
- **Networking**: Retrofit `2.11.0` + `kotlinx.serialization` `1.7.3`
- **Image Loading**: Coil Compose `2.7.0`
- **Background Tasks**: WorkManager `2.9.1`
- **Preferences**: DataStore Preferences `1.1.1`
- **Navigation**: Navigation Compose `2.8.3`

### Clean Architecture Modules

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

*Note: `:app` never imports from `:data` except in the Hilt DI wiring package.*

---

## Project Structure

```text
app/src/main/java/com/cinemagallery/
├── ui/
│   ├── components/      # Reusable MediaCard, ExternalPlayerButton
│   ├── library/         # Library Screen & ViewModel
│   ├── details/         # Movie Details & TV Show Details Screens
│   ├── unmatched/       # Unmatched Files & Manual Fix Overlay Screen
│   ├── settings/        # SAF Folder Picker & Rescan Screen
│   ├── navigation/       # Main Navigation & Bottom Bar
│   └── theme/          # Material 3 Dark Theme & Palette
├── util/                # External Player Intent Helpers (FileProvider)
└── CinemaGalleryApplication.kt

data/src/main/java/com/cinemagallery/data/
├── local/               # Room DB, Entities (Movie, TvShow, Season, Episode, Override, Folder) & DAOs
├── parser/              # RegexFileNameParser Implementation
├── scanner/             # SafFolderScanner (SAF DocumentFile directory traversal)
├── remote/              # Retrofit TMDB API Service & Serialized DTOs
├── repository/          # Repository Implementations (Room + Offline Caching)
└── worker/              # WorkManager Background Rescan Worker

domain/src/main/java/com/cinemagallery/domain/
├── model/               # Domain Models (MediaItem, Season, Episode, UnmatchedFile)
├── repository/          # Repository Contracts
├── parser/              # FileNameParser Contract
└── usecase/             # Business Use Cases (GetLibraryMedia, ToggleWatched, ResolveUnmatched)
```

---

## Getting Started & Configuration

### Prerequisites
- **Android Studio**: Jellyfish / Ladybug or newer
- **JDK**: Java 17 (Embedded JDK in Android Studio)
- **Minimum SDK**: Android 8.0 (API level 26)
- **Target SDK**: Android 15 (API level 35)

### Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/bestdayin2016/CinemaGallery.git
   cd CinemaGallery
   ```

2. **Configure TMDB API Key** (Optional):
   Open `local.properties` in the project root and add your free TMDB API key:
   ```properties
   tmdb.api.key=YOUR_TMDB_API_KEY_HERE
   ```
   *Note: `local.properties` is listed in `.gitignore` and is never committed.*

3. **Build & Run**:
   - Open the project in **Android Studio**.
   - Ensure **Gradle JDK** is set to `JDK 17` (**Settings** ➔ **Build, Execution, Deployment** ➔ **Build Tools** ➔ **Gradle**).
   - Click **File** ➔ **Sync Project with Gradle Files**.
   - Press the green **Run ▶** button (`Shift + F10`).

---

## Usage Guide

1. **Pick Library Folders**: Navigate to **Settings** ➔ tap **Add Media Folder** to launch Android's Storage Access Framework picker and grant folder access.
2. **Scan Folders**: Tap **Rescan Folders Now** in Settings to traverse video files and fetch online metadata.
3. **Browse Media**: Filter by *Movies*, *TV Shows*, or *Unwatched Only* in the **Library** tab.
4. **Manual Fixes**: If a messy filename fails auto-matching, navigate to **Unmatched**, tap **Manual Fix & Persist**, search TMDB, and select the correct match. Fixes persist across all future rescans.
5. **Playback**: Tap **Play in External Player** on any movie or episode to launch VLC, MX Player, or your preferred Android video player.

---

## License

Personal use application.
