package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seasonId: String,
    val tvShowId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val stillPath: String?,
    val filePath: String,
    val fileSizeFormatted: String,
    val runtimeMinutes: Int,
    val isWatched: Boolean,
    val lastPlayedPositionMs: Long = 0L
)
