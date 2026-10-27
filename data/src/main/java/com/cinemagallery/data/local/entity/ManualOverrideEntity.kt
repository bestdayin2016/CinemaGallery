package com.cinemagallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_overrides")
data class ManualOverrideEntity(
    @PrimaryKey val filePath: String,
    val tmdbId: Int,
    val isTvShow: Boolean,
    val customTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
