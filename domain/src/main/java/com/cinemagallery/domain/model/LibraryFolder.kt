package com.cinemagallery.domain.model

data class LibraryFolder(
    val uriString: String,
    val displayName: String,
    val totalFilesFound: Int,
    val lastScannedTimeFormatted: String
)

data class ScanProgress(
    val isScanning: Boolean,
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0
)
