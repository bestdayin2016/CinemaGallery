package com.cinemagallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cinemagallery.data.local.dao.FolderDao
import com.cinemagallery.data.local.dao.ManualOverrideDao
import com.cinemagallery.data.local.dao.MovieDao
import com.cinemagallery.data.local.dao.TvShowDao
import com.cinemagallery.data.local.dao.UnmatchedFileDao
import com.cinemagallery.data.local.entity.EpisodeEntity
import com.cinemagallery.data.local.entity.FolderEntity
import com.cinemagallery.data.local.entity.ManualOverrideEntity
import com.cinemagallery.data.local.entity.MovieEntity
import com.cinemagallery.data.local.entity.SeasonEntity
import com.cinemagallery.data.local.entity.TvShowEntity
import com.cinemagallery.data.local.entity.UnmatchedFileEntity

@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        UnmatchedFileEntity::class,
        ManualOverrideEntity::class,
        FolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CinemaDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun unmatchedFileDao(): UnmatchedFileDao
    abstract fun folderDao(): FolderDao
    abstract fun manualOverrideDao(): ManualOverrideDao
}
