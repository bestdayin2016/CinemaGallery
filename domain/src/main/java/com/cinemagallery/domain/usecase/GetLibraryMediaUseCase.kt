package com.cinemagallery.domain.usecase

import com.cinemagallery.domain.model.LibraryFilter
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.MediaTypeFilter
import com.cinemagallery.domain.model.SortOrder
import com.cinemagallery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetLibraryMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    operator fun invoke(filter: LibraryFilter): Flow<List<MediaItem>> {
        return combine(
            mediaRepository.getMovies(),
            mediaRepository.getTvShows()
        ) { movies, tvShows ->
            val allItems: MutableList<MediaItem> = mutableListOf()

            if (filter.mediaType == MediaTypeFilter.ALL || filter.mediaType == MediaTypeFilter.MOVIES) {
                allItems.addAll(movies)
            }
            if (filter.mediaType == MediaTypeFilter.ALL || filter.mediaType == MediaTypeFilter.TV_SHOWS) {
                allItems.addAll(tvShows)
            }

            // Apply search query
            var filteredList = if (filter.searchQuery.isNotBlank()) {
                val query = filter.searchQuery.trim().lowercase()
                allItems.filter { item ->
                    item.title.lowercase().contains(query) ||
                            item.genres.any { it.lowercase().contains(query) }
                }
            } else {
                allItems
            }

            // Apply unwatched filter
            if (filter.onlyUnwatched) {
                filteredList = filteredList.filter { item ->
                    when (item) {
                        is MediaItem.Movie -> !item.isWatched
                        is MediaItem.TvShow -> !item.isFullyWatched
                    }
                }
            }

            // Apply sorting
            when (filter.sortOrder) {
                SortOrder.TITLE_ASC -> filteredList.sortedBy { it.title.lowercase() }
                SortOrder.TITLE_DESC -> filteredList.sortedByDescending { it.title.lowercase() }
                SortOrder.RATING_DESC -> filteredList.sortedByDescending { it.rating }
                SortOrder.YEAR_DESC -> filteredList.sortedByDescending { it.year ?: 0 }
                SortOrder.RECENTLY_ADDED -> filteredList // natural order or by ID
            }
        }
    }
}
