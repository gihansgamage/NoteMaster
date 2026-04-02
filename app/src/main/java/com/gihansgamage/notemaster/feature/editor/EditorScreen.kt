package com.gihansgamage.notemaster.feature.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.AttachmentDraft
import com.gihansgamage.notemaster.ui.EditorUiState
import com.gihansgamage.notemaster.util.buildAttachmentDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onCreateSubject: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onAddAttachment: (AttachmentDraft) -> Unit,
    onAddTextMaterial: (String, String) -> Unit,
    onAddLink: (String, String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    val pdfPicker = rememberAttachmentPicker(fallbackType = AttachmentType.PDF, onAddAttachment = onAddAttachment)
    val imagePicker = rememberAttachmentPicker(fallbackType = AttachmentType.IMAGE, onAddAttachment = onAddAttachment)
    val audioPicker = rememberAttachmentPicker(fallbackType = AttachmentType.AUDIO, onAddAttachment = onAddAttachment)
    val videoPicker = rememberAttachmentPicker(fallbackType = AttachmentType.VIDEO, onAddAttachment = onAddAttachment)
    val textPicker = rememberAttachmentPicker(fallbackType = AttachmentType.TEXT, onAddAttachment = onAddAttachment)

    if (showTextDialog) {
        AddTextDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { title, content ->
                onAddTextMaterial(title, content)
                showTextDialog = false
            },
        )
    }

    if (showLinkDialog) {
        AddLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { title, url ->
                onAddLink(title, url)
                showLinkDialog = false
            }
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
                            imageVector = if (uiState.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (uiState.isPinned) "Unpin note" else "Pin note",
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save")
                    }
                    if (uiState.noteId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                windowInsets = WindowInsets.statusBars,
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
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
                    placeholder = { Text("Enter a descriptive title...") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
            }

            // Subject Selector - Only show if not pre-locked from a notebook
            if (uiState.noteId == null && uiState.subjectId == null) {
                item {
                    SubjectSelector(
                        uiState = uiState,
                        onSelectSubject = onSelectSubject,
                        onAddSubject = { showSubjectDialog = true },
                    )
                }
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
                            text = "Add material",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
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
                                        showTextDialog = true
                                    },
                                    label = { Text("Text") },
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

            // MATERIALS LIST - Moved up!
            if (uiState.attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "Added materials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.attachments, key = { it.localId }) { attachment ->
                    AttachmentEditorRow(
                        attachment = attachment,
                        onRemove = { onRemoveAttachment(attachment.localId) },
                    )
                }
            }

            item {
                Column {
                    OutlinedTextField(
                        value = uiState.tagsText,
                        onValueChange = onTagsChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hashtags") },
                        supportingText = { Text("Separate tags by comma (e.g. biology, exam)") },
                        shape = RoundedCornerShape(22.dp),
                        singleLine = false,
                    )
                    if (uiState.suggestedTags.isNotEmpty()) {
                        val currentTags = uiState.tagsText.split(Regex("[,\\s]+")).map { it.trim().lowercase() }.filter { it.isNotBlank() }
                        val unusedTags = uiState.suggestedTags.filter { it !in currentTags }
                        if (unusedTags.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            ) {
                                items(unusedTags) { tag ->
                                    AssistChip(
                                        onClick = {
                                            val currentText = uiState.tagsText.trimEnd()
                                            val separator = if (currentText.isEmpty() || currentText.endsWith(",")) "" else ", "
                                            onTagsChange(currentText + separator + tag)
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    label = { Text("Description") },
                    placeholder = { Text("Write your detailed description here...") },
                    shape = RoundedCornerShape(24.dp),
                )
            }

            item {
                SummaryPreviewCard(summary = uiState.summaryPreview)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Material?") },
            text = { Text("This will permanently delete this material and all its data. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                        AttachmentType.TEXT -> "Text Material"
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

@Composable
private fun AddTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var textValue by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormattingToolbar(
                    textValue = textValue,
                    onValueChange = { textValue = it }
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textValue.text.isNotBlank()) {
                        val derivedTitle = textValue.text.lines().firstOrNull { it.isNotBlank() }?.take(40) ?: ""
                        onConfirm(derivedTitle, textValue.text)
                    }
                },
            ) {
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
private fun FormattingToolbar(
    textValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modifiers: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>> = listOf(
            Icons.Rounded.FormatBold to "**",
            Icons.Rounded.FormatItalic to "_",
            Icons.Rounded.AutoFixHigh to "==",
            Icons.Rounded.FormatListBulleted to "\n• ",
            Icons.Rounded.FormatListNumbered to "\n1. "
        )

        modifiers.forEach { (icon: androidx.compose.ui.graphics.vector.ImageVector, symbol: String) ->
            IconButton(
                onClick = {
                    val selection = textValue.selection
                    val text = textValue.text
                    val newText = if (selection.collapsed) {
                        text.substring(0, selection.start) + symbol + symbol + text.substring(selection.end)
                    } else {
                        text.substring(0, selection.start) + symbol + text.substring(selection.start, selection.end) + symbol + text.substring(selection.end)
                    }
                    val newSelection = if (selection.collapsed) {
                        TextRange(selection.start + symbol.length)
                    } else {
                        TextRange(selection.end + symbol.length * 2)
                    }
                    onValueChange(textValue.copy(text = newText, selection = newSelection))
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
