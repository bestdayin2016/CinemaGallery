package com.cinemagallery.domain.repository

import com.cinemagallery.domain.model.Episode
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.Season
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMovies(): Flow<List<MediaItem.Movie>>
    fun getMovieById(id: String): Flow<MediaItem.Movie?>
    fun getTvShows(): Flow<List<MediaItem.TvShow>>
    fun getTvShowById(id: String): Flow<MediaItem.TvShow?>
    fun getSeasonsForShow(tvShowId: String): Flow<List<Season>>
    fun getEpisodesForSeason(tvShowId: String, seasonNumber: Int): Flow<List<Episode>>
    suspend fun toggleMovieWatched(movieId: String, isWatched: Boolean)
    suspend fun toggleEpisodeWatched(episodeId: String, isWatched: Boolean)
    suspend fun toggleSeasonWatched(tvShowId: String, seasonNumber: Int, isWatched: Boolean)
}
