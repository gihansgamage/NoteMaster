package com.gihansgamage.notemaster.data.repository

import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.model.EditableNote
import com.gihansgamage.notemaster.data.model.NoteDetails
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteDetails>>
    fun observeSubjects(): Flow<List<SubjectEntity>>
    fun observeNote(noteId: Long): Flow<NoteDetails?>
    suspend fun getNote(noteId: Long): NoteDetails?
    suspend fun saveNote(draft: EditableNote): Long
    suspend fun deleteNote(noteId: Long)
    suspend fun togglePinned(noteId: Long)
    suspend fun createSubject(name: String): SubjectEntity
    suspend fun ensureSeedData()
    suspend fun deleteAllData()
}
