package com.cinemagallery.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinemagallery.data.local.dao.FolderDao
import com.cinemagallery.data.scanner.SafFolderScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

@HiltWorker
class FolderScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val folderDao: FolderDao,
    private val scanner: SafFolderScanner
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val folders = folderDao.getAllFolders()
            // Folder scan work logic
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
