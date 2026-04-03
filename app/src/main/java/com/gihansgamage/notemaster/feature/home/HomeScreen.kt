package com.gihansgamage.notemaster.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gihansgamage.notemaster.feature.home.components.*
import com.gihansgamage.notemaster.ui.HomeUiState
import com.gihansgamage.notemaster.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchChange: (String) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    userName: String,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onEditNote: (Long) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onToggleSubjectPinned: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateSubject: (String) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onRenameSubject: (Long, String) -> Unit,
) {
    var showSubjectDialog by remember { mutableStateOf(false) }
    var subjectToRename by remember { mutableStateOf<com.gihansgamage.notemaster.data.local.entity.SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<com.gihansgamage.notemaster.data.local.entity.SubjectEntity?>(null) }
    
    if (showSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showSubjectDialog = false },
            onConfirm = { name ->
                onCreateSubject(name)
                showSubjectDialog = false
            }
        )
    }

    if (subjectToRename != null) {
        RenameSubjectDialog(
            currentName = subjectToRename?.name.orEmpty(),
            onDismiss = { subjectToRename = null },
            onConfirm = { newName ->
                subjectToRename?.let { onRenameSubject(it.id, newName) }
                subjectToRename = null
            }
        )
    }

    if (subjectToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { androidx.compose.material3.Text("Delete Notebook?") },
            text = { androidx.compose.material3.Text("This will permanently delete the '${subjectToDelete?.name}' notebook and ALL its materials. This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        subjectToDelete?.let { onDeleteSubject(it.id) }
                        subjectToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error)
                ) {
                    androidx.compose.material3.Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { subjectToDelete = null }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSubjectDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Notebook")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val isSearching = uiState.searchQuery.isNotBlank()
        val pinnedNotes = uiState.notes.filter { it.isPinned }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeHeader(
                    userName = userName,
                    onSettingsClick = onOpenSettings
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { 
                        Text(
                            "Search notes, tags, or summaries",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Rounded.Search, 
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 20.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        if (isSearching) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )
            }

            if (isSearching) {
                if (uiState.subjects.isNotEmpty()) {
                    item {
                        NotebookSection(
                            subjects = uiState.subjects,
                            notes = uiState.notes,
                            selectedSubjectId = uiState.selectedSubjectId,
                            onSelectSubject = onSelectSubject,
                            onTogglePinned = onToggleSubjectPinned,
                            onCreateSubject = { showSubjectDialog = true },
                            onDeleteSubject = { subjectToDelete = it },
                            onRenameSubject = { subjectToRename = it }
                        )
                    }
                }

                item {
                    Text(
                        text = "Matching Materials",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (uiState.notes.isEmpty() && uiState.subjects.isEmpty()) {
                    item {
                        EmptyNotesCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            hasUnpinnedNotes = false
                        )
                    }
                } else if (uiState.notes.isEmpty()) {
                    item {
                        Text(
                            text = "No materials match this search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { onOpenNote(note.id) },
                            onEdit = { onEditNote(note.id) },
                            onTogglePinned = { onTogglePinned(note.id) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            } else {
                item {
                    NotebookSection(
                        subjects = uiState.subjects,
                        notes = uiState.notes,
                        selectedSubjectId = uiState.selectedSubjectId,
                        onSelectSubject = onSelectSubject,
                        onTogglePinned = onToggleSubjectPinned,
                        onCreateSubject = { showSubjectDialog = true },
                        onDeleteSubject = { subjectToDelete = it },
                        onRenameSubject = { subjectToRename = it }
                    )
                }

                item {
                    HeroSection(
                        materialCount = uiState.totalMaterialsCount,
                        subjectCount = uiState.subjects.size,
                    )
                }

                item {
                    Text(
                        text = "Pinned Materials",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (pinnedNotes.isEmpty()) {
                    item {
                        val hasUnpinnedNotes = uiState.notes.isNotEmpty()
                        EmptyNotesCard(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            hasUnpinnedNotes = hasUnpinnedNotes
                        )
                    }
                } else {
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { onOpenNote(note.id) },
                            onEdit = { onEditNote(note.id) },
                            onTogglePinned = { onTogglePinned(note.id) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}
