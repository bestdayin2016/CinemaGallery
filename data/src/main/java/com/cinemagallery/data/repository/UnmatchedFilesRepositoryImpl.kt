package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.ManualOverrideDao
import com.cinemagallery.data.local.dao.MovieDao
import com.cinemagallery.data.local.dao.TvShowDao
import com.cinemagallery.data.local.dao.UnmatchedFileDao
import com.cinemagallery.data.local.entity.EpisodeEntity
import com.cinemagallery.data.local.entity.ManualOverrideEntity
import com.cinemagallery.data.local.entity.MovieEntity
import com.cinemagallery.data.local.entity.SeasonEntity
import com.cinemagallery.data.local.entity.TvShowEntity
import com.cinemagallery.domain.model.UnmatchedFile
import com.cinemagallery.domain.model.UnmatchedStatus
import com.cinemagallery.domain.repository.TmdbRepository
import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnmatchedFilesRepositoryImpl @Inject constructor(
    private val unmatchedFileDao: UnmatchedFileDao,
    private val manualOverrideDao: ManualOverrideDao,
    private val movieDao: MovieDao,
    private val tvShowDao: TvShowDao,
    private val tmdbRepository: TmdbRepository
) : UnmatchedFilesRepository {

    override fun getUnmatchedFiles(): Flow<List<UnmatchedFile>> {
        return unmatchedFileDao.getUnmatchedFiles().map { entities ->
            entities.map { entity ->
                UnmatchedFile(
                    id = entity.id,
                    filePath = entity.filePath,
                    fileName = entity.fileName,
                    detectedTitle = entity.detectedTitle,
                    detectedYear = entity.detectedYear,
                    detectedSeason = entity.detectedSeason,
                    detectedEpisode = entity.detectedEpisode,
                    isTvShow = entity.isTvShow,
                    status = try {
                        UnmatchedStatus.valueOf(entity.status)
                    } catch (e: Exception) {
                        UnmatchedStatus.PENDING
                    },
                    matchedTmdbId = entity.matchedTmdbId,
                    manualTitleOverride = entity.manualTitleOverride
                )
            }
        }
    }

    override suspend fun resolveUnmatchedFile(
        fileId: String,
        tmdbId: Int,
        isTvShow: Boolean,
        customTitle: String
    ) {
        val target = unmatchedFileDao.getUnmatchedFileById(fileId) ?: return

        // 1. Save manual override so fixes persist across future folder rescans!
        manualOverrideDao.insertOverride(
            ManualOverrideEntity(
                filePath = target.filePath,
                tmdbId = tmdbId,
                isTvShow = isTvShow,
                customTitle = customTitle
            )
        )

        // 2. Mark unmatched status as RESOLVED
        unmatchedFileDao.updateResolution(fileId, "RESOLVED", tmdbId, customTitle)

        // 3. Insert real movie or TV show into Room Database!
        if (isTvShow) {
            val searchResult = tmdbRepository.searchTvShows(customTitle)
            val match = searchResult.getOrNull()?.find { it.tmdbId == tmdbId } ?: searchResult.getOrNull()?.firstOrNull()

            val tvShowId = "tv_$tmdbId"
            val seasonNum = target.detectedSeason ?: 1
            val episodeNum = target.detectedEpisode ?: 1
            val seasonId = "season_${tmdbId}_$seasonNum"

            val tvShowEntity = TvShowEntity(
                id = tvShowId,
                tmdbId = tmdbId,
                title = customTitle,
                year = match?.year ?: target.detectedYear,
                posterPath = match?.posterPath,
                backdropPath = match?.backdropPath,
                overview = match?.overview?.ifBlank { "Manually matched series from local storage." } ?: "Manually matched series from local storage.",
                rating = match?.rating ?: 8.0,
                genresCsv = match?.genres?.joinToString(",") ?: "Series",
                folderPath = target.filePath.substringBeforeLast('/'),
                seasonsCount = seasonNum,
                totalEpisodesCount = 1,
                watchedEpisodesCount = 0
            )
            tvShowDao.insertTvShows(listOf(tvShowEntity))

            val seasonEntity = SeasonEntity(
                id = seasonId,
                tvShowId = tvShowId,
                seasonNumber = seasonNum,
                title = "Season $seasonNum",
                posterPath = match?.posterPath,
                overview = "Season $seasonNum",
                totalEpisodesCount = 1,
                watchedEpisodesCount = 0
            )
            tvShowDao.insertSeasons(listOf(seasonEntity))

            val episodeEntity = EpisodeEntity(
                id = "ep_${tmdbId}_s${seasonNum}_e${episodeNum}",
                seasonId = seasonId,
                tvShowId = tvShowId,
                seasonNumber = seasonNum,
                episodeNumber = episodeNum,
                title = "Episode $episodeNum",
                overview = target.fileName,
                stillPath = match?.backdropPath,
                filePath = target.filePath,
                fileSizeFormatted = "Local File",
                runtimeMinutes = 45,
                isWatched = false
            )
            tvShowDao.insertEpisodes(listOf(episodeEntity))
        } else {
            val searchResult = tmdbRepository.searchMovies(customTitle)
            val match = searchResult.getOrNull()?.find { it.tmdbId == tmdbId } ?: searchResult.getOrNull()?.firstOrNull()

            val movieEntity = MovieEntity(
                id = "movie_$tmdbId",
                tmdbId = tmdbId,
                title = customTitle,
                year = match?.year ?: target.detectedYear,
                posterPath = match?.posterPath,
                backdropPath = match?.backdropPath,
                overview = match?.overview?.ifBlank { "Local video file: ${target.fileName}" } ?: "Local video file: ${target.fileName}",
                rating = match?.rating ?: 8.0,
                genresCsv = match?.genres?.joinToString(",") ?: "Movie",
                runtimeMinutes = match?.runtimeMinutes ?: 120,
                filePath = target.filePath,
                fileSizeFormatted = "Local File",
                isWatched = false
            )
            movieDao.insertMovies(listOf(movieEntity))
        }
    }

    override suspend fun ignoreUnmatchedFile(fileId: String) {
        unmatchedFileDao.ignoreFile(fileId)
    }
}
