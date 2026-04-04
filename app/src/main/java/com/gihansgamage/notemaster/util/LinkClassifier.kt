package com.gihansgamage.notemaster.util

import android.net.Uri
import com.gihansgamage.notemaster.data.local.entity.AttachmentType

object LinkClassifier {
    fun typeFor(url: String): AttachmentType {
        return if (extractYouTubeId(normalizeUrl(url)) != null) {
            AttachmentType.YOUTUBE
        } else {
            AttachmentType.WEB_LINK
        }
    }

    fun toYouTubeEmbedUrl(url: String): String? {
        return extractYouTubeId(normalizeUrl(url))?.let { videoId ->
            "https://www.youtube.com/embed/$videoId?controls=1&playsinline=1&rel=0"
        }
    }

    fun normalizeUrl(url: String): String {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return cleanUrl
        return if ("://" in cleanUrl) cleanUrl else "https://$cleanUrl"
    }

    private val youtubeRegex = Regex(
        "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?|shorts|live)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})",
        RegexOption.IGNORE_CASE
    )

    fun extractYouTubeId(url: String): String? {
        val matchResult = youtubeRegex.find(url)
        return matchResult?.groupValues?.get(1)
    }
}
