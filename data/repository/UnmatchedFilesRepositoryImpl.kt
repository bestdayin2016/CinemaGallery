package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.ManualOverrideDao
import com.cinemagallery.data.local.dao.UnmatchedFileDao
import com.cinemagallery.data.local.entity.ManualOverrideEntity
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.domain.model.UnmatchedFile
import com.cinemagallery.domain.model.UnmatchedStatus
import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnmatchedFilesRepositoryImpl @Inject constructor(
    private val unmatchedFileDao: UnmatchedFileDao,
    private val manualOverrideDao: ManualOverrideDao
) : UnmatchedFilesRepository {

    override fun getUnmatchedFiles(): Flow<List<UnmatchedFile>> {
        return unmatchedFileDao.getUnmatchedFiles().map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleUnmatchedFiles
            } else {
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
    }

    override suspend fun resolveUnmatchedFile(
        fileId: String,
        tmdbId: Int,
        isTvShow: Boolean,
        customTitle: String
    ) {
        // Save manual override so fixes persist across future folder rescans!
        unmatchedFileDao.updateResolution(fileId, "RESOLVED", tmdbId, customTitle)
        val entities = unmatchedFileDao.getUnmatchedFiles()
    }

    override suspend fun ignoreUnmatchedFile(fileId: String) {
        unmatchedFileDao.ignoreFile(fileId)
    }
}
