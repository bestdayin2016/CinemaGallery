package com.cinemagallery.domain.repository

import com.cinemagallery.domain.model.LibraryFolder
import com.cinemagallery.domain.model.ScanProgress
import kotlinx.coroutines.flow.Flow

interface LibraryFolderRepository {
    fun getFolders(): Flow<List<LibraryFolder>>
    suspend fun addFolder(uriString: String, displayName: String)
    suspend fun removeFolder(uriString: String)
    fun scanFolders(): Flow<ScanProgress>
}
