package com.cinemagallery.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinemagallery.domain.model.Episode
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.domain.model.Season
import com.cinemagallery.domain.repository.MediaRepository
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

data class TvShowDetailUiState(
    val show: MediaItem.TvShow? = null,
    val seasons: List<Season> = emptyList(),
    val selectedSeasonNumber: Int = 1,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TvShowDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val toggleWatchedUseCase: ToggleWatchedUseCase
) : ViewModel() {

    private val tvShowId: String = checkNotNull(savedStateHandle["tvShowId"])

    private val _uiState = MutableStateFlow(TvShowDetailUiState())
    val uiState: StateFlow<TvShowDetailUiState> = _uiState.asStateFlow()

    private val selectedSeasonNumberState = MutableStateFlow(1)

    init {
        // Load TvShow info
        mediaRepository.getTvShowById(tvShowId)
            .onEach { show ->
                _uiState.value = _uiState.value.copy(show = show)
            }
            .launchIn(viewModelScope)

        // Load Seasons
        mediaRepository.getSeasonsForShow(tvShowId)
            .onEach { seasons ->
                val currentSelected = _uiState.value.selectedSeasonNumber
                val firstSeasonNum = seasons.firstOrNull()?.seasonNumber ?: 1
                val activeNum = if (seasons.any { it.seasonNumber == currentSelected }) currentSelected else firstSeasonNum
                _uiState.value = _uiState.value.copy(
                    seasons = seasons,
                    selectedSeasonNumber = activeNum,
                    isLoading = false
                )
                if (selectedSeasonNumberState.value != activeNum) {
                    selectedSeasonNumberState.value = activeNum
                }
            }
            .launchIn(viewModelScope)

        // Load Episodes for selected season
        selectedSeasonNumberState
            .flatMapLatest { seasonNum ->
                mediaRepository.getEpisodesForSeason(tvShowId, seasonNum)
            }
            .onEach { episodes ->
                _uiState.value = _uiState.value.copy(episodes = episodes)
            }
            .launchIn(viewModelScope)
    }

    fun selectSeason(seasonNumber: Int) {
        selectedSeasonNumberState.value = seasonNumber
        _uiState.value = _uiState.value.copy(selectedSeasonNumber = seasonNumber)
    }

    fun toggleEpisodeWatched(episodeId: String, currentWatched: Boolean) {
        viewModelScope.launch {
            toggleWatchedUseCase.toggleEpisode(episodeId, !currentWatched)
        }
    }

    fun toggleSeasonWatched(seasonNumber: Int, isWatched: Boolean) {
        viewModelScope.launch {
            toggleWatchedUseCase.toggleSeason(tvShowId, seasonNumber, isWatched)
        }
    }
}
