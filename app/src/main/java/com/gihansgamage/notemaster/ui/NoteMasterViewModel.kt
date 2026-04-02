package com.gihansgamage.notemaster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.local.entity.SubjectEntity
import com.gihansgamage.notemaster.data.model.AttachmentDraft
import com.gihansgamage.notemaster.data.model.EditableNote
import com.gihansgamage.notemaster.data.model.NoteDetails
import com.gihansgamage.notemaster.data.repository.NoteRepository
import com.gihansgamage.notemaster.data.repository.UserPreferencesRepository
import com.gihansgamage.notemaster.domain.summary.NoteSummarizer
import com.gihansgamage.notemaster.domain.toc.TocService
import com.gihansgamage.notemaster.util.LinkClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val notes: List<NoteDetails> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val searchQuery: String = "",
)

data class EditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val body: String = "",
    val subjectId: Long? = null,
    val tagsText: String = "",
    val attachments: List<AttachmentDraft> = emptyList(),
    val availableSubjects: List<SubjectEntity> = emptyList(),
    val summaryPreview: String = "",
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
    val suggestedTags: List<String> = emptyList(),
)

class NoteMasterViewModel(
    private val repository: NoteRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val summarizer: NoteSummarizer,
    private val tocService: TocService = TocService(),
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = com.gihansgamage.notemaster.data.repository.UserPreferences()
    )

    private val selectedSubjectId = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")
    private val editorDraft = MutableStateFlow(EditableNote())
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val isSaving = MutableStateFlow(false)

    val userMessages = messages.asSharedFlow()

    val subjects: StateFlow<List<SubjectEntity>> = repository.observeSubjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val suggestedTags: StateFlow<List<String>> = combine(
        editorDraft,
        repository.observeNotes()
    ) { draft, notes ->
        notes.filter { it.subject?.id == draft.subjectId }
            .flatMap { it.tagNames }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val homeUiState: StateFlow<HomeUiState> = combine(
        repository.observeNotes(),
        subjects,
        selectedSubjectId,
        searchQuery,
    ) { notes, subjectsList, selectedId, query ->
        val filtered = notes.filter { note ->
            val subjectMatches = selectedId == null || note.subject?.id == selectedId
            val textMatches = query.isBlank() || buildSearchableText(note).contains(query.trim(), ignoreCase = true)
            subjectMatches && textMatches
        }
        HomeUiState(
            notes = filtered,
            subjects = subjectsList,
            selectedSubjectId = selectedId,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    val editorUiState: StateFlow<EditorUiState> = combine(
        editorDraft, 
        subjects, 
        isSaving, 
        suggestedTags
    ) { draft, subjectsList, saving, suggested ->
        EditorUiState(
            noteId = draft.id,
            title = draft.title,
            body = draft.body,
            subjectId = draft.subjectId,
            tagsText = draft.tagsText,
            attachments = draft.attachments,
            availableSubjects = subjectsList,
            summaryPreview = summarizer.buildSummary(
                title = draft.title,
                body = draft.body,
                attachments = draft.attachments,
                tagNames = parseTags(draft.tagsText),
            ),
            isPinned = draft.isPinned,
            isSaving = saving,
            suggestedTags = suggested,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun selectSubjectFilter(subjectId: Long?) {
        selectedSubjectId.value = subjectId
    }

    fun startNewNote() {
        editorDraft.value = EditableNote()
    }

    fun loadNoteIntoEditor(noteId: Long?) {
        if (noteId == null || noteId <= 0L) {
            startNewNote()
            return
        }

        viewModelScope.launch {
            val note = repository.getNote(noteId)
            editorDraft.value = if (note != null) {
                EditableNote(
                    id = note.id,
                    title = note.title,
                    body = note.body,
                    subjectId = note.subject?.id,
                    tagsText = note.tagNames.joinToString(", "),
                    attachments = note.attachments,
                    isPinned = note.isPinned,
                )
            } else {
                EditableNote()
            }
        }
    }

    fun updateTitle(value: String) {
        editorDraft.value = editorDraft.value.copy(title = value)
    }

    fun updateBody(value: String) {
        editorDraft.value = editorDraft.value.copy(body = value)
    }

    fun updateTags(value: String) {
        editorDraft.value = editorDraft.value.copy(tagsText = value)
    }

    fun selectSubject(subjectId: Long?) {
        editorDraft.value = editorDraft.value.copy(subjectId = subjectId)
    }

    fun togglePinnedInEditor() {
        editorDraft.value = editorDraft.value.copy(isPinned = !editorDraft.value.isPinned)
    }

    fun addAttachment(attachment: AttachmentDraft) {
        editorDraft.value = editorDraft.value.copy(
            attachments = editorDraft.value.attachments + attachment,
        )
        viewModelScope.launch {
            messages.emit("Material added successfully!")
        }
    }

    fun addLink(title: String, url: String) {
        val cleanUrl = LinkClassifier.normalizeUrl(url)
        if (cleanUrl.isBlank()) return

        addAttachment(
            AttachmentDraft(
                localId = UUID.randomUUID().toString(),
                title = title.ifBlank { cleanUrl },
                type = LinkClassifier.typeFor(cleanUrl),
                linkUrl = cleanUrl,
            ),
        )
    }

    fun addTextMaterial(title: String, content: String) {
        addAttachment(
            AttachmentDraft(
                localId = UUID.randomUUID().toString(),
                title = title.ifBlank { "Text Material" },
                type = AttachmentType.TEXT,
                content = content,
            ),
        )
    }

    fun removeAttachment(localId: String) {
        editorDraft.value = editorDraft.value.copy(
            attachments = editorDraft.value.attachments.filterNot { it.localId == localId },
        )
    }

    fun saveCurrentNote(onSaved: (Long) -> Unit = {}) {
        val snapshot = editorDraft.value
        if (snapshot.title.isBlank() && snapshot.body.isBlank() && snapshot.attachments.isEmpty()) {
            messages.tryEmit("Add a title, text, or attachment before saving.")
            return
        }

        viewModelScope.launch {
            val noteId = repository.saveNote(snapshot)
            editorDraft.value = editorDraft.value.copy(id = noteId)
            messages.emit("Note saved.")
            onSaved(noteId)
        }
    }

    fun deleteNote(noteId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            messages.emit("Note deleted.")
            onDeleted()
        }
    }

    fun togglePinned(noteId: Long) {
        viewModelScope.launch {
            repository.togglePinned(noteId)
        }
    }

    fun createSubject(name: String) {
        viewModelScope.launch {
            val subject = repository.createSubject(name)
            editorDraft.value = editorDraft.value.copy(subjectId = subject.id)
            messages.emit("Subject added.")
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            repository.deleteSubject(id)
            messages.emit("Notebook deleted.")
        }
    }

    fun updateSubject(id: Long, name: String) {
        viewModelScope.launch {
            repository.updateSubject(id, name)
            messages.emit("Notebook renamed.")
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserName(name)
        }
    }

    fun updateDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDarkMode(isDark)
        }
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserName(name)
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            messages.emit("All data deleted.")
        }
    }

    fun noteDetails(id: Long): Flow<NoteDetails?> = repository.observeNote(id)

    fun observeSubject(id: Long) = repository.observeSubject(id)

    fun observeNotesBySubject(id: Long) = repository.observeNotesBySubject(id)

    fun getToc(body: String) = tocService.extractHeaders(body)

    private fun buildSearchableText(note: NoteDetails): String {
        return listOf(
            note.title,
            note.body,
            note.summary,
            note.subject?.name.orEmpty(),
            note.tagNames.joinToString(" "),
            note.attachments.joinToString(" ") { "${it.title} ${it.content.orEmpty()}" }
        ).joinToString(" ")
    }

    private fun parseTags(raw: String): List<String> {
        return raw
            .split(Regex("[,\\s]+"))
            .map { it.removePrefix("#").trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
