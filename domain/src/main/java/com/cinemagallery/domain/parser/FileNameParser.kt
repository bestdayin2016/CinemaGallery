package com.cinemagallery.domain.parser

import com.cinemagallery.domain.model.ParsedFileName

interface FileNameParser {
    fun parse(fileName: String): ParsedFileName
}
