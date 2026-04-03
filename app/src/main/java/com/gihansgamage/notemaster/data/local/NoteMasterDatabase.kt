package com.gihansgamage.notemaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gihansgamage.notemaster.data.local.dao.NoteDao
import com.gihansgamage.notemaster.data.local.dao.SubjectDao
import com.gihansgamage.notemaster.data.local.dao.TagDao
import com.gihansgamage.notemaster.data.local.entity.AttachmentEntity
import com.gihansgamage.notemaster.data.local.entity.NoteEntity
import com.gihansgamage.notemaster.data.local.entity.NoteTagCrossRef
import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.local.entity.TagEntity

@Database(
    entities = [
        SubjectEntity::class,
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AttachmentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class NoteMasterDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun subjectDao(): SubjectDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var instance: NoteMasterDatabase? = null

        fun getDatabase(context: Context): NoteMasterDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = NoteMasterDatabase::class.java,
                    name = "note_master.db",
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
