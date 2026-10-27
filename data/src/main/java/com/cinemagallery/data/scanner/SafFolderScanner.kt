package com.cinemagallery.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.cinemagallery.data.local.CinemaDatabase
import com.cinemagallery.data.local.entity.EpisodeEntity
import com.cinemagallery.data.local.entity.FolderEntity
import com.cinemagallery.data.local.entity.MovieEntity
import com.cinemagallery.data.local.entity.SeasonEntity
import com.cinemagallery.data.local.entity.TvShowEntity
import com.cinemagallery.data.local.entity.UnmatchedFileEntity
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.ParsedFileName
import com.cinemagallery.domain.model.ScanProgress
import com.cinemagallery.domain.parser.FileNameParser
import com.cinemagallery.domain.repository.TmdbRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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
        "mkv", "mp4", "avi", "m4v", "webm", "mov", "wmv", "flv", "ts",
        "mpg", "mpeg", "m2ts", "3gp", "vob", "divx", "ogv", "rm", "rmvb", "asf"
    )

    fun scanFolder(uriString: String): Flow<ScanProgress> = flow {
        emit(ScanProgress(isScanning = true, currentFile = "Opening folder...", filesProcessed = 0, totalFiles = 0))

        try {
            val rootUri = Uri.parse(uriString)
            val rootDoc = withContext(Dispatchers.IO) {
                DocumentFile.fromTreeUri(context, rootUri)
            }

            if (rootDoc == null || !rootDoc.exists()) {
                emit(ScanProgress(isScanning = false, currentFile = "Folder unavailable or permission revoked"))
                return@flow
            }

            emit(ScanProgress(isScanning = true, currentFile = "Discovering files in directory...", filesProcessed = 0, totalFiles = 0))

            val videoFiles = mutableListOf<ScannedVideoFile>()
            withContext(Dispatchers.IO) {
                traverseDirectory(rootDoc, videoFiles)
            }

            val total = videoFiles.size
            if (total == 0) {
                emit(ScanProgress(isScanning = false, currentFile = "Scan finished: 0 video files found in folder", filesProcessed = 0, totalFiles = 0))
                return@flow
            }

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

                try {
                    withContext(Dispatchers.IO) {
                        // 1. Check for persisted manual override first!
                        val manualOverride = db.manualOverrideDao().getOverrideForFile(file.filePath)

                        if (manualOverride != null) {
                            if (manualOverride.isTvShow) {
                                val searchResult = tmdbRepository.searchTvShows(manualOverride.customTitle)
                                val match = searchResult.getOrNull()?.find { it.tmdbId == manualOverride.tmdbId }
                                insertOrUpdateTvShowEpisode(file, manualOverride.tmdbId, manualOverride.customTitle, file.parsedInfo, match)
                            } else {
                                val searchResult = tmdbRepository.searchMovies(manualOverride.customTitle)
                                val match = searchResult.getOrNull()?.find { it.tmdbId == manualOverride.tmdbId }
                                insertOrUpdateMovie(file, manualOverride.tmdbId, manualOverride.customTitle, match)
                            }
                            return@withContext
                        }

                        // 2. Automatic regex parsing
                        val parsed = file.parsedInfo
                        val searchTitle = parsed.cleanTitle

                        if (parsed.isTvShow) {
                            val searchResult = tmdbRepository.searchTvShows(searchTitle, parsed.year)
                            val matches = searchResult.getOrNull().orEmpty()

                            if (matches.isNotEmpty()) {
                                val bestMatch = matches.first()
                                insertOrUpdateTvShowEpisode(file, bestMatch.tmdbId, bestMatch.title, parsed, bestMatch)
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
                                insertOrUpdateMovie(file, bestMatch.tmdbId, bestMatch.title, bestMatch)
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.IO) {
                db.folderDao().insertFolder(
                    FolderEntity(
                        uriString = uriString,
                        displayName = rootDoc.name ?: "Scanned Folder",
                        totalFilesFound = total,
                        lastScannedTimestamp = System.currentTimeMillis()
                    )
                )
            }

            emit(ScanProgress(isScanning = false, currentFile = "Scan completed ($total files found)", filesProcessed = total, totalFiles = total))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(ScanProgress(isScanning = false, currentFile = "Scan error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun traverseDirectory(dir: DocumentFile, result: MutableList<ScannedVideoFile>) {
        try {
            val files = dir.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    traverseDirectory(file, result)
                } else {
                    val name = file.name
                    if (!name.isNullOrBlank()) {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val isVideoExt = ext in supportedVideoExtensions
                        val isVideoMime = file.type?.lowercase()?.startsWith("video/") == true

                        if (isVideoExt || isVideoMime) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun insertOrUpdateMovie(
        file: ScannedVideoFile,
        tmdbId: Int,
        title: String,
        match: MediaItem.Movie? = null
    ) {
        val movieEntity = MovieEntity(
            id = "movie_$tmdbId",
            tmdbId = tmdbId,
            title = title,
            year = match?.year ?: file.parsedInfo.year,
            posterPath = match?.posterPath,
            backdropPath = match?.backdropPath,
            overview = match?.overview?.ifBlank { "Local video file: ${file.fileName}" } ?: "Local video file: ${file.fileName}",
            rating = match?.rating ?: 0.0,
            genresCsv = match?.genres?.joinToString(",") ?: "Movie",
            runtimeMinutes = match?.runtimeMinutes ?: 120,
            filePath = file.filePath,
            fileSizeFormatted = formatFileSize(file.sizeBytes),
            isWatched = false
        )
        db.movieDao().insertMovies(listOf(movieEntity))
    }

    private suspend fun insertOrUpdateTvShowEpisode(
        file: ScannedVideoFile,
        tmdbId: Int,
        showTitle: String,
        parsed: ParsedFileName,
        match: MediaItem.TvShow? = null
    ) {
        val tvShowId = "tv_$tmdbId"
        val seasonNum = parsed.seasonNumber ?: 1
        val episodeNum = parsed.episodeNumber ?: 1
        val seasonId = "season_${tmdbId}_$seasonNum"

        val tvShowEntity = TvShowEntity(
            id = tvShowId,
            tmdbId = tmdbId,
            title = showTitle,
            year = match?.year ?: parsed.year,
            posterPath = match?.posterPath,
            backdropPath = match?.backdropPath,
            overview = match?.overview?.ifBlank { "Series folder scanned from local storage." } ?: "Series folder scanned from local storage.",
            rating = match?.rating ?: 0.0,
            genresCsv = match?.genres?.joinToString(",") ?: "Series",
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
            posterPath = match?.posterPath,
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
            stillPath = match?.backdropPath,
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
