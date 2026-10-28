package com.cinemagallery.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.MediaTypeFilter
import com.cinemagallery.domain.model.SortOrder
import com.cinemagallery.ui.components.MediaCard
import com.cinemagallery.ui.components.MediaListItem
import com.cinemagallery.ui.theme.CinemaGalleryTheme

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onMediaClick: (MediaItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LibraryContent(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onMediaTypeSelect = viewModel::onMediaTypeSelect,
        onSortOrderSelect = viewModel::onSortOrderSelect,
        onToggleOnlyUnwatched = viewModel::onToggleOnlyUnwatched,
        onToggleViewMode = viewModel::toggleViewMode,
        onMediaClick = onMediaClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onSearchQueryChange: (String) -> Unit,
    onMediaTypeSelect: (MediaTypeFilter) -> Unit,
    onSortOrderSelect: (SortOrder) -> Unit,
    onToggleOnlyUnwatched: () -> Unit,
    onToggleViewMode: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = state.filter.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search titles or genres...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    onSearchQueryChange("")
                                    isSearchExpanded = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Close search")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    if (!isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = { isSortMenuExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)") },
                            onClick = {
                                onSortOrderSelect(SortOrder.TITLE_ASC)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (Z-A)") },
                            onClick = {
                                onSortOrderSelect(SortOrder.TITLE_DESC)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Rated") },
                            onClick = {
                                onSortOrderSelect(SortOrder.RATING_DESC)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Release Year") },
                            onClick = {
                                onSortOrderSelect(SortOrder.YEAR_DESC)
                                isSortMenuExpanded = false
                            }
                        )
                    }
                    IconButton(onClick = onToggleViewMode) {
                        Icon(
                            imageVector = if (state.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = state.filter.mediaType == MediaTypeFilter.ALL,
                        onClick = { onMediaTypeSelect(MediaTypeFilter.ALL) },
                        label = { Text("All Media") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter.mediaType == MediaTypeFilter.MOVIES,
                        onClick = { onMediaTypeSelect(MediaTypeFilter.MOVIES) },
                        label = { Text("Movies") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter.mediaType == MediaTypeFilter.TV_SHOWS,
                        onClick = { onMediaTypeSelect(MediaTypeFilter.TV_SHOWS) },
                        label = { Text("TV Shows") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter.onlyUnwatched,
                        onClick = onToggleOnlyUnwatched,
                        label = { Text("Unwatched Only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No media found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting filters or scan a media folder in Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                if (state.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.mediaItems,
                            key = { it.id }
                        ) { item ->
                            MediaCard(
                                item = item,
                                onClick = { onMediaClick(item) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.mediaItems,
                            key = { it.id }
                        ) { item ->
                            MediaListItem(
                                item = item,
                                onClick = { onMediaClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    CinemaGalleryTheme {
        LibraryContent(
            state = LibraryUiState(
                mediaItems = listOf(
                    MediaItem.Movie(
                        id = "m1",
                        tmdbId = 157336,
                        title = "Interstellar",
                        year = 2014,
                        posterPath = null,
                        backdropPath = null,
                        overview = "Space exploration drama",
                        rating = 8.6,
                        genres = listOf("Sci-Fi"),
                        runtimeMinutes = 169,
                        filePath = "",
                        fileSizeFormatted = "3.2 GB",
                        isWatched = true
                    ),
                    MediaItem.TvShow(
                        id = "tv1",
                        tmdbId = 1396,
                        title = "Breaking Bad",
                        year = 2008,
                        posterPath = null,
                        backdropPath = null,
                        overview = "Crime drama",
                        rating = 8.9,
                        genres = listOf("Drama", "Crime"),
                        folderPath = "",
                        seasonsCount = 5,
                        totalEpisodesCount = 62,
                        watchedEpisodesCount = 45
                    )
                )
            ),
            onSearchQueryChange = {},
            onMediaTypeSelect = {},
            onSortOrderSelect = {},
            onToggleOnlyUnwatched = {},
            onToggleViewMode = {},
            onMediaClick = {}
        )
    }
}
