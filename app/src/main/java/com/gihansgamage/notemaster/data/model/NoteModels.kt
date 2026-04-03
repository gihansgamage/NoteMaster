package com.gihansgamage.notemaster.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.local.entity.SubjectEntity

@Immutable
data class AttachmentDraft(
    val localId: String,
    val title: String,
    val uri: String? = null,
    val mimeType: String = "",
    val type: AttachmentType,
    val linkUrl: String? = null,
    val content: String? = null,
)

@Stable
data class EditableNote(
    val id: Long? = null,
    val title: String = "",
    val body: String = "",
    val subjectId: Long? = null,
    val tagsText: String = "",
    val attachments: List<AttachmentDraft> = emptyList(),
    val isPinned: Boolean = false,
)

@Immutable
data class NoteDetails(
    val id: Long,
    val title: String,
    val body: String,
    val summary: String,
    val subject: SubjectEntity?,
    val tagNames: List<String>,
    val attachments: List<AttachmentDraft>,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
)
