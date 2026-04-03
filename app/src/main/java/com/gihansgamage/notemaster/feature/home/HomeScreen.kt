package com.gihansgamage.notemaster.feature.home

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.gihansgamage.notemaster.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.NoteDetails
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
        topBar = {},
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
                // --- SEARCH RESULTS VIEW ---
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
                            hasUnpinnedNotes = false // Use properties to show appropriate message
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
                // --- NORMAL VIEW ---
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

@Composable
private fun HeroSection(
    materialCount: Int,
    subjectCount: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "A quieter way to study and store ideas",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Subjects, hashtags, PDFs, links, images, and audio all stay attached to the same note.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(label = "$materialCount total materials")
                StatPill(label = "$subjectCount subjects")
            }
        }
    }
}

@Composable
private fun StatPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun NotebookSection(
    subjects: List<com.gihansgamage.notemaster.data.local.entity.SubjectEntity>,
    notes: List<com.gihansgamage.notemaster.data.model.NoteDetails>,
    selectedSubjectId: Long?,
    onSelectSubject: (Long?) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onCreateSubject: () -> Unit,
    onDeleteSubject: (com.gihansgamage.notemaster.data.local.entity.SubjectEntity) -> Unit,
    onRenameSubject: (com.gihansgamage.notemaster.data.local.entity.SubjectEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Notebooks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // The user wants a "create notebook" button here
            TextButton(
                onClick = onCreateSubject,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Create Notebook", fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                NotebookCard(
                    name = "All Notes",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    materialCount = notes.sumOf { it.attachments.size },
                    selected = selectedSubjectId == null,
                    onClick = { onSelectSubject(null) }
                )
            }
            items(subjects, key = { it.id }) { subject ->
                NotebookCard(
                    name = subject.name,
                    color = subject.accentColorHex.toColorOrFallback(),
                    materialCount = notes.filter { it.subject?.id == subject.id }.sumOf { it.attachments.size },
                    selected = selectedSubjectId == subject.id,
                    isPinned = subject.isPinned,
                    onClick = { onSelectSubject(subject.id) },
                    onDelete = { onDeleteSubject(subject) },
                    onRename = { onRenameSubject(subject) },
                    onTogglePinned = { onTogglePinned(subject.id) }
                )
            }
        }
    }
}

@Composable
private fun NotebookCard(
    name: String,
    color: Color,
    materialCount: Int,
    selected: Boolean,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onTogglePinned: (() -> Unit)? = null,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(width = 130.dp, height = 160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else color.copy(alpha = 0.5f)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isPinned) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(14.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                if (onDelete != null || onRename != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Menu",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onTogglePinned != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isPinned) "Unpin from top" else "Pin to top") },
                                    onClick = {
                                        showMenu = false
                                        onTogglePinned()
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            if (isPinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    }
                                )
                            }
                            if (onRename != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$materialCount Materials",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteDetails,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onTogglePinned: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(note.subject?.accentColorHex.toColorOrFallback()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = attachmentLeadIcon(note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = note.subject?.name ?: "No subject",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row {
                    IconButton(onClick = onTogglePinned) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit note")
                    }
                }
            }

            if (note.summary.isNotBlank()) {
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (note.tagNames.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(note.tagNames.take(5), key = { it }) { tag ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AttachmentSummaryRow(note = note)
                Text(
                    text = formatDateTime(note.updatedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AttachmentSummaryRow(note: NoteDetails) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (note.attachments.isEmpty()) {
            Icon(
                imageVector = Icons.Rounded.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Text note",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            note.attachments.take(3).forEach { attachment ->
                Icon(
                    imageVector = when (attachment.type) {
                        AttachmentType.PDF -> Icons.Rounded.PictureAsPdf
                        AttachmentType.VIDEO -> Icons.Rounded.PlayCircleOutline
                        AttachmentType.YOUTUBE -> Icons.Rounded.PlayCircleOutline
                        else -> Icons.Rounded.Description
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${note.attachments.size} attachment${if (note.attachments.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyNotesCard(
    modifier: Modifier = Modifier,
    hasUnpinnedNotes: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (hasUnpinnedNotes) "No pinned materials" else "No notes yet",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (hasUnpinnedNotes) "Pin your most important materials to see them here on your Home feed." else "Tap the + button to create your first subject-based note with files, hashtags, and links.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun attachmentLeadIcon(note: NoteDetails) = when {
    note.attachments.any { it.type == AttachmentType.PDF } -> Icons.Rounded.PictureAsPdf
    note.attachments.any { it.type == AttachmentType.YOUTUBE } -> Icons.Rounded.SmartDisplay
    note.attachments.any { it.type == AttachmentType.VIDEO } -> Icons.Rounded.PlayCircle
    else -> Icons.Rounded.Description
}

private fun String?.toColorOrFallback(): Color {
    return runCatching {
        Color(parseColor(this ?: "#F1F5F9"))
    }.getOrDefault(Color(0xFFF1F5F9))
}

@Composable
private fun HomeHeader(
    userName: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    text = getGreeting(userName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your smart note companion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGreeting(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return if (name.isNotBlank()) "$timeGreeting, ${name.trim()}" else "Note Master"
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Notebook") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Notebook Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameSubjectDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Notebook") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Notebook Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
