package com.gihansgamage.notemaster.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.AttachmentDraft
import java.util.UUID

fun buildAttachmentDraft(
    context: Context,
    uri: Uri,
    fallbackType: AttachmentType = AttachmentType.DOCUMENT,
): AttachmentDraft {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri).orEmpty()
    val title = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "Attachment"
    val type = when {
        mimeType == "application/pdf" -> AttachmentType.PDF
        mimeType.startsWith("image/") -> AttachmentType.IMAGE
        mimeType.startsWith("video/") -> AttachmentType.VIDEO
        mimeType.startsWith("audio/") -> AttachmentType.AUDIO
        else -> fallbackType
    }

    return AttachmentDraft(
        localId = UUID.randomUUID().toString(),
        title = title,
        uri = uri.toString(),
        mimeType = mimeType,
        type = type,
    )
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    val cursor = context.contentResolver.query(uri, projection, null, null, null)
    return cursor.useSingleString()
}

private fun Cursor?.useSingleString(): String? {
    this ?: return null
    use { cursor ->
        return if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null
        }
    }
}
