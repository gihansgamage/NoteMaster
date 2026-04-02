package com.gihansgamage.notemaster.util

import android.net.Uri
import com.gihansgamage.notemaster.data.local.entity.AttachmentType

object LinkClassifier {
    fun typeFor(url: String): AttachmentType {
        return if (extractYouTubeId(url) != null) {
            AttachmentType.YOUTUBE
        } else {
            AttachmentType.WEB_LINK
        }
    }

    fun toYouTubeEmbedUrl(url: String): String? {
        return extractYouTubeId(url)?.let { videoId ->
            "https://www.youtube.com/embed/$videoId"
        }
    }

    private fun extractYouTubeId(url: String): String? {
        return runCatching {
            val uri = Uri.parse(url)
            when {
                uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
                uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v")
                uri.host?.contains("youtube.com") == true && uri.path?.contains("/shorts/") == true -> {
                    uri.pathSegments.lastOrNull()
                }

                else -> null
            }
        }.getOrNull()
    }
}
