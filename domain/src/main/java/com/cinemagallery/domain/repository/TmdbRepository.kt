package com.cinemagallery.domain.repository

import com.cinemagallery.domain.model.MediaItem

interface TmdbRepository {
    suspend fun searchMovies(query: String, year: Int? = null): Result<List<MediaItem.Movie>>
    suspend fun searchTvShows(query: String, year: Int? = null): Result<List<MediaItem.TvShow>>
    suspend fun getApiKeyStatus(): Boolean
}
