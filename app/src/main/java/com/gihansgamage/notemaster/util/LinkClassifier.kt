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

    fun extractYouTubeId(url: String): String? {
        return runCatching {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            
            when {
                // Handle youtu.be/VIDEO_ID
                host.contains("youtu.be") -> uri.pathSegments.firstOrNull()
                
                // Handle youtube.com (m.youtube.com, etc.)
                host.contains("youtube.com") -> {
                    val segments = uri.pathSegments
                    when (segments.firstOrNull()) {
                        "shorts", "embed", "live", "v" -> segments.getOrNull(1)
                        else -> uri.getQueryParameter("v")
                    }
                }
                else -> null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
