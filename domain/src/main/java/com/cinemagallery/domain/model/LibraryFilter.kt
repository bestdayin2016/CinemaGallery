package com.cinemagallery.domain.model

enum class MediaTypeFilter {
    ALL,
    MOVIES,
    TV_SHOWS
}

enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    RATING_DESC,
    YEAR_DESC,
    RECENTLY_ADDED
}

data class LibraryFilter(
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val searchQuery: String = "",
    val onlyUnwatched: Boolean = false
)
