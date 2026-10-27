package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.MovieDao
import com.cinemagallery.data.local.dao.TvShowDao
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.domain.model.Episode
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.Season
import com.cinemagallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao
) : MediaRepository {

    override fun getMovies(): Flow<List<MediaItem.Movie>> {
        return movieDao.getAllMovies().map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleMovies
            } else {
                entities.map { entity ->
                    MediaItem.Movie(
                        id = entity.id,
                        tmdbId = entity.tmdbId,
                        title = entity.title,
                        year = entity.year,
                        posterPath = entity.posterPath,
                        backdropPath = entity.backdropPath,
                        overview = entity.overview,
                        rating = entity.rating,
                        genres = entity.genresCsv.split(',').map { it.trim() }.filter { it.isNotBlank() },
                        runtimeMinutes = entity.runtimeMinutes,
                        filePath = entity.filePath,
                        fileSizeFormatted = entity.fileSizeFormatted,
                        isWatched = entity.isWatched,
                        lastPlayedPositionMs = entity.lastPlayedPositionMs
                    )
                }
            }
        }
    }

    override fun getMovieById(id: String): Flow<MediaItem.Movie?> {
        return getMovies().map { list -> list.find { it.id == id } }
    }

    override fun getTvShows(): Flow<List<MediaItem.TvShow>> {
        return tvShowDao.getAllTvShows().map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleTvShows
            } else {
                entities.map { entity ->
                    MediaItem.TvShow(
                        id = entity.id,
                        tmdbId = entity.tmdbId,
                        title = entity.title,
                        year = entity.year,
                        posterPath = entity.posterPath,
                        backdropPath = entity.backdropPath,
                        overview = entity.overview,
                        rating = entity.rating,
                        genres = entity.genresCsv.split(',').map { it.trim() }.filter { it.isNotBlank() },
                        folderPath = entity.folderPath,
                        seasonsCount = entity.seasonsCount,
                        totalEpisodesCount = entity.totalEpisodesCount,
                        watchedEpisodesCount = entity.watchedEpisodesCount
                    )
                }
            }
        }
    }

    override fun getTvShowById(id: String): Flow<MediaItem.TvShow?> {
        return getTvShows().map { list -> list.find { it.id == id } }
    }

    override fun getSeasonsForShow(tvShowId: String): Flow<List<Season>> {
        return tvShowDao.getSeasonsForShow(tvShowId).map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleSeasons.filter { it.tvShowId == tvShowId }
            } else {
                entities.map { entity ->
                    Season(
                        id = entity.id,
                        tvShowId = entity.tvShowId,
                        seasonNumber = entity.seasonNumber,
                        title = entity.title,
                        posterPath = entity.posterPath,
                        overview = entity.overview,
                        totalEpisodesCount = entity.totalEpisodesCount,
                        watchedEpisodesCount = entity.watchedEpisodesCount
                    )
                }
            }
        }
    }

    override fun getEpisodesForSeason(
        tvShowId: String,
        seasonNumber: Int
    ): Flow<List<Episode>> {
        return tvShowDao.getEpisodesForSeason(tvShowId, seasonNumber).map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleSeasons
                    .find { it.tvShowId == tvShowId && it.seasonNumber == seasonNumber }
                    ?.episodes.orEmpty()
            } else {
                entities.map { entity ->
                    Episode(
                        id = entity.id,
                        seasonId = entity.seasonId,
                        tvShowId = entity.tvShowId,
                        seasonNumber = entity.seasonNumber,
                        episodeNumber = entity.episodeNumber,
                        title = entity.title,
                        overview = entity.overview,
                        stillPath = entity.stillPath,
                        filePath = entity.filePath,
                        fileSizeFormatted = entity.fileSizeFormatted,
                        runtimeMinutes = entity.runtimeMinutes,
                        isWatched = entity.isWatched,
                        lastPlayedPositionMs = entity.lastPlayedPositionMs
                    )
                }
            }
        }
    }

    override suspend fun toggleMovieWatched(movieId: String, isWatched: Boolean) {
        movieDao.updateWatchedStatus(movieId, isWatched)
    }

    override suspend fun toggleEpisodeWatched(episodeId: String, isWatched: Boolean) {
        tvShowDao.updateEpisodeWatchedStatus(episodeId, isWatched)
    }

    override suspend fun toggleSeasonWatched(
        tvShowId: String,
        seasonNumber: Int,
        isWatched: Boolean
    ) {
        tvShowDao.updateSeasonWatchedStatus(tvShowId, seasonNumber, isWatched)
    }
}
