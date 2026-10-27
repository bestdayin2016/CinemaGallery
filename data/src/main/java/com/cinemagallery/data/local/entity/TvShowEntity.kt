package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: String,
    val tmdbId: Int,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val rating: Double,
    val genresCsv: String,
    val folderPath: String,
    val seasonsCount: Int,
    val totalEpisodesCount: Int,
    val watchedEpisodesCount: Int,
    val addedTimestamp: Long = System.currentTimeMillis()
)
