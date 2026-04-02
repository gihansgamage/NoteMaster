package com.gihansgamage.notemaster.domain.summary

import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.AttachmentDraft

interface NoteSummarizer {
    fun buildSummary(
        title: String,
        body: String,
        attachments: List<AttachmentDraft>,
        tagNames: List<String>,
    ): String
}

class HeuristicNoteSummarizer : NoteSummarizer {
    override fun buildSummary(
        title: String,
        body: String,
        attachments: List<AttachmentDraft>,
        tagNames: List<String>,
    ): String {
        val cleanBody = body
            .replace(Regex("(?m)^#+\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val bodySummary = cleanBody
            .split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull { it.length > 24 }
            ?.take(180)
            ?: cleanBody.take(180)

        val attachmentLine = attachments
            .groupingBy { it.type }
            .eachCount()
            .entries
            .sortedBy { it.key.name }
            .joinToString(", ") { (type, count) ->
                when (type) {
                    AttachmentType.PDF -> "$count PDF"
                    AttachmentType.IMAGE -> "$count image"
                    AttachmentType.VIDEO -> "$count video clip"
                    AttachmentType.AUDIO -> "$count audio clip"
                    AttachmentType.TEXT -> "$count text"
                    AttachmentType.WEB_LINK -> "$count web link"
                    AttachmentType.YOUTUBE -> "$count YouTube link"
                } + if (count > 1) "s" else ""
            }
            .ifBlank { "" }

        val tagLine = tagNames
            .take(4)
            .joinToString(" ") { "#$it" }
            .ifBlank { "" }

        val intro = bodySummary.takeIf { it.isNotBlank() } ?: title.takeIf { it.isNotBlank() }

        return listOfNotNull(
            intro,
            attachmentLine.takeIf { it.isNotBlank() }?.let { "Includes $it." },
            tagLine.takeIf { it.isNotBlank() },
        ).joinToString("\n")
    }
}
