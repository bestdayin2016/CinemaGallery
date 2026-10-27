package com.cinemagallery.data.repository

import com.cinemagallery.data.BuildConfig
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.data.remote.api.TmdbApiService
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.repository.TmdbRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepositoryImpl @Inject constructor(
    private val apiService: TmdbApiService
) : TmdbRepository {

    override suspend fun searchMovies(query: String, year: Int?): Result<List<MediaItem.Movie>> {
        val apiKey = BuildConfig.TMDB_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY_HERE") {
            // Fallback to mock search results if no key provided
            val matches = MockDataSupplier.sampleMovies.filter {
                it.title.contains(query, ignoreCase = true)
            }
            return Result.success(matches)
        }

        return try {
            val response = apiService.searchMovies(apiKey = apiKey, query = query, year = year)
            val mapped = response.results.map { dto ->
                MediaItem.Movie(
                    id = "tmdb_${dto.id}",
                    tmdbId = dto.id,
                    title = dto.title,
                    year = dto.releaseDate?.take(4)?.toIntOrNull(),
                    posterPath = dto.posterPath?.let { "${BuildConfig.TMDB_IMAGE_BASE_URL}$it" },
                    backdropPath = dto.backdropPath?.let { "${BuildConfig.TMDB_BACKDROP_BASE_URL}$it" },
                    overview = dto.overview,
                    rating = dto.voteAverage,
                    genres = listOf("Movie"),
                    runtimeMinutes = 120,
                    filePath = "",
                    fileSizeFormatted = "",
                    isWatched = false
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchTvShows(query: String, year: Int?): Result<List<MediaItem.TvShow>> {
        val apiKey = BuildConfig.TMDB_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_TMDB_API_KEY_HERE") {
            val matches = MockDataSupplier.sampleTvShows.filter {
                it.title.contains(query, ignoreCase = true)
            }
            return Result.success(matches)
        }

        return try {
            val response = apiService.searchTvShows(apiKey = apiKey, query = query, year = year)
            val mapped = response.results.map { dto ->
                MediaItem.TvShow(
                    id = "tmdb_tv_${dto.id}",
                    tmdbId = dto.id,
                    title = dto.name,
                    year = dto.firstAirDate?.take(4)?.toIntOrNull(),
                    posterPath = dto.posterPath?.let { "${BuildConfig.TMDB_IMAGE_BASE_URL}$it" },
                    backdropPath = dto.backdropPath?.let { "${BuildConfig.TMDB_BACKDROP_BASE_URL}$it" },
                    overview = dto.overview,
                    rating = dto.voteAverage,
                    genres = listOf("TV Show"),
                    folderPath = "",
                    seasonsCount = 1,
                    totalEpisodesCount = 10,
                    watchedEpisodesCount = 0
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getApiKeyStatus(): Boolean {
        val apiKey = BuildConfig.TMDB_API_KEY
        return apiKey.isNotBlank() && apiKey != "YOUR_TMDB_API_KEY_HERE"
    }
}
