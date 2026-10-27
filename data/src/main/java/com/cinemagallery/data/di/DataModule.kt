package com.cinemagallery.data.di

import android.content.Context
import androidx.room.Room
import com.cinemagallery.data.BuildConfig
import com.cinemagallery.data.local.CinemaDatabase
import com.cinemagallery.data.local.dao.FolderDao
import com.cinemagallery.data.local.dao.ManualOverrideDao
import com.cinemagallery.data.local.dao.MovieDao
import com.cinemagallery.data.local.dao.TvShowDao
import com.cinemagallery.data.local.dao.UnmatchedFileDao
import com.cinemagallery.data.parser.RegexFileNameParser
import com.cinemagallery.data.remote.api.TmdbApiService
import com.cinemagallery.data.repository.LibraryFolderRepositoryImpl
import com.cinemagallery.data.repository.MediaRepositoryImpl
import com.cinemagallery.data.repository.TmdbRepositoryImpl
import com.cinemagallery.data.repository.UnmatchedFilesRepositoryImpl
import com.cinemagallery.domain.parser.FileNameParser
import com.cinemagallery.domain.repository.LibraryFolderRepository
import com.cinemagallery.domain.repository.MediaRepository
import com.cinemagallery.domain.repository.TmdbRepository
import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindFileNameParser(
        parser: RegexFileNameParser
    ): FileNameParser

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindUnmatchedFilesRepository(
        impl: UnmatchedFilesRepositoryImpl
    ): UnmatchedFilesRepository

    @Binds
    @Singleton
    abstract fun bindLibraryFolderRepository(
        impl: LibraryFolderRepositoryImpl
    ): LibraryFolderRepository

    @Binds
    @Singleton
    abstract fun bindTmdbRepository(
        impl: TmdbRepositoryImpl
    ): TmdbRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context
        ): CinemaDatabase {
            return Room.databaseBuilder(
                context,
                CinemaDatabase::class.java,
                "cinema_gallery.db"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        fun provideMovieDao(db: CinemaDatabase): MovieDao = db.movieDao()

        @Provides
        fun provideTvShowDao(db: CinemaDatabase): TvShowDao = db.tvShowDao()

        @Provides
        fun provideUnmatchedFileDao(db: CinemaDatabase): UnmatchedFileDao = db.unmatchedFileDao()

        @Provides
        fun provideFolderDao(db: CinemaDatabase): FolderDao = db.folderDao()

        @Provides
        fun provideManualOverrideDao(db: CinemaDatabase): ManualOverrideDao = db.manualOverrideDao()

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        }

        @Provides
        @Singleton
        fun provideTmdbApiService(okHttpClient: OkHttpClient): TmdbApiService {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            val contentType = "application/json".toMediaType()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.TMDB_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(TmdbApiService::class.java)
        }
    }
}
