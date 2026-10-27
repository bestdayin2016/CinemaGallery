package com.cinemagallery.domain.usecase

import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import javax.inject.Inject

class ResolveUnmatchedFileUseCase @Inject constructor(
    private val unmatchedFilesRepository: UnmatchedFilesRepository
) {
    suspend fun resolve(fileId: String, tmdbId: Int, isTvShow: Boolean, customTitle: String) {
        unmatchedFilesRepository.resolveUnmatchedFile(fileId, tmdbId, isTvShow, customTitle)
    }

    suspend fun ignore(fileId: String) {
        unmatchedFilesRepository.ignoreUnmatchedFile(fileId)
    }
}
