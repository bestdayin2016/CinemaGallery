package com.cinemagallery.domain.usecase

import com.cinemagallery.domain.repository.MediaRepository
import javax.inject.Inject

class ToggleWatchedUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend fun toggleMovie(movieId: String, isWatched: Boolean) {
        mediaRepository.toggleMovieWatched(movieId, isWatched)
    }

    suspend fun toggleEpisode(episodeId: String, isWatched: Boolean) {
        mediaRepository.toggleEpisodeWatched(episodeId, isWatched)
    }

    suspend fun toggleSeason(tvShowId: String, seasonNumber: Int, isWatched: Boolean) {
        mediaRepository.toggleSeasonWatched(tvShowId, seasonNumber, isWatched)
    }
}
