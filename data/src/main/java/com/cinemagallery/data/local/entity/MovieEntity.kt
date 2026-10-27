package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val tmdbId: Int,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val rating: Double,
    val genresCsv: String,
    val runtimeMinutes: Int,
    val filePath: String,
    val fileSizeFormatted: String,
    val isWatched: Boolean,
    val lastPlayedPositionMs: Long = 0L,
    val addedTimestamp: Long = System.currentTimeMillis()
)
