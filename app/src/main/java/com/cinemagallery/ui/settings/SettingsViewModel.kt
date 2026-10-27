package com.cinemagallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinemagallery.domain.model.LibraryFolder
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.ScanProgress
import com.cinemagallery.domain.repository.LibraryFolderRepository
import com.cinemagallery.domain.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val folders: List<LibraryFolder> = emptyList(),
    val scanProgress: ScanProgress = ScanProgress(isScanning = false),
    val isTmdbApiKeyConfigured: Boolean = false,
    val testQuery: String = "Interstellar",
    val testResults: List<MediaItem.Movie> = emptyList(),
    val isTestSearching: Boolean = false,
    val testError: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val folderRepository: LibraryFolderRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load folders
        folderRepository.getFolders()
            .onEach { folders ->
                _uiState.value = _uiState.value.copy(folders = folders, isLoading = false)
            }
            .launchIn(viewModelScope)

        // Check TMDB API key
        viewModelScope.launch {
            val hasKey = tmdbRepository.getApiKeyStatus()
            _uiState.value = _uiState.value.copy(isTmdbApiKeyConfigured = hasKey)
            if (hasKey) {
                testSearchTmdb()
            }
        }
    }

    fun addFolder(uriString: String, displayName: String) {
        viewModelScope.launch {
            folderRepository.addFolder(uriString, displayName)
            startManualRescan()
        }
    }

    fun removeFolder(uriString: String) {
        viewModelScope.launch {
            folderRepository.removeFolder(uriString)
        }
    }

    fun startManualRescan() {
        folderRepository.scanFolders()
            .onEach { progress ->
                _uiState.value = _uiState.value.copy(scanProgress = progress)
            }
            .launchIn(viewModelScope)
    }

    fun onTestQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(testQuery = query)
    }

    fun testSearchTmdb() {
        val query = _uiState.value.testQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestSearching = true, testError = null, testResults = emptyList())
            val result = tmdbRepository.searchMovies(query)
            result.onSuccess { movies ->
                _uiState.value = _uiState.value.copy(
                    testResults = movies,
                    isTestSearching = false,
                    testError = if (movies.isEmpty()) "No movies found on TMDB for '$query'" else null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isTestSearching = false,
                    testError = "TMDB API Error: ${error.localizedMessage ?: "Invalid API key or network error"}"
                )
            }
        }
    }
}
