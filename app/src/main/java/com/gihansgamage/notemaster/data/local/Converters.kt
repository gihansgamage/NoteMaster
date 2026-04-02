package com.gihansgamage.notemaster.data.local

import androidx.room.TypeConverter
import com.gihansgamage.notemaster.data.local.entity.AttachmentType

class Converters {
    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType {
        return try {
            if (value == "DOCUMENT") AttachmentType.TEXT
            else AttachmentType.valueOf(value)
        } catch (e: Exception) {
            AttachmentType.TEXT // Safe fallback
        }
    }
}
