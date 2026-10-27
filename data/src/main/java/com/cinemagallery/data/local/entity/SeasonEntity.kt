package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey val id: String,
    val tvShowId: String,
    val seasonNumber: Int,
    val title: String,
    val posterPath: String?,
    val overview: String,
    val totalEpisodesCount: Int,
    val watchedEpisodesCount: Int
)
