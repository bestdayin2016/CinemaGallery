package com.cinemagallery.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.cinemagallery.data.local.CinemaDatabase
import com.cinemagallery.data.local.entity.EpisodeEntity
import com.cinemagallery.data.local.entity.MovieEntity
import com.cinemagallery.data.local.entity.SeasonEntity
import com.cinemagallery.data.local.entity.TvShowEntity
import com.cinemagallery.data.local.entity.UnmatchedFileEntity
import com.cinemagallery.domain.model.ParsedFileName
import com.cinemagallery.domain.model.ScanProgress
import com.cinemagallery.domain.parser.FileNameParser
import com.cinemagallery.domain.repository.TmdbRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedVideoFile(
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val parsedInfo: ParsedFileName
)

@Singleton
class SafFolderScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CinemaDatabase,
    private val parser: FileNameParser,
    private val tmdbRepository: TmdbRepository
) {

    private val supportedVideoExtensions = setOf(
        "mkv", "mp4", "avi", "m4v", "webm", "mov", "wmv", "flv", "ts"
    )

    fun scanFolder(uriString: String): Flow<ScanProgress> = flow {
        val rootUri = Uri.parse(uriString)
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)

        if (rootDoc == null || !rootDoc.exists() || !rootDoc.isDirectory) {
            emit(ScanProgress(isScanning = false, currentFile = "Folder unavailable or permission revoked"))
            return@flow
        }

        emit(ScanProgress(isScanning = true, currentFile = "Discovering files...", filesProcessed = 0, totalFiles = 0))

        val videoFiles = mutableListOf<ScannedVideoFile>()
        traverseDirectory(rootDoc, videoFiles)

        val total = videoFiles.size
        emit(ScanProgress(isScanning = true, currentFile = "Found $total video files", filesProcessed = 0, totalFiles = total))

        for ((index, file) in videoFiles.withIndex()) {
            emit(
                ScanProgress(
                    isScanning = true,
                    currentFile = file.fileName,
                    filesProcessed = index + 1,
                    totalFiles = total
                )
            )

            // 1. Check for persisted manual override first!
            val manualOverride = db.manualOverrideDao().getOverrideForFile(file.filePath)

            if (manualOverride != null) {
                // User manual fix persisted!
                if (manualOverride.isTvShow) {
                    insertOrUpdateTvShow(file, manualOverride.tmdbId, manualOverride.customTitle)
                } else {
                    insertOrUpdateMovie(file, manualOverride.tmdbId, manualOverride.customTitle)
                }
                continue
            }

            // 2. Automatic regex parsing
            val parsed = file.parsedInfo
            val searchTitle = parsed.cleanTitle

            if (parsed.isTvShow) {
                val searchResult = tmdbRepository.searchTvShows(searchTitle, parsed.year)
                val matches = searchResult.getOrNull().orEmpty()

                if (matches.isNotEmpty()) {
                    val bestMatch = matches.first()
                    insertOrUpdateTvShowEpisode(file, bestMatch.tmdbId, bestMatch.title, parsed)
                } else {
                    // Unmatched
                    db.unmatchedFileDao().insertUnmatchedFiles(
                        listOf(
                            UnmatchedFileEntity(
                                id = UUID.randomUUID().toString(),
                                filePath = file.filePath,
                                fileName = file.fileName,
                                detectedTitle = parsed.cleanTitle,
                                detectedYear = parsed.year,
                                detectedSeason = parsed.seasonNumber,
                                detectedEpisode = parsed.episodeNumber,
                                isTvShow = true,
                                status = "PENDING"
                            )
                        )
                    )
                }
            } else {
                val searchResult = tmdbRepository.searchMovies(searchTitle, parsed.year)
                val matches = searchResult.getOrNull().orEmpty()

                if (matches.isNotEmpty()) {
                    val bestMatch = matches.first()
                    insertOrUpdateMovie(file, bestMatch.tmdbId, bestMatch.title)
                } else {
                    // Unmatched
                    db.unmatchedFileDao().insertUnmatchedFiles(
                        listOf(
                            UnmatchedFileEntity(
                                id = UUID.randomUUID().toString(),
                                filePath = file.filePath,
                                fileName = file.fileName,
                                detectedTitle = parsed.cleanTitle,
                                detectedYear = parsed.year,
                                detectedSeason = null,
                                detectedEpisode = null,
                                isTvShow = false,
                                status = "PENDING"
                            )
                        )
                    )
                }
            }
        }

        emit(ScanProgress(isScanning = false, currentFile = "Scan completed", filesProcessed = total, totalFiles = total))
    }

    private fun traverseDirectory(dir: DocumentFile, result: MutableList<ScannedVideoFile>) {
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                traverseDirectory(file, result)
            } else if (file.isFile) {
                val name = file.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in supportedVideoExtensions) {
                    val parsed = parser.parse(name)
                    result.add(
                        ScannedVideoFile(
                            uri = file.uri,
                            filePath = file.uri.toString(),
                            fileName = name,
                            sizeBytes = file.length(),
                            parsedInfo = parsed
                        )
                    )
                }
            }
        }
    }

    private suspend fun insertOrUpdateMovie(
        file: ScannedVideoFile,
        tmdbId: Int,
        title: String
    ) {
        val movieEntity = MovieEntity(
            id = "movie_$tmdbId",
            tmdbId = tmdbId,
            title = title,
            year = file.parsedInfo.year,
            posterPath = null,
            backdropPath = null,
            overview = "Local video file: ${file.fileName}",
            rating = 8.0,
            genresCsv = "Movie",
            runtimeMinutes = 120,
            filePath = file.filePath,
            fileSizeFormatted = formatFileSize(file.sizeBytes),
            isWatched = false
        )
        db.movieDao().insertMovies(listOf(movieEntity))
    }

    private suspend fun insertOrUpdateTvShow(
        file: ScannedVideoFile,
        tmdbId: Int,
        title: String
    ) {
        val tvShowEntity = TvShowEntity(
            id = "tv_$tmdbId",
            tmdbId = tmdbId,
            title = title,
            year = file.parsedInfo.year,
            posterPath = null,
            backdropPath = null,
            overview = "Local TV Show folder",
            rating = 8.0,
            genresCsv = "Series",
            folderPath = file.filePath.substringBeforeLast('/'),
            seasonsCount = 1,
            totalEpisodesCount = 1,
            watchedEpisodesCount = 0
        )
        db.tvShowDao().insertTvShows(listOf(tvShowEntity))
    }

    private suspend fun insertOrUpdateTvShowEpisode(
        file: ScannedVideoFile,
        tmdbId: Int,
        showTitle: String,
        parsed: ParsedFileName
    ) {
        val tvShowId = "tv_$tmdbId"
        val seasonNum = parsed.seasonNumber ?: 1
        val episodeNum = parsed.episodeNumber ?: 1
        val seasonId = "season_${tmdbId}_$seasonNum"

        val tvShowEntity = TvShowEntity(
            id = tvShowId,
            tmdbId = tmdbId,
            title = showTitle,
            year = parsed.year,
            posterPath = null,
            backdropPath = null,
            overview = "Series folder scanned from local storage.",
            rating = 8.5,
            genresCsv = "Series",
            folderPath = file.filePath.substringBeforeLast('/'),
            seasonsCount = seasonNum,
            totalEpisodesCount = 1,
            watchedEpisodesCount = 0
        )
        db.tvShowDao().insertTvShows(listOf(tvShowEntity))

        val seasonEntity = SeasonEntity(
            id = seasonId,
            tvShowId = tvShowId,
            seasonNumber = seasonNum,
            title = "Season $seasonNum",
            posterPath = null,
            overview = "Season $seasonNum",
            totalEpisodesCount = 1,
            watchedEpisodesCount = 0
        )
        db.tvShowDao().insertSeasons(listOf(seasonEntity))

        val episodeEntity = EpisodeEntity(
            id = "ep_${tmdbId}_s${seasonNum}_e${episodeNum}",
            seasonId = seasonId,
            tvShowId = tvShowId,
            seasonNumber = seasonNum,
            episodeNumber = episodeNum,
            title = "Episode $episodeNum",
            overview = file.fileName,
            stillPath = null,
            filePath = file.filePath,
            fileSizeFormatted = formatFileSize(file.sizeBytes),
            runtimeMinutes = 45,
            isWatched = false
        )
        db.tvShowDao().insertEpisodes(listOf(episodeEntity))
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
