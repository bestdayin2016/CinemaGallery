package com.cinemagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cinemagallery.data.local.entity.UnmatchedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnmatchedFileDao {
    @Query("SELECT * FROM unmatched_files WHERE status = 'PENDING' ORDER BY fileName ASC")
    fun getUnmatchedFiles(): Flow<List<UnmatchedFileEntity>>

    @Query("SELECT * FROM unmatched_files WHERE id = :id LIMIT 1")
    suspend fun getUnmatchedFileById(id: String): UnmatchedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnmatchedFiles(files: List<UnmatchedFileEntity>)

    @Query("UPDATE unmatched_files SET status = :status, matchedTmdbId = :tmdbId, manualTitleOverride = :customTitle WHERE id = :id")
    suspend fun updateResolution(id: String, status: String, tmdbId: Int, customTitle: String)

    @Query("UPDATE unmatched_files SET status = 'IGNORED' WHERE id = :id")
    suspend fun ignoreFile(id: String)
}
