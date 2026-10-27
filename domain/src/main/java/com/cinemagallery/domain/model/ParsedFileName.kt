package com.cinemagallery.domain.model

data class ParsedFileName(
    val originalFileName: String,
    val cleanTitle: String,
    val year: Int?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val isTvShow: Boolean,
    val resolution: String? = null,
    val codec: String? = null
)
