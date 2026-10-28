package com.cinemagallery.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinemagallery.domain.model.LibraryFilter
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.MediaTypeFilter
import com.cinemagallery.domain.model.SortOrder
import com.cinemagallery.domain.usecase.GetLibraryMediaUseCase
import com.cinemagallery.domain.usecase.ToggleWatchedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryViewMode {
    GRID,
    HORIZONTAL,
    STACK
}

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter(),
    val mediaItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val selectedMediaId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryMediaUseCase: GetLibraryMediaUseCase,
    private val toggleWatchedUseCase: ToggleWatchedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val filterState = MutableStateFlow(LibraryFilter())

    init {
        filterState
            .flatMapLatest { filter ->
                _uiState.value = _uiState.value.copy(filter = filter, isLoading = true)
                getLibraryMediaUseCase(filter)
            }
            .onEach { items ->
                val currentSelected = _uiState.value.selectedMediaId
                val newSelected = if (items.any { it.id == currentSelected }) currentSelected else items.firstOrNull()?.id
                _uiState.value = _uiState.value.copy(
                    mediaItems = items,
                    selectedMediaId = newSelected,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        filterState.value = filterState.value.copy(searchQuery = query)
    }

    fun onMediaTypeSelect(type: MediaTypeFilter) {
        filterState.value = filterState.value.copy(mediaType = type)
    }

    fun onSortOrderSelect(sortOrder: SortOrder) {
        filterState.value = filterState.value.copy(sortOrder = sortOrder)
    }

    fun onToggleOnlyUnwatched() {
        val current = filterState.value.onlyUnwatched
        filterState.value = filterState.value.copy(onlyUnwatched = !current)
    }

    fun cycleViewMode() {
        val nextMode = when (_uiState.value.viewMode) {
            LibraryViewMode.GRID -> LibraryViewMode.HORIZONTAL
            LibraryViewMode.HORIZONTAL -> LibraryViewMode.STACK
            LibraryViewMode.STACK -> LibraryViewMode.GRID
        }
        _uiState.value = _uiState.value.copy(viewMode = nextMode)
    }

    fun setViewMode(mode: LibraryViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setSelectedMediaId(id: String) {
        _uiState.value = _uiState.value.copy(selectedMediaId = id)
    }

    fun toggleMovieWatched(movieId: String, currentWatched: Boolean) {
        viewModelScope.launch {
            toggleWatchedUseCase.toggleMovie(movieId, !currentWatched)
        }
    }
}
