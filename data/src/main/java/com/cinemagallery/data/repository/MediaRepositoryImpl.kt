package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.MovieDao
import com.cinemagallery.data.local.dao.TvShowDao
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.domain.model.Episode
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.Season
import com.cinemagallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao
) : MediaRepository {

    // In-memory mock reactive state for prototype flexibility
    private val moviesState = MutableStateFlow(MockDataSupplier.sampleMovies)
    private val tvShowsState = MutableStateFlow(MockDataSupplier.sampleTvShows)
    private val seasonsState = MutableStateFlow(MockDataSupplier.sampleSeasons)

    override fun getMovies(): Flow<List<MediaItem.Movie>> = moviesState.asStateFlow()

    override fun getMovieById(id: String): Flow<MediaItem.Movie?> {
        return moviesState.map { list -> list.find { it.id == id } }
    }

    override fun getTvShows(): Flow<List<MediaItem.TvShow>> = tvShowsState.asStateFlow()

    override fun getTvShowById(id: String): Flow<MediaItem.TvShow?> {
        return tvShowsState.map { list -> list.find { it.id == id } }
    }

    override fun getSeasonsForShow(tvShowId: String): Flow<List<Season>> {
        return seasonsState.map { list -> list.filter { it.tvShowId == tvShowId } }
    }

    override fun getEpisodesForSeason(
        tvShowId: String,
        seasonNumber: Int
    ): Flow<List<Episode>> {
        return seasonsState.map { list ->
            list.find { it.tvShowId == tvShowId && it.seasonNumber == seasonNumber }?.episodes.orEmpty()
        }
    }

    override suspend fun toggleMovieWatched(movieId: String, isWatched: Boolean) {
        moviesState.value = moviesState.value.map { movie ->
            if (movie.id == movieId) movie.copy(isWatched = isWatched) else movie
        }
        movieDao.updateWatchedStatus(movieId, isWatched)
    }

    override suspend fun toggleEpisodeWatched(episodeId: String, isWatched: Boolean) {
        seasonsState.value = seasonsState.value.map { season ->
            val updatedEpisodes = season.episodes.map { ep ->
                if (ep.id == episodeId) ep.copy(isWatched = isWatched) else ep
            }
            val watchedCount = updatedEpisodes.count { it.isWatched }
            season.copy(episodes = updatedEpisodes, watchedEpisodesCount = watchedCount)
        }
        tvShowDao.updateEpisodeWatchedStatus(episodeId, isWatched)
    }

    override suspend fun toggleSeasonWatched(
        tvShowId: String,
        seasonNumber: Int,
        isWatched: Boolean
    ) {
        seasonsState.value = seasonsState.value.map { season ->
            if (season.tvShowId == tvShowId && season.seasonNumber == seasonNumber) {
                val updatedEpisodes = season.episodes.map { ep -> ep.copy(isWatched = isWatched) }
                val watchedCount = if (isWatched) updatedEpisodes.size else 0
                season.copy(episodes = updatedEpisodes, watchedEpisodesCount = watchedCount)
            } else season
        }
        tvShowDao.updateSeasonWatchedStatus(tvShowId, seasonNumber, isWatched)
    }
}
