package com.cinemagallery.data.remote.api

import com.cinemagallery.data.remote.model.TmdbMovieDto
import com.cinemagallery.data.remote.model.TmdbSearchResponse
import com.cinemagallery.data.remote.model.TmdbTvShowDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("year") year: Int? = null,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse<TmdbMovieDto>

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("first_air_date_year") year: Int? = null,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse<TmdbTvShowDto>
}
