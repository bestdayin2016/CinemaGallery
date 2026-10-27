package com.cinemagallery.data.mock

import com.cinemagallery.domain.model.Episode
import com.cinemagallery.domain.model.LibraryFolder
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.Season
import com.cinemagallery.domain.model.UnmatchedFile
import com.cinemagallery.domain.model.UnmatchedStatus

object MockDataSupplier {

    val sampleMovies = listOf(
        MediaItem.Movie(
            id = "m1",
            tmdbId = 157336,
            title = "Interstellar",
            year = 2014,
            posterPath = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fKSuS33R7.jpg",
            overview = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
            rating = 8.4,
            genres = listOf("Sci-Fi", "Drama", "Adventure"),
            runtimeMinutes = 169,
            filePath = "/storage/emulated/0/Movies/Interstellar.2014.1080p.BluRay.x264.mkv",
            fileSizeFormatted = "3.2 GB",
            isWatched = true
        ),
        MediaItem.Movie(
            id = "m2",
            tmdbId = 27205,
            title = "Inception",
            year = 2010,
            posterPath = "https://image.tmdb.org/t/p/w500/oYuLE131MB33yB3i8299S922vB.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8P3Ajw3S1S1RSo33R7.jpg",
            overview = "Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets is offered a chance to regain his old life as payment for a task considered to be impossible.",
            rating = 8.3,
            genres = listOf("Action", "Sci-Fi", "Thriller"),
            runtimeMinutes = 148,
            filePath = "/storage/emulated/0/Movies/Inception.2010.1080p.mkv",
            fileSizeFormatted = "2.8 GB",
            isWatched = false
        ),
        MediaItem.Movie(
            id = "m3",
            tmdbId = 155,
            title = "The Dark Knight",
            year = 2008,
            posterPath = "https://image.tmdb.org/t/p/w500/qJ2tW6WM2323iP12338.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/hkBa133R7n6k3M8.jpg",
            overview = "Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.",
            rating = 8.5,
            genres = listOf("Action", "Crime", "Drama"),
            runtimeMinutes = 152,
            filePath = "/storage/emulated/0/Movies/The.Dark.Knight.2008.1080p.mkv",
            fileSizeFormatted = "3.5 GB",
            isWatched = true
        ),
        MediaItem.Movie(
            id = "m4",
            tmdbId = 299536,
            title = "Avengers: Infinity War",
            year = 2018,
            posterPath = "https://image.tmdb.org/t/p/w500/7WsyChLLEz3313R7.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/bOG2343R7.jpg",
            overview = "As the Avengers and their allies have continued to protect the world from threats too large for any one hero to handle, a new danger has emerged from the cosmic shadows: Thanos.",
            rating = 8.2,
            genres = listOf("Action", "Adventure", "Sci-Fi"),
            runtimeMinutes = 149,
            filePath = "/storage/emulated/0/Movies/Avengers.Infinity.War.2018.2160p.mkv",
            fileSizeFormatted = "6.1 GB",
            isWatched = false
        )
    )

    val sampleTvShows = listOf(
        MediaItem.TvShow(
            id = "tv1",
            tmdbId = 1396,
            title = "Breaking Bad",
            year = 2008,
            posterPath = "https://image.tmdb.org/t/p/w500/ztC783313R7.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/tsRy3313R7.jpg",
            overview = "Walter White, a New Mexico chemistry teacher, is diagnosed with Stage III cancer and given a prognosis of two years left to live. He chooses to enter a dangerous world of drugs and crime.",
            rating = 8.9,
            genres = listOf("Drama", "Crime", "Thriller"),
            folderPath = "/storage/emulated/0/TV/Breaking Bad",
            seasonsCount = 5,
            totalEpisodesCount = 62,
            watchedEpisodesCount = 45
        ),
        MediaItem.TvShow(
            id = "tv2",
            tmdbId = 66732,
            title = "Stranger Things",
            year = 2016,
            posterPath = "https://image.tmdb.org/t/p/w500/49WJ3313R7.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/563313R7.jpg",
            overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
            rating = 8.6,
            genres = listOf("Sci-Fi", "Horror", "Mystery"),
            folderPath = "/storage/emulated/0/TV/Stranger Things",
            seasonsCount = 4,
            totalEpisodesCount = 34,
            watchedEpisodesCount = 34
        )
    )

    val sampleSeasons = listOf(
        Season(
            id = "s1",
            tvShowId = "tv1",
            seasonNumber = 1,
            title = "Season 1",
            posterPath = "https://image.tmdb.org/t/p/w500/ztC783313R7.jpg",
            overview = "High school chemistry teacher Walter White's life is changed when he is diagnosed with stage III lung cancer.",
            totalEpisodesCount = 7,
            watchedEpisodesCount = 7,
            episodes = listOf(
                Episode("e1", "s1", "tv1", 1, 1, "Pilot", "Walter White, a 50-year-old high school chemistry teacher, is diagnosed with terminal lung cancer.", null, "/storage/emulated/0/TV/Breaking Bad/S01E01.mkv", "450 MB", 58, true),
                Episode("e2", "s1", "tv1", 1, 2, "Cat's in the Bag...", "Walt and Jesse attempt to dispose of two bodies in the RV.", null, "/storage/emulated/0/TV/Breaking Bad/S01E02.mkv", "420 MB", 48, true),
                Episode("e3", "s1", "tv1", 1, 3, "...And the Bag's in the River", "Walt is left to decide whether or not to kill Krazy-8.", null, "/storage/emulated/0/TV/Breaking Bad/S01E03.mkv", "430 MB", 48, true),
                Episode("e4", "s1", "tv1", 1, 4, "Cancer Man", "Walt tells the rest of his family about his cancer diagnosis.", null, "/storage/emulated/0/TV/Breaking Bad/S01E04.mkv", "410 MB", 48, true),
                Episode("e5", "s1", "tv1", 1, 5, "Gray Matter", "Walt and Skyler attend a party thrown by Walt's former business partner.", null, "/storage/emulated/0/TV/Breaking Bad/S01E05.mkv", "440 MB", 48, true),
                Episode("e6", "s1", "tv1", 1, 6, "Crazy Handful of Nothin'", "Walt decides to shave his head as he begins chemotherapy.", null, "/storage/emulated/0/TV/Breaking Bad/S01E06.mkv", "450 MB", 48, true),
                Episode("e7", "s1", "tv1", 1, 7, "A No-Rough-Stuff-Type Deal", "Walt and Jesse face logistical problems when trying to produce larger quantities of meth.", null, "/storage/emulated/0/TV/Breaking Bad/S01E07.mkv", "460 MB", 48, true)
            )
        ),
        Season(
            id = "s2",
            tvShowId = "tv1",
            seasonNumber = 2,
            title = "Season 2",
            posterPath = "https://image.tmdb.org/t/p/w500/ztC783313R7.jpg",
            overview = "Walt and Jesse face intense escalation as Tuco Salamanca becomes unpredictable.",
            totalEpisodesCount = 13,
            watchedEpisodesCount = 10,
            episodes = listOf(
                Episode("e8", "s2", "tv1", 2, 1, "Seven Thirty-Seven", "Walt and Jesse realize how unhinged Tuco is.", null, "/storage/emulated/0/TV/Breaking Bad/S02E01.mkv", "480 MB", 47, true),
                Episode("e9", "s2", "tv1", 2, 2, "Grilled", "Walt and Jesse are held captive by Tuco.", null, "/storage/emulated/0/TV/Breaking Bad/S02E02.mkv", "490 MB", 47, true),
                Episode("e10", "s2", "tv1", 2, 3, "Bit by a Dead Bee", "Walt and Jesse try to cover their tracks after Tuco's death.", null, "/storage/emulated/0/TV/Breaking Bad/S02E03.mkv", "470 MB", 47, false)
            )
        )
    )

    val sampleUnmatchedFiles = listOf(
        UnmatchedFile(
            id = "uf1",
            filePath = "/storage/emulated/0/Movies/Unsorted/SciFi.Project.X.2023.PROPER.1080p.mkv",
            fileName = "SciFi.Project.X.2023.PROPER.1080p.mkv",
            detectedTitle = "SciFi Project X",
            detectedYear = 2023,
            detectedSeason = null,
            detectedEpisode = null,
            isTvShow = false,
            status = UnmatchedStatus.PENDING
        ),
        UnmatchedFile(
            id = "uf2",
            filePath = "/storage/emulated/0/Movies/Unsorted/Unknown.Indie.Film.2021.720p.mkv",
            fileName = "Unknown.Indie.Film.2021.720p.mkv",
            detectedTitle = "Unknown Indie Film",
            detectedYear = 2021,
            detectedSeason = null,
            detectedEpisode = null,
            isTvShow = false,
            status = UnmatchedStatus.PENDING
        )
    )

    val sampleFolders = listOf(
        LibraryFolder(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3AMovies",
            displayName = "Movies (Internal Storage)",
            totalFilesFound = 4,
            lastScannedTimeFormatted = "Today at 10:15 AM"
        ),
        LibraryFolder(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3ATV",
            displayName = "TV Shows (Internal Storage)",
            totalFilesFound = 96,
            lastScannedTimeFormatted = "Today at 10:16 AM"
        )
    )
}
