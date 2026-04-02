package com.gihansgamage.notemaster.data.local

import androidx.room.TypeConverter
import com.gihansgamage.notemaster.data.local.entity.AttachmentType

class Converters {
    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)
}
