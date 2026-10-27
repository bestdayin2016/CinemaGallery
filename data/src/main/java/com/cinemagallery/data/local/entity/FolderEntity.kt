package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_folders")
data class FolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val totalFilesFound: Int,
    val lastScannedTimestamp: Long
)
