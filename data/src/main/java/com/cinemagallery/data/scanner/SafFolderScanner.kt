package com.cinemagallery.data.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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

    private val TAG = "SafScan"

    private val supportedVideoExtensions = setOf(
        "mkv", "mp4", "avi", "m4v", "webm", "mov", "wmv", "flv", "ts",
        "mpg", "mpeg", "m2ts", "3gp", "vob", "divx", "ogv", "rm", "rmvb", "asf"
    )

    fun scanFolder(uriString: String): Flow<ScanProgress> = flow {
        Log.d(TAG, "Starting scan of $uriString")
        emit(ScanProgress(isScanning = true, currentFile = "Opening folder...", filesProcessed = 0, totalFiles = 0))

        try {
            val rootUri = Uri.parse(uriString)

            // Log whether persistable permission is present
            val persistedPermissions = context.contentResolver.persistedUriPermissions
            val hasPermission = persistedPermissions.any { it.uri == rootUri && it.isReadPermission }
            Log.d(TAG, "Persistable permission present for $rootUri: $hasPermission")

            val videoFiles = mutableListOf<ScannedVideoFile>()

            emit(ScanProgress(isScanning = true, currentFile = "Discovering files in directory...", filesProcessed = 0, totalFiles = 0))

            withContext(Dispatchers.IO) {
                // Method 1: Native SAF DocumentsContract Cursor Query
                if (rootUri.scheme == "content") {
                    scanSafTree(rootUri, videoFiles)
                }

                // Method 2: Fallback to DocumentFile on original treeUri ONLY
                if (videoFiles.isEmpty() && rootUri.scheme == "content") {
                    Log.d(TAG, "Native SAF query yielded 0 files. Trying DocumentFile fallback on original treeUri: $rootUri")
                    val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
                    if (rootDoc != null && rootDoc.exists()) {
                        traverseDocumentFile(rootDoc, videoFiles)
                    } else {
                        Log.e(TAG, "DocumentFile.fromTreeUri returned null or non-existent document for $rootUri")
                    }
                }

                // Method 3: Fallback to java.io.File traversal if URI represents file/path
                if (videoFiles.isEmpty() && (rootUri.scheme == "file" || uriString.startsWith("/"))) {
                    val path = rootUri.path ?: uriString
                    val fileDir = File(path)
                    if (fileDir.exists() && fileDir.isDirectory) {
                        traverseIoFile(fileDir, videoFiles)
                    }
                }
            }

            val total = videoFiles.size
            Log.d(TAG, "Final number of video files found: $total for $uriString")

            if (total == 0) {
                emit(ScanProgress(isScanning = false, currentFile = "Scan finished: 0 video files found in folder", filesProcessed = 0, totalFiles = 0))
                return@flow
            }

            emit(ScanProgress(isScanning = true, currentFile = "Found $total video files", filesProcessed = 0, totalFiles = total))

            var tmdbNetworkAvailable = true

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
                            Log.d(TAG, "Applying manual override for ${file.fileName}: ${manualOverride.customTitle}")
                            var match: MediaItem? = null
                            if (tmdbNetworkAvailable) {
                                val searchResult = if (manualOverride.isTvShow) {
                                    tmdbRepository.searchTvShows(manualOverride.customTitle)
                                } else {
                                    tmdbRepository.searchMovies(manualOverride.customTitle)
                                }
                                if (searchResult.isFailure) {
                                    val err = searchResult.exceptionOrNull()
                                    if (err is SocketTimeoutException || err is UnknownHostException || err is ConnectException) {
                                        tmdbNetworkAvailable = false
                                    }
                                }
                                match = searchResult.getOrNull()?.find { it.tmdbId == manualOverride.tmdbId }
                            }

                            if (manualOverride.isTvShow) {
                                insertOrUpdateTvShowEpisode(file, manualOverride.tmdbId, manualOverride.customTitle, file.parsedInfo, match as? MediaItem.TvShow)
                            } else {
                                insertOrUpdateMovie(file, manualOverride.tmdbId, manualOverride.customTitle, match as? MediaItem.Movie)
                            }
                            return@withContext
                        }

                        // 2. Automatic regex parsing
                        val parsed = file.parsedInfo
                        val searchTitle = parsed.cleanTitle

                        if (parsed.isTvShow) {
                            var matches: List<MediaItem.TvShow> = emptyList()
                            if (tmdbNetworkAvailable) {
                                val searchResult = tmdbRepository.searchTvShows(searchTitle, parsed.year)
                                if (searchResult.isFailure) {
                                    val err = searchResult.exceptionOrNull()
                                    if (err is SocketTimeoutException || err is UnknownHostException || err is ConnectException) {
                                        tmdbNetworkAvailable = false
                                        Log.w(TAG, "Network timeout reaching TMDB. Switching remaining files in scan to fast local mode.")
                                    }
                                }
                                matches = searchResult.getOrNull().orEmpty()
                            }

                            if (matches.isNotEmpty()) {
                                val bestMatch = matches.first()
                                Log.d(TAG, "Matched TV show: ${bestMatch.title} (tmdbId=${bestMatch.tmdbId})")
                                insertOrUpdateTvShowEpisode(file, bestMatch.tmdbId, bestMatch.title, parsed, bestMatch)
                            } else {
                                // Add to local unmatched files
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
                            var matches: List<MediaItem.Movie> = emptyList()
                            if (tmdbNetworkAvailable) {
                                val searchResult = tmdbRepository.searchMovies(searchTitle, parsed.year)
                                if (searchResult.isFailure) {
                                    val err = searchResult.exceptionOrNull()
                                    if (err is SocketTimeoutException || err is UnknownHostException || err is ConnectException) {
                                        tmdbNetworkAvailable = false
                                        Log.w(TAG, "Network timeout reaching TMDB. Switching remaining files in scan to fast local mode.")
                                    }
                                }
                                matches = searchResult.getOrNull().orEmpty()
                            }

                            if (matches.isNotEmpty()) {
                                val bestMatch = matches.first()
                                Log.d(TAG, "Matched Movie: ${bestMatch.title} (tmdbId=${bestMatch.tmdbId})")
                                insertOrUpdateMovie(file, bestMatch.tmdbId, bestMatch.title, bestMatch)
                            } else {
                                // Add to local unmatched files
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

            val folderName = rootUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
                ?.ifBlank { "Scanned Folder" } ?: "Scanned Folder"

            withContext(Dispatchers.IO) {
                db.folderDao().insertFolder(
                    FolderEntity(
                        uriString = uriString,
                        displayName = folderName,
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

    // Method 1: Native SAF DocumentsContract Cursor Query
    private fun scanSafTree(treeUri: Uri, result: MutableList<ScannedVideoFile>) {
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            Log.d(TAG, "Root Tree Document ID: '$rootDocId'")
            scanSafDirectory(treeUri, rootDocId, result)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining tree document ID for $treeUri", e)
        }
    }

    private fun scanSafDirectory(treeUri: Uri, docId: String, result: MutableList<ScannedVideoFile>) {
        // ALWAYS pass original treeUri as first parameter
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                Log.d(TAG, "Cursor count for directory docId '$docId': ${cursor.count}")
                if (cursor.count == 0) {
                    Log.d(TAG, "Directory '$docId' is empty")
                    return
                }

                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idIndex) ?: continue
                    val name = cursor.getString(nameIndex) ?: continue
                    val mimeType = cursor.getString(mimeIndex) ?: ""
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                    val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    Log.d(TAG, "Found -> Doc ID: '$childDocId' | Name: '$name' | MIME: '$mimeType' | isDir: $isDirectory")

                    if (isDirectory) {
                        Log.d(TAG, "Recursively entering subfolder: '$name' (childDocId: '$childDocId')")
                        // ALWAYS pass original treeUri and child document ID
                        scanSafDirectory(treeUri, childDocId, result)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val isVideoExt = ext in supportedVideoExtensions
                        val isVideoMime = mimeType.lowercase().startsWith("video/")
                        val isVideo = isVideoExt || isVideoMime || (ext.length in 2..4 && ext != "txt" && ext != "srt" && ext != "jpg" && ext != "png" && ext != "nfo")

                        if (isVideo) {
                            // ALWAYS pass original treeUri and child document ID
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                            val parsed = parser.parse(name)
                            Log.d(TAG, "ACCEPTED VIDEO -> Name: '$name' | Doc ID: '$childDocId' | Size: $size bytes")
                            result.add(
                                ScannedVideoFile(
                                    uri = fileUri,
                                    filePath = fileUri.toString(),
                                    fileName = name,
                                    sizeBytes = size,
                                    parsedInfo = parsed
                                )
                            )
                        } else {
                            Log.d(TAG, "SKIPPED NON-VIDEO -> Name: '$name' | Ext: '$ext'")
                        }
                    }
                }
            } ?: Log.e(TAG, "Null cursor returned for childrenUri: $childrenUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SAF directory '$docId'", e)
        }
    }

    // Method 2: DocumentFile Traversal (Uses original tree URI, never calls fromTreeUri on child URI)
    private fun traverseDocumentFile(dir: DocumentFile, result: MutableList<ScannedVideoFile>) {
        try {
            val files = dir.listFiles()
            Log.d(TAG, "DocumentFile listing '${dir.name}': ${files.size} items")
            for (file in files) {
                val name = file.name ?: continue
                if (file.isDirectory) {
                    traverseDocumentFile(file, result)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isVideoExt = ext in supportedVideoExtensions
                    val isVideoMime = file.type?.lowercase()?.startsWith("video/") == true
                    val isVideo = isVideoExt || isVideoMime || (ext.length in 2..4 && ext != "txt" && ext != "srt" && ext != "jpg" && ext != "png" && ext != "nfo")

                    if (isVideo) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in traverseDocumentFile", e)
        }
    }

    // Method 3: java.io.File Traversal
    private fun traverseIoFile(dir: File, result: MutableList<ScannedVideoFile>) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    traverseIoFile(file, result)
                } else if (file.isFile) {
                    val name = file.name
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in supportedVideoExtensions) {
                        val parsed = parser.parse(name)
                        result.add(
                            ScannedVideoFile(
                                uri = Uri.fromFile(file),
                                filePath = file.absolutePath,
                                fileName = name,
                                sizeBytes = file.length(),
                                parsedInfo = parsed
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in traverseIoFile", e)
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
