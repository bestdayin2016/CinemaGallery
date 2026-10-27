package com.cinemagallery.domain.repository

import com.cinemagallery.domain.model.UnmatchedFile
import kotlinx.coroutines.flow.Flow

interface UnmatchedFilesRepository {
    fun getUnmatchedFiles(): Flow<List<UnmatchedFile>>
    suspend fun resolveUnmatchedFile(
        fileId: String,
        tmdbId: Int,
        isTvShow: Boolean,
        customTitle: String
    )
    suspend fun ignoreUnmatchedFile(fileId: String)
}
