package com.cinemagallery.domain.usecase

import com.cinemagallery.domain.repository.MediaRepository
import javax.inject.Inject

class DeleteMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: String, isTvShow: Boolean) {
        repository.deleteMediaItem(id, isTvShow)
    }
}
