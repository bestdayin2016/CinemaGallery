package com.cinemagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cinemagallery.data.local.entity.EpisodeEntity
import com.cinemagallery.data.local.entity.SeasonEntity
import com.cinemagallery.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {
    @Query("SELECT * FROM tv_shows ORDER BY title ASC")
    fun getAllTvShows(): Flow<List<TvShowEntity>>

    @Query("SELECT * FROM tv_shows WHERE id = :id")
    fun getTvShowById(id: String): Flow<TvShowEntity?>

    @Query("SELECT * FROM seasons WHERE tvShowId = :tvShowId ORDER BY seasonNumber ASC")
    fun getSeasonsForShow(tvShowId: String): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM episodes WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    fun getEpisodesForSeason(tvShowId: String, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvShows(shows: List<TvShowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("UPDATE episodes SET isWatched = :isWatched WHERE id = :episodeId")
    suspend fun updateEpisodeWatchedStatus(episodeId: String, isWatched: Boolean)

    @Query("UPDATE episodes SET isWatched = :isWatched WHERE tvShowId = :tvShowId AND seasonNumber = :seasonNumber")
    suspend fun updateSeasonWatchedStatus(tvShowId: String, seasonNumber: Int, isWatched: Boolean)
}
