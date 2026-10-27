package com.cinemagallery.domain.model

enum class UnmatchedStatus {
    PENDING,
    RESOLVED,
    IGNORED
}

data class UnmatchedFile(
    val id: String,
    val filePath: String,
    val fileName: String,
    val detectedTitle: String,
    val detectedYear: Int?,
    val detectedSeason: Int?,
    val detectedEpisode: Int?,
    val isTvShow: Boolean,
    val status: UnmatchedStatus,
    val matchedTmdbId: Int? = null,
    val manualTitleOverride: String? = null
)
