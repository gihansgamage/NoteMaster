package com.gihansgamage.notemaster.data.repository

import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.model.EditableNote
import com.gihansgamage.notemaster.data.model.NoteDetails
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteDetails>>
    fun observeSubjects(): Flow<List<SubjectEntity>>
    fun observeSubject(subjectId: Long): Flow<SubjectEntity?>
    fun observeNote(noteId: Long): Flow<NoteDetails?>
    fun observeNotesBySubject(subjectId: Long): Flow<List<NoteDetails>>
    suspend fun getNote(noteId: Long): NoteDetails?
    suspend fun saveNote(draft: EditableNote): Long
    suspend fun deleteNote(noteId: Long)
    suspend fun togglePinned(noteId: Long)
    suspend fun toggleSubjectPinned(id: Long)
    suspend fun createSubject(name: String): SubjectEntity
    suspend fun updateSubject(id: Long, name: String)
    suspend fun deleteSubject(id: Long)
    suspend fun updateAttachmentContent(attachmentId: Long, newContent: String)
    suspend fun ensureSeedData()
    suspend fun deleteAllData()
}
