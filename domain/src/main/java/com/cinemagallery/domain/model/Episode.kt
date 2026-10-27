package com.cinemagallery.domain.model

data class Episode(
    val id: String,
    val seasonId: String,
    val tvShowId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val stillPath: String?,
    val filePath: String,
    val fileSizeFormatted: String,
    val runtimeMinutes: Int,
    val isWatched: Boolean,
    val lastPlayedPositionMs: Long = 0L
)
