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
            "https://www.youtube.com/embed/$videoId"
        }
    }

    fun normalizeUrl(url: String): String {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return cleanUrl
        return if ("://" in cleanUrl) cleanUrl else "https://$cleanUrl"
    }

    fun extractYouTubeId(url: String): String? {
        return runCatching {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            when {
                host.contains("youtu.be") -> uri.pathSegments.firstOrNull()
                host.contains("youtube.com") && uri.pathSegments.firstOrNull() == "shorts" -> uri.pathSegments.getOrNull(1)
                host.contains("youtube.com") && uri.pathSegments.firstOrNull() == "embed" -> uri.pathSegments.getOrNull(1)
                host.contains("youtube.com") -> uri.getQueryParameter("v")
                else -> null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
