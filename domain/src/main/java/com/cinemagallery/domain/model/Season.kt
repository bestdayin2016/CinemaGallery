package com.cinemagallery.domain.model

data class Season(
    val id: String,
    val tvShowId: String,
    val seasonNumber: Int,
    val title: String,
    val posterPath: String?,
    val overview: String,
    val totalEpisodesCount: Int,
    val watchedEpisodesCount: Int,
    val episodes: List<Episode> = emptyList()
) {
    val isFullyWatched: Boolean
        get() = totalEpisodesCount > 0 && watchedEpisodesCount == totalEpisodesCount
}
