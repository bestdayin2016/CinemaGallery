package com.cinemagallery.data.scanner

import android.content.Context
import android.net.Uri
import android.util.Log
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

    private val TAG = "SafFolderScanner"

    private val supportedVideoExtensions = setOf(
        "mkv", "mp4", "avi", "m4v", "webm", "mov", "wmv", "flv", "ts",
        "mpg", "mpeg", "m2ts", "3gp", "vob", "divx", "ogv", "rm", "rmvb", "asf"
    )

    fun scanFolder(uriString: String): Flow<ScanProgress> = flow {
        Log.d(TAG, "Starting scan for URI: $uriString")
        emit(ScanProgress(isScanning = true, currentFile = "Opening folder...", filesProcessed = 0, totalFiles = 0))

        try {
            val rootUri = Uri.parse(uriString)
            val rootDoc = withContext(Dispatchers.IO) {
                DocumentFile.fromTreeUri(context, rootUri)
            }

            Log.d(TAG, "Root DocumentFile: name=${rootDoc?.name}, exists=${rootDoc?.exists()}, isDir=${rootDoc?.isDirectory}")

            if (rootDoc == null || !rootDoc.exists()) {
                Log.e(TAG, "Root document is null or does not exist")
                emit(ScanProgress(isScanning = false, currentFile = "Folder unavailable or permission revoked"))
                return@flow
            }

            emit(ScanProgress(isScanning = true, currentFile = "Discovering files in ${rootDoc.name ?: "folder"}...", filesProcessed = 0, totalFiles = 0))

            val videoFiles = mutableListOf<ScannedVideoFile>()
            withContext(Dispatchers.IO) {
                traverseDirectory(rootDoc, videoFiles)
            }

            val total = videoFiles.size
            Log.d(TAG, "Traverse complete. Found $total video files in $uriString")

            if (total == 0) {
                emit(ScanProgress(isScanning = false, currentFile = "Scan finished: 0 video files found in ${rootDoc.name ?: "folder"}", filesProcessed = 0, totalFiles = 0))
                return@flow
            }

            emit(ScanProgress(isScanning = true, currentFile = "Found $total video files", filesProcessed = 0, totalFiles = total))

            for ((index, file) in videoFiles.withIndex()) {
                Log.d(TAG, "Processing video file [${index + 1}/$total]: ${file.fileName}")
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
                            Log.d(TAG, "Using manual override for ${file.fileName}: ${manualOverride.customTitle}")
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
                        Log.d(TAG, "Parsed title for ${file.fileName}: '$searchTitle' (year=${parsed.year}, isTv=${parsed.isTvShow})")

                        if (parsed.isTvShow) {
                            val searchResult = tmdbRepository.searchTvShows(searchTitle, parsed.year)
                            val matches = searchResult.getOrNull().orEmpty()

                            if (matches.isNotEmpty()) {
                                val bestMatch = matches.first()
                                Log.d(TAG, "Matched TV show: ${bestMatch.title} (tmdbId=${bestMatch.tmdbId})")
                                insertOrUpdateTvShowEpisode(file, bestMatch.tmdbId, bestMatch.title, parsed, bestMatch)
                            } else {
                                Log.w(TAG, "No TMDB match found for TV show: $searchTitle. Adding to unmatched.")
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
                                Log.d(TAG, "Matched Movie: ${bestMatch.title} (tmdbId=${bestMatch.tmdbId})")
                                insertOrUpdateMovie(file, bestMatch.tmdbId, bestMatch.title, bestMatch)
                            } else {
                                Log.w(TAG, "No TMDB match found for Movie: $searchTitle. Adding to unmatched.")
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
                    Log.e(TAG, "Error processing file ${file.fileName}", e)
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
            Log.e(TAG, "Fatal scan error for $uriString", e)
            emit(ScanProgress(isScanning = false, currentFile = "Scan error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun traverseDirectory(dir: DocumentFile, result: MutableList<ScannedVideoFile>) {
        try {
            val files = dir.listFiles()
            Log.d(TAG, "traverseDirectory '${dir.name}': found ${files.size} items")
            for (file in files) {
                val name = file.name ?: continue
                val isDir = file.isDirectory
                Log.d(TAG, "Item in '${dir.name}': name='$name', isDir=$isDir, type='${file.type}'")

                if (isDir) {
                    traverseDirectory(file, result)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isVideoExt = ext in supportedVideoExtensions
                    val isVideoMime = file.type?.lowercase()?.startsWith("video/") == true
                    // Accept any file if extension matches OR mime type is video OR file is inside media folder
                    val isVideo = isVideoExt || isVideoMime || (ext.length in 2..4 && ext != "txt" && ext != "srt" && ext != "jpg" && ext != "png" && ext != "nfo")

                    if (isVideo) {
                        val parsed = parser.parse(name)
                        Log.d(TAG, "Added video file: '$name' (ext=$ext)")
                        result.add(
                            ScannedVideoFile(
                                uri = file.uri,
                                filePath = file.uri.toString(),
                                fileName = name,
                                sizeBytes = file.length(),
                                parsedInfo = parsed
                            )
                        )
                    } else {
                        Log.d(TAG, "Skipping non-video file: '$name' (ext=$ext)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in traverseDirectory '${dir.name}'", e)
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
