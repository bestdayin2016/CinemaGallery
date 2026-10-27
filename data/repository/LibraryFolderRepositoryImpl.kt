package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.FolderDao
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.domain.model.LibraryFolder
import com.cinemagallery.domain.model.ScanProgress
import com.cinemagallery.domain.repository.LibraryFolderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryFolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : LibraryFolderRepository {

    private val foldersState = MutableStateFlow(MockDataSupplier.sampleFolders)

    override fun getFolders(): Flow<List<LibraryFolder>> = foldersState.asStateFlow()

    override suspend fun addFolder(uriString: String, displayName: String) {
        val newFolder = LibraryFolder(
            uriString = uriString,
            displayName = displayName,
            totalFilesFound = 0,
            lastScannedTimeFormatted = "Just added"
        )
        foldersState.value = foldersState.value + newFolder
    }

    override suspend fun removeFolder(uriString: String) {
        foldersState.value = foldersState.value.filter { it.uriString != uriString }
    }

    override fun scanFolders(): Flow<ScanProgress> = flow {
        emit(ScanProgress(isScanning = true, currentFile = "Initializing scan...", filesProcessed = 0, totalFiles = 10))
        delay(600)
        emit(ScanProgress(isScanning = true, currentFile = "Scanning Interstellar.2014.mkv", filesProcessed = 3, totalFiles = 10))
        delay(600)
        emit(ScanProgress(isScanning = true, currentFile = "Scanning Breaking Bad S01E01.mkv", filesProcessed = 7, totalFiles = 10))
        delay(600)
        emit(ScanProgress(isScanning = true, currentFile = "Parsing metadata...", filesProcessed = 10, totalFiles = 10))
        delay(400)
        emit(ScanProgress(isScanning = false, currentFile = "", filesProcessed = 10, totalFiles = 10))
    }
}
