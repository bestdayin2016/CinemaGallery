package com.cinemagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cinemagallery.data.local.entity.ManualOverrideEntity

@Dao
interface ManualOverrideDao {
    @Query("SELECT * FROM manual_overrides WHERE filePath = :filePath LIMIT 1")
    suspend fun getOverrideForFile(filePath: String): ManualOverrideEntity?

    @Query("SELECT * FROM manual_overrides")
    suspend fun getAllOverrides(): List<ManualOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: ManualOverrideEntity)
}
