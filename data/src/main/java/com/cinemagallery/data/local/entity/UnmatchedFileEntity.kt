package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unmatched_files")
data class UnmatchedFileEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val fileName: String,
    val detectedTitle: String,
    val detectedYear: Int?,
    val detectedSeason: Int?,
    val detectedEpisode: Int?,
    val isTvShow: Boolean,
    val status: String, // PENDING, RESOLVED, IGNORED
    val matchedTmdbId: Int? = null,
    val manualTitleOverride: String? = null
)
