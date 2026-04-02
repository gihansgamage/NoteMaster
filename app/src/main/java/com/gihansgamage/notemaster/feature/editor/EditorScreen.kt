package com.gihansgamage.notemaster.feature.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.AttachmentDraft
import com.gihansgamage.notemaster.ui.EditorUiState
import com.gihansgamage.notemaster.util.buildAttachmentDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onCreateSubject: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onAddAttachment: (AttachmentDraft) -> Unit,
    onAddLink: (String, String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    val pdfPicker = rememberAttachmentPicker(fallbackType = AttachmentType.PDF, onAddAttachment = onAddAttachment)
    val imagePicker = rememberAttachmentPicker(fallbackType = AttachmentType.IMAGE, onAddAttachment = onAddAttachment)
    val audioPicker = rememberAttachmentPicker(fallbackType = AttachmentType.AUDIO, onAddAttachment = onAddAttachment)
    val videoPicker = rememberAttachmentPicker(fallbackType = AttachmentType.VIDEO, onAddAttachment = onAddAttachment)
    val documentPicker = rememberAttachmentPicker(fallbackType = AttachmentType.DOCUMENT, onAddAttachment = onAddAttachment)

    if (showLinkDialog) {
        AddLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { title, url ->
                onAddLink(title, url)
                showLinkDialog = false
            },
        )
    }

    if (showSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showSubjectDialog = false },
            onConfirm = { subject ->
                onCreateSubject(subject)
                showSubjectDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(if (uiState.noteId == null) "New note" else "Edit note")
                        Text(
                            text = "Markdown headings become the table of contents automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pin note",
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                windowInsets = WindowInsets.statusBars,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
            }

            item {
                SubjectSelector(
                    uiState = uiState,
                    onSelectSubject = onSelectSubject,
                    onAddSubject = { showSubjectDialog = true },
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Quick add",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                AssistChip(
                                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                                    label = { Text("PDF") },
                                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                                    label = { Text("Image") },
                                    leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                    label = { Text("Audio") },
                                    leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null) },
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { videoPicker.launch(arrayOf("video/*")) },
                                    label = { Text("Video") },
                                    leadingIcon = { Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null) },
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        documentPicker.launch(
                                            arrayOf(
                                                "application/*",
                                                "text/plain",
                                            ),
                                        )
                                    },
                                    label = { Text("Document") },
                                    leadingIcon = { Icon(Icons.Rounded.Subject, contentDescription = null) },
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { showLinkDialog = true },
                                    label = { Text("Web / YouTube") },
                                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.tagsText,
                    onValueChange = onTagsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Hashtags") },
                    supportingText = { Text("Use values like #biology #exam or comma-separated tags") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = false,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    label = { Text("Write your note") },
                    shape = RoundedCornerShape(24.dp),
                )
            }

            item {
                SummaryPreviewCard(summary = uiState.summaryPreview)
            }

            if (uiState.attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "Attachments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(uiState.attachments, key = { it.localId }) { attachment ->
                    AttachmentEditorRow(
                        attachment = attachment,
                        onRemove = { onRemoveAttachment(attachment.localId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAttachmentPicker(
    fallbackType: AttachmentType,
    onAddAttachment: (AttachmentDraft) -> Unit,
) = LocalContext.current.let { context ->
    rememberLauncherForActivityResult(contract = OpenDocument()) { uri ->
    if (uri != null) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        onAddAttachment(buildAttachmentDraft(context, uri, fallbackType))
    }
}
}

@Composable
private fun SubjectSelector(
    uiState: EditorUiState,
    onSelectSubject: (Long?) -> Unit,
    onAddSubject: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = onAddSubject) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("New subject")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    FilterChip(
                        selected = uiState.subjectId == null,
                        onClick = { onSelectSubject(null) },
                        label = { Text("Unsorted") },
                    )
                }
                items(uiState.availableSubjects, key = { it.id }) { subject ->
                    FilterChip(
                        selected = uiState.subjectId == subject.id,
                        onClick = { onSelectSubject(subject.id) },
                        label = { Text(subject.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryPreviewCard(summary: String) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Auto summary preview",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (summary.isBlank()) {
                    "As you write, Note Master prepares a clean summary using text, tags, and attachment types."
                } else {
                    summary
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AttachmentEditorRow(
    attachment: AttachmentDraft,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = attachment.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = when (attachment.type) {
                        AttachmentType.PDF -> "PDF file"
                        AttachmentType.IMAGE -> "Image"
                        AttachmentType.VIDEO -> "Video"
                        AttachmentType.AUDIO -> "Audio clip"
                        AttachmentType.DOCUMENT -> "Document"
                        AttachmentType.WEB_LINK -> "Web link"
                        AttachmentType.YOUTUBE -> "YouTube link"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                attachment.linkUrl?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove attachment")
            }
        }
    }
}

@Composable
private fun AddLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add web or YouTube link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, url) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create subject") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Subject name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
