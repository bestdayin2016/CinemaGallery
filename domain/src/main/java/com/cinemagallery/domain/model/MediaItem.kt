package com.cinemagallery.domain.model

sealed interface MediaItem {
    val id: String
    val tmdbId: Int
    val title: String
    val year: Int?
    val posterPath: String?
    val backdropPath: String?
    val overview: String
    val rating: Double
    val genres: List<String>

    data class Movie(
        override val id: String,
        override val tmdbId: Int,
        override val title: String,
        override val year: Int?,
        override val posterPath: String?,
        override val backdropPath: String?,
        override val overview: String,
        override val rating: Double,
        override val genres: List<String>,
        val runtimeMinutes: Int,
        val filePath: String,
        val fileSizeFormatted: String,
        val isWatched: Boolean,
        val lastPlayedPositionMs: Long = 0L
    ) : MediaItem

    data class TvShow(
        override val id: String,
        override val tmdbId: Int,
        override val title: String,
        override val year: Int?,
        override val posterPath: String?,
        override val backdropPath: String?,
        override val overview: String,
        override val rating: Double,
        override val genres: List<String>,
        val folderPath: String,
        val seasonsCount: Int,
        val totalEpisodesCount: Int,
        val watchedEpisodesCount: Int
    ) : MediaItem {
        val isFullyWatched: Boolean
            get() = totalEpisodesCount > 0 && watchedEpisodesCount == totalEpisodesCount
    }
}
