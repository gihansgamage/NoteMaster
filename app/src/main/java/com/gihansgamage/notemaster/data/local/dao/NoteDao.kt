package com.gihansgamage.notemaster.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.gihansgamage.notemaster.data.local.entity.AttachmentEntity
import com.gihansgamage.notemaster.data.local.entity.NoteEntity
import com.gihansgamage.notemaster.data.local.entity.NoteTagCrossRef
import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

data class NoteWithRelations(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id",
    )
    val subject: SubjectEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
    )
    val attachments: List<AttachmentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteWithRelations>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun observeById(noteId: Long): Flow<NoteWithRelations?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getById(noteId: Long): NoteWithRelations?

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getEntityById(noteId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("UPDATE notes SET isPinned = NOT isPinned, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun togglePinned(noteId: Long, updatedAt: Long)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteTagRefsForNote(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRefs(refs: List<NoteTagCrossRef>)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
