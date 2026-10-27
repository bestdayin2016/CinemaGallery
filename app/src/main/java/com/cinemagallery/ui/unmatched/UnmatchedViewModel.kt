package com.cinemagallery.ui.unmatched

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.UnmatchedFile
import com.cinemagallery.domain.repository.TmdbRepository
import com.cinemagallery.domain.repository.UnmatchedFilesRepository
import com.cinemagallery.domain.usecase.ResolveUnmatchedFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnmatchedUiState(
    val files: List<UnmatchedFile> = emptyList(),
    val activeFixFile: UnmatchedFile? = null,
    val searchQuery: String = "",
    val searchResults: List<MediaItem> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class UnmatchedViewModel @Inject constructor(
    private val unmatchedRepository: UnmatchedFilesRepository,
    private val resolveUnmatchedUseCase: ResolveUnmatchedFileUseCase,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnmatchedUiState())
    val uiState: StateFlow<UnmatchedUiState> = _uiState.asStateFlow()

    init {
        unmatchedRepository.getUnmatchedFiles()
            .onEach { files ->
                _uiState.value = _uiState.value.copy(files = files, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun openManualFixDialog(file: UnmatchedFile) {
        _uiState.value = _uiState.value.copy(
            activeFixFile = file,
            searchQuery = file.detectedTitle
        )
        performSearch(file.detectedTitle, file.isTvShow)
    }

    fun closeManualFixDialog() {
        _uiState.value = _uiState.value.copy(
            activeFixFile = null,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun onQueryChange(query: String) {
        val file = _uiState.value.activeFixFile
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (file != null && query.isNotBlank()) {
            performSearch(query, file.isTvShow)
        }
    }

    private fun performSearch(query: String, isTvShow: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = if (isTvShow) {
                tmdbRepository.searchTvShows(query)
            } else {
                tmdbRepository.searchMovies(query)
            }
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(searchResults = list, isSearching = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            }
        }
    }

    fun resolveFile(tmdbItem: MediaItem) {
        val active = _uiState.value.activeFixFile ?: return
        viewModelScope.launch {
            resolveUnmatchedUseCase.resolve(
                fileId = active.id,
                tmdbId = tmdbItem.tmdbId,
                isTvShow = tmdbItem is MediaItem.TvShow,
                customTitle = tmdbItem.title
            )
            closeManualFixDialog()
        }
    }

    fun ignoreFile(fileId: String) {
        viewModelScope.launch {
            resolveUnmatchedUseCase.ignore(fileId)
        }
    }
}
