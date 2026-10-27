package com.cinemagallery.data.repository

import com.cinemagallery.data.local.dao.FolderDao
import com.cinemagallery.data.local.entity.FolderEntity
import com.cinemagallery.data.mock.MockDataSupplier
import com.cinemagallery.data.scanner.SafFolderScanner
import com.cinemagallery.domain.model.LibraryFolder
import com.cinemagallery.domain.model.ScanProgress
import com.cinemagallery.domain.repository.LibraryFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryFolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val scanner: SafFolderScanner
) : LibraryFolderRepository {

    override fun getFolders(): Flow<List<LibraryFolder>> {
        return folderDao.getAllFolders().map { entities ->
            if (entities.isEmpty()) {
                MockDataSupplier.sampleFolders
            } else {
                entities.map { entity ->
                    LibraryFolder(
                        uriString = entity.uriString,
                        displayName = entity.displayName,
                        totalFilesFound = entity.totalFilesFound,
                        lastScannedTimeFormatted = if (entity.lastScannedTimestamp > 0) {
                            "Scanned recently"
                        } else "Not scanned yet"
                    )
                }
            }
        }
    }

    override suspend fun addFolder(uriString: String, displayName: String) {
        folderDao.insertFolder(
            FolderEntity(
                uriString = uriString,
                displayName = displayName,
                totalFilesFound = 0,
                lastScannedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFolder(uriString: String) {
        folderDao.deleteFolder(uriString)
    }

    override fun scanFolders(): Flow<ScanProgress> {
        return flow {
            emit(ScanProgress(isScanning = true, currentFile = "Starting scan..."))
            folderDao.getAllFolders().collect { list ->
                if (list.isEmpty()) {
                    emit(ScanProgress(isScanning = false, currentFile = "No folders added. Add a folder in Settings."))
                } else {
                    for (folder in list) {
                        scanner.scanFolder(folder.uriString).collect { progress ->
                            emit(progress)
                        }
                    }
                }
            }
        }
    }
}
