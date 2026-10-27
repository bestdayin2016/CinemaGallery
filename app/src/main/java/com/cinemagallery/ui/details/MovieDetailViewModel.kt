package com.cinemagallery.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.repository.MediaRepository
import com.cinemagallery.domain.usecase.ToggleWatchedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: MediaItem.Movie? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val toggleWatchedUseCase: ToggleWatchedUseCase
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        mediaRepository.getMovieById(movieId)
            .onEach { movie ->
                _uiState.value = MovieDetailUiState(movie = movie, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun toggleWatched() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            toggleWatchedUseCase.toggleMovie(movie.id, !movie.isWatched)
        }
    }
}
