package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.ManualOverrideDao
import com.cinemagallery.data.local.dao.UnmatchedFileDao
import com.cinemagallery.data.local.entity.ManualOverrideEntity
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.domain.model.UnmatchedFile
import com.cinemagallery.domain.model.UnmatchedStatus
import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnmatchedFilesRepositoryImpl @Inject constructor(
    private val unmatchedFileDao: UnmatchedFileDao,
    private val manualOverrideDao: ManualOverrideDao
) : UnmatchedFilesRepository {

    private val unmatchedState = MutableStateFlow(MockDataSupplier.sampleUnmatchedFiles)

    override fun getUnmatchedFiles(): Flow<List<UnmatchedFile>> = unmatchedState.asStateFlow()

    override suspend fun resolveUnmatchedFile(
        fileId: String,
        tmdbId: Int,
        isTvShow: Boolean,
        customTitle: String
    ) {
        val target = unmatchedState.value.find { it.id == fileId }
        if (target != null) {
            // Save manual override to DB so fixes persist across rescans!
            manualOverrideDao.insertOverride(
                ManualOverrideEntity(
                    filePath = target.filePath,
                    tmdbId = tmdbId,
                    isTvShow = isTvShow,
                    customTitle = customTitle
                )
            )

            // Update local state
            unmatchedState.value = unmatchedState.value.map { file ->
                if (file.id == fileId) {
                    file.copy(
                        status = UnmatchedStatus.RESOLVED,
                        matchedTmdbId = tmdbId,
                        manualTitleOverride = customTitle
                    )
                } else file
            }
        }
    }

    override suspend fun ignoreUnmatchedFile(fileId: String) {
        unmatchedState.value = unmatchedState.value.map { file ->
            if (file.id == fileId) {
                file.copy(status = UnmatchedStatus.IGNORED)
            } else file
        }
    }
}
