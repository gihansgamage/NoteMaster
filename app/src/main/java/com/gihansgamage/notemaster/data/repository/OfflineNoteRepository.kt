package com.gihansgamage.notemaster.data.repository

import com.gihansgamage.notemaster.data.local.NoteMasterDatabase
import com.gihansgamage.notemaster.data.local.dao.NoteWithRelations
import com.gihansgamage.notemaster.data.local.entity.AttachmentEntity
import com.gihansgamage.notemaster.data.local.entity.NoteEntity
import com.gihansgamage.notemaster.data.local.entity.NoteTagCrossRef
import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.local.entity.TagEntity
import com.gihansgamage.notemaster.data.model.AttachmentDraft
import com.gihansgamage.notemaster.data.model.EditableNote
import com.gihansgamage.notemaster.data.model.NoteDetails
import com.gihansgamage.notemaster.domain.summary.NoteSummarizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.util.UUID

class OfflineNoteRepository(
    private val database: NoteMasterDatabase,
    private val summarizer: NoteSummarizer,
) : NoteRepository {

    override fun observeNotes(): Flow<List<NoteDetails>> {
        return database.noteDao().observeAll().map { notes ->
            notes.map(::toNoteDetails)
        }
    }

    override fun observeSubjects(): Flow<List<SubjectEntity>> = database.subjectDao().observeAll()

    override fun observeSubject(subjectId: Long): Flow<SubjectEntity?> = database.subjectDao().observeById(subjectId)

    override fun observeNotesBySubject(subjectId: Long): Flow<List<NoteDetails>> {
        return database.noteDao().observeBySubject(subjectId).map { notes ->
            notes.map(::toNoteDetails)
        }
    }

    override fun observeNote(noteId: Long): Flow<NoteDetails?> {
        return database.noteDao().observeById(noteId).map { note ->
            note?.let(::toNoteDetails)
        }
    }

    override suspend fun getNote(noteId: Long): NoteDetails? {
        return database.noteDao().getById(noteId)?.let(::toNoteDetails)
    }

    override suspend fun saveNote(draft: EditableNote): Long {
        return database.withTransaction {
            val noteDao = database.noteDao()
            val tagDao = database.tagDao()
            val now = System.currentTimeMillis()
            val tagNames = parseTags(draft.tagsText)
            val summary = summarizer.buildSummary(
                title = draft.title,
                body = draft.body,
                attachments = draft.attachments,
                tagNames = tagNames,
            )

            val noteId = if (draft.id == null) {
                noteDao.insert(
                    NoteEntity(
                        title = draft.title.ifBlank { "Untitled note" },
                        body = draft.body,
                        subjectId = draft.subjectId,
                        summary = summary,
                        createdAt = now,
                        updatedAt = now,
                        isPinned = draft.isPinned,
                    ),
                )
            } else {
                val existing = noteDao.getEntityById(draft.id)
                    ?: error("Unable to update missing note ${draft.id}")
                noteDao.update(
                    existing.copy(
                        title = draft.title.ifBlank { "Untitled note" },
                        body = draft.body,
                        subjectId = draft.subjectId,
                        summary = summary,
                        updatedAt = now,
                        isPinned = draft.isPinned,
                    ),
                )
                draft.id
            }

            noteDao.deleteAttachmentsForNote(noteId)
            if (draft.attachments.isNotEmpty()) {
                noteDao.insertAttachments(
                    draft.attachments.map { attachment ->
                        AttachmentEntity(
                            noteId = noteId,
                            title = attachment.title,
                            uri = attachment.uri,
                            mimeType = attachment.mimeType,
                            type = attachment.type,
                            linkUrl = attachment.linkUrl,
                            content = attachment.content,
                            createdAt = now,
                        )
                    },
                )
            }

            noteDao.deleteTagRefsForNote(noteId)
            if (tagNames.isNotEmpty()) {
                val tagIds = mutableListOf<Long>()
                for (tagName in tagNames) {
                    tagIds.add(findOrCreateTag(tagName, tagDao))
                }
                noteDao.insertTagRefs(tagIds.map { tagId -> NoteTagCrossRef(noteId = noteId, tagId = tagId) })
            }

            noteId
        }
    }

    override suspend fun deleteNote(noteId: Long) {
        database.noteDao().deleteById(noteId)
    }

    override suspend fun togglePinned(noteId: Long) {
        database.noteDao().togglePinned(noteId = noteId, updatedAt = System.currentTimeMillis())
    }

    override suspend fun createSubject(name: String): SubjectEntity {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Subject name cannot be blank." }

        database.subjectDao().findByName(normalized)?.let { return it }

        val insertedId = database.subjectDao().insert(
            SubjectEntity(
                name = normalized,
                accentColorHex = palette[(normalized.hashCode().absoluteValue()) % palette.size],
            ),
        )

        return if (insertedId != -1L) {
            SubjectEntity(
                id = insertedId,
                name = normalized,
                accentColorHex = palette[(normalized.hashCode().absoluteValue()) % palette.size],
            )
        } else {
            database.subjectDao().findByName(normalized)
                ?: error("Unable to create subject $normalized")
        }
    }

    override suspend fun updateSubject(id: Long, name: String) {
        val subject = database.subjectDao().getAll().find { it.id == id }
        if (subject != null) {
            database.subjectDao().update(subject.copy(name = name.trim()))
        }
    }

    override suspend fun deleteSubject(id: Long) {
        database.subjectDao().deleteById(id)
    }

    override suspend fun ensureSeedData() {
        if (database.subjectDao().count() == 0) {
            database.subjectDao().insertAll(
                listOf(
                    SubjectEntity(name = "Biology", accentColorHex = "#DCEEE7"),
                    SubjectEntity(name = "History", accentColorHex = "#EFE8DA"),
                    SubjectEntity(name = "Design", accentColorHex = "#E7E6F4"),
                    SubjectEntity(name = "Work", accentColorHex = "#DDE7F5"),
                ),
            )
        }

        if (database.noteDao().count() == 0) {
            val subject = database.subjectDao().findByName("Biology")
            val draft = EditableNote(
                title = "Cell Structure Revision",
                body = """
                    # Cell Structure
                    ## Core ideas
                    Cells are the basic units of life and each organelle has a clear purpose.
                    
                    ## Key organelles
                    - Nucleus controls the cell activities.
                    - Mitochondria release energy through respiration.
                    - Ribosomes help build proteins.
                    
                    ## Exam reminder
                    Compare plant and animal cells in a short table before the test.
                """.trimIndent(),
                subjectId = subject?.id,
                tagsText = "#biology #cells #revision",
                attachments = listOf(
                    AttachmentDraft(
                        localId = UUID.randomUUID().toString(),
                        title = "Cell animation",
                        type = com.gihansgamage.notemaster.data.local.entity.AttachmentType.YOUTUBE,
                        linkUrl = "https://www.youtube.com/watch?v=URUJD5NEXC8",
                    ),
                    AttachmentDraft(
                        localId = UUID.randomUUID().toString(),
                        title = "Reference article",
                        type = com.gihansgamage.notemaster.data.local.entity.AttachmentType.WEB_LINK,
                        linkUrl = "https://en.wikipedia.org/wiki/Cell_(biology)",
                    ),
                ),
                isPinned = true,
            )
            saveNote(draft)
        }
    }

    override suspend fun deleteAllData() {
        database.noteDao().deleteAll()
        database.subjectDao().deleteAll()
        database.tagDao().deleteAll()
    }

    private suspend fun findOrCreateTag(name: String, tagDao: com.gihansgamage.notemaster.data.local.dao.TagDao): Long {
        tagDao.findByName(name)?.let { return it.id }
        val insertedId = tagDao.insert(TagEntity(name = name))
        return if (insertedId != -1L) {
            insertedId
        } else {
            tagDao.findByName(name)?.id ?: error("Unable to create tag $name")
        }
    }

    private fun toNoteDetails(note: NoteWithRelations): NoteDetails {
        return NoteDetails(
            id = note.note.id,
            title = note.note.title,
            body = note.note.body,
            summary = note.note.summary,
            subject = note.subject,
            tagNames = note.tags.map(TagEntity::name).sorted(),
            attachments = note.attachments.map { attachment ->
                AttachmentDraft(
                    localId = attachment.id.toString(),
                    title = attachment.title,
                    uri = attachment.uri,
                    mimeType = attachment.mimeType,
                    type = attachment.type,
                    linkUrl = attachment.linkUrl,
                    content = attachment.content,
                )
            },
            createdAt = note.note.createdAt,
            updatedAt = note.note.updatedAt,
            isPinned = note.note.isPinned,
        )
    }

    private fun parseTags(raw: String): List<String> {
        return raw
            .split(Regex("[,\\s]+"))
            .map { value -> value.removePrefix("#").trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun Int.absoluteValue(): Int = if (this < 0) -this else this

    private companion object {
        val palette = listOf("#DCEEE7", "#EFE8DA", "#DDE7F5", "#E6E0F5", "#F1E4E0", "#DDECE8")
    }
}
