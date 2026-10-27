package com.cinemagallery.data.parser

import com.cinemagallery.domain.model.ParsedFileName
import com.cinemagallery.domain.parser.FileNameParser
import javax.inject.Inject

class RegexFileNameParser @Inject constructor() : FileNameParser {

    override fun parse(fileName: String): ParsedFileName {
        val extension = fileName.substringAfterLast('.', "")
        val baseName = if (extension.isNotBlank() && extension.length <= 4) {
            fileName.substringBeforeLast('.')
        } else {
            fileName
        }

        // Clean dots, underscores, dashes
        var clean = baseName.replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()

        // Detect resolution
        val resolutionRegex = Regex("(?i)\\b(4k|2160p|1080p|720p|480p)\\b")
        val resolutionMatch = resolutionRegex.find(clean)
        val resolution = resolutionMatch?.value

        // Detect codec
        val codecRegex = Regex("(?i)\\b(x264|x265|h264|hevc|xvid|divx)\\b")
        val codecMatch = codecRegex.find(clean)
        val codec = codecMatch?.value

        var seasonNumber: Int? = null
        var episodeNumber: Int? = null
        var isTvShow = false

        // Detect TV Show patterns e.g. S01E02 or S1E2 or S01.E02
        val s00e00Regex = Regex("(?i)\\bS(\\d{1,2})[ .]?E(\\d{1,2})\\b")
        val s00Match = s00e00Regex.find(clean)

        if (s00Match != null) {
            isTvShow = true
            seasonNumber = s00Match.groupValues.getOrNull(1)?.toIntOrNull()
            episodeNumber = s00Match.groupValues.getOrNull(2)?.toIntOrNull()
            clean = clean.substring(0, s00Match.range.first).trim()
        } else {
            // Detect TV Show pattern e.g. 1x02 or 01x02
            val nxnRegex = Regex("(?i)\\b(\\d{1,2})x(\\d{1,2})\\b")
            val nxnMatch = nxnRegex.find(clean)
            if (nxnMatch != null) {
                isTvShow = true
                seasonNumber = nxnMatch.groupValues.getOrNull(1)?.toIntOrNull()
                episodeNumber = nxnMatch.groupValues.getOrNull(2)?.toIntOrNull()
                clean = clean.substring(0, nxnMatch.range.first).trim()
            }
        }

        // Detect Year (e.g., 19xx or 20xx)
        val yearRegex = Regex("\\b(19\\d{2}|20\\d{2})\\b")
        val yearMatch = yearRegex.find(clean)
        val year = yearMatch?.value?.toIntOrNull()

        if (yearMatch != null && !isTvShow) {
            // Cut title at year
            clean = clean.substring(0, yearMatch.range.first).trim()
        }

        // Remove garbage keywords
        val garbageKeywords = listOf(
            "BluRay", "WEBRip", "HDTV", "HDRip", "DVDRip", "PROPER", "REPACK",
            "AAC", "DTS", "5 1", "7 1", "REMUX", "x264", "x265", "GRP", "1080p", "720p", "2160p"
        )

        for (keyword in garbageKeywords) {
            clean = clean.replace(Regex("(?i)\\b$keyword\\b"), "").trim()
        }

        // Remove trailing/leading non-alphanumeric
        clean = clean.replace(Regex("\\s+"), " ").trim()

        return ParsedFileName(
            originalFileName = fileName,
            cleanTitle = clean.ifBlank { baseName },
            year = year,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            isTvShow = isTvShow,
            resolution = resolution,
            codec = codec
        )
    }
}
