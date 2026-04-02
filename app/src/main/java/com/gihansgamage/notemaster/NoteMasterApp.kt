package com.gihansgamage.notemaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.NoteDetails
import com.gihansgamage.notemaster.feature.detail.NoteDetailScreen
import com.gihansgamage.notemaster.feature.editor.EditorScreen
import com.gihansgamage.notemaster.feature.home.HomeScreen
import com.gihansgamage.notemaster.feature.viewer.PdfViewerScreen
import com.gihansgamage.notemaster.feature.viewer.VideoViewerScreen
import com.gihansgamage.notemaster.feature.viewer.WebMediaScreen
import com.gihansgamage.notemaster.ui.AppViewModelProvider
import com.gihansgamage.notemaster.ui.NoteMasterViewModel
import com.gihansgamage.notemaster.ui.theme.NoteMasterTheme
import kotlinx.coroutines.flow.collectLatest
import com.gihansgamage.notemaster.feature.subject.SubjectDetailScreen
import com.gihansgamage.notemaster.feature.viewer.AudioPlayerScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn

private object Destination {
    const val Home = "home"
    const val Editor = "editor?noteId={noteId}"
    const val Detail = "detail/{noteId}"
    const val Pdf = "pdf?title={title}&uri={uri}"
    const val Video = "video?title={title}&uri={uri}"
    const val Web = "web?title={title}&url={url}"
    const val Audio = "audio?title={title}&uri={uri}"
    const val SubjectDetail = "subject_detail/{subjectId}"
    const val Welcome = "welcome"
    const val Settings = "settings"


    fun editor(noteId: Long? = null): String = "editor?noteId=${noteId ?: -1L}"
    fun detail(noteId: Long): String = "detail/$noteId"
    fun pdf(title: String, uri: String): String = "pdf?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun video(title: String, uri: String): String = "video?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun web(title: String, url: String): String = "web?title=${Uri.encode(title)}&url=${Uri.encode(url)}"
    fun audio(title: String, uri: String): String = "audio?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun subjectDetail(subjectId: Long): String = "subject_detail/$subjectId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteMasterApp(
    viewModel: NoteMasterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val navController = rememberNavController()
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val editorUiState by viewModel.editorUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    NoteMasterTheme(
        darkTheme = userPreferences.isDarkMode,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val startDestination = if (userPreferences.isOnboardingCompleted) Destination.Home else Destination.Welcome

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(Destination.Home) {
                    HomeScreen(
                        uiState = homeUiState,
                        onSearchChange = viewModel::updateSearchQuery,
                        onSelectSubject = { subjectId ->
                            if (subjectId != null) {
                                navController.navigate(Destination.subjectDetail(subjectId))
                            } else {
                                viewModel.selectSubjectFilter(null)
                            }
                        },
                        userName = userPreferences.userName,
                        onCreateNote = {
                            viewModel.startNewNote()
                            navController.navigate(Destination.editor())
                        },
                        onOpenNote = { noteId ->
                            navController.navigate(Destination.detail(noteId))
                        },
                        onEditNote = { noteId ->
                            navController.navigate(Destination.editor(noteId))
                        },
                        onTogglePinned = viewModel::togglePinned,
                        onOpenSettings = {
                            navController.navigate(Destination.Settings)
                        },
                        onCreateSubject = viewModel::createSubject,
                        onDeleteSubject = viewModel::deleteSubject,
                        onRenameSubject = viewModel::updateSubject,
                    )
                }

                composable(Destination.Welcome) {
                    com.gihansgamage.notemaster.feature.onboarding.WelcomeScreen(
                        onComplete = { name ->
                            viewModel.completeOnboarding(name)
                            navController.navigate(Destination.Home) {
                                popUpTo(Destination.Welcome) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Destination.Settings) {
                    com.gihansgamage.notemaster.feature.settings.SettingsScreen(
                        userName = userPreferences.userName,
                        isDarkMode = userPreferences.isDarkMode,
                        onNameChange = viewModel::updateUserName,
                        onThemeToggle = viewModel::updateDarkMode,
                        onDeleteAllData = viewModel::deleteAllData,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Destination.Editor,
                    arguments = listOf(
                        navArgument("noteId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { backStackEntry ->
                    val noteIdArg = backStackEntry.arguments?.getLong("noteId") ?: -1L

                    LaunchedEffect(noteIdArg) {
                        if (noteIdArg > 0L && editorUiState.noteId != noteIdArg) {
                            viewModel.loadNoteIntoEditor(noteIdArg)
                        }
                    }

                    EditorScreen(
                        uiState = editorUiState,
                        onBack = { navController.popBackStack() },
                        onTitleChange = viewModel::updateTitle,
                        onBodyChange = viewModel::updateBody,
                        onTagsChange = viewModel::updateTags,
                        onSelectSubject = viewModel::selectSubject,
                        onCreateSubject = viewModel::createSubject,
                        onTogglePinned = viewModel::togglePinnedInEditor,
                        onAddAttachment = viewModel::addAttachment,
                        onAddLink = viewModel::addLink,
                        onRemoveAttachment = viewModel::removeAttachment,
                        onSave = {
                            viewModel.saveCurrentNote { savedId ->
                                navController.navigate(Destination.detail(savedId)) {
                                    popUpTo(Destination.Home)
                                }
                            }
                        },
                    )
                }

                composable(
                    route = Destination.Detail,
                    arguments = listOf(
                        navArgument("noteId") { type = NavType.LongType },
                    ),
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                    val note by viewModel.noteDetails(noteId).collectAsStateWithLifecycle(initialValue = null)

                    NoteDetailScreen(
                        note = note,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Destination.editor(noteId)) },
                        onDelete = {
                            viewModel.deleteNote(noteId) {
                                navController.navigate(Destination.Home) {
                                    popUpTo(Destination.Home) { inclusive = true }
                                }
                            }
                        },
                        onTogglePinned = { viewModel.togglePinned(noteId) },
                        onShare = { currentNote ->
                            shareNote(context, currentNote)
                        },
                        onOpenAttachment = { attachment ->
                            when (attachment.type) {
                                AttachmentType.PDF -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.pdf(attachment.title, uri))
                                    }
                                }

                                AttachmentType.VIDEO -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.video(attachment.title, uri))
                                    }
                                }

                                AttachmentType.WEB_LINK,
                                AttachmentType.YOUTUBE -> {
                                    attachment.linkUrl?.let { url ->
                                        navController.navigate(Destination.web(attachment.title, url))
                                    }
                                }

                                AttachmentType.AUDIO -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.audio(attachment.title, uri))
                                    }
                                }

                                AttachmentType.DOCUMENT -> {
                                    attachment.uri?.let { openExternally(context, it) }
                                }

                                else -> Unit
                            }
                        },
                        getToc = viewModel::getToc
                    )
                }

                composable(
                    route = Destination.Pdf,
                    arguments = listOf(
                        navArgument("title") {
                            type = NavType.StringType
                            defaultValue = "PDF"
                        },
                        navArgument("uri") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    PdfViewerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUri = backStackEntry.arguments?.getString("uri").orEmpty(),
                        onOpenExternal = { rawUri -> openExternally(context, rawUri) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Destination.SubjectDetail,
                    arguments = listOf(
                        navArgument("subjectId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
                    val subject by viewModel.observeSubject(subjectId).collectAsStateWithLifecycle(initialValue = null)
                    val notes by viewModel.observeNotesBySubject(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())

                    SubjectDetailScreen(
                        subject = subject,
                        notes = notes,
                        onBack = { navController.popBackStack() },
                        onOpenNote = { noteId -> navController.navigate(Destination.detail(noteId)) },
                        onEditNote = { noteId -> navController.navigate(Destination.editor(noteId)) },
                        onTogglePinned = { noteId -> viewModel.togglePinned(noteId) },
                        onOpenAttachment = { attachment ->
                            when (attachment.type) {
                                AttachmentType.PDF -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.pdf(attachment.title, uri))
                                    }
                                }
                                AttachmentType.VIDEO -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.video(attachment.title, uri))
                                    }
                                }
                                AttachmentType.YOUTUBE -> {
                                    attachment.linkUrl?.let { url ->
                                        navController.navigate(Destination.web(attachment.title, url))
                                    }
                                }
                                AttachmentType.AUDIO -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.audio(attachment.title, uri))
                                    }
                                }
                                else -> {}
                            }
                        },
                        onCreateNote = { subjectId ->
                            viewModel.startNewNote()
                            viewModel.selectSubject(subjectId)
                            navController.navigate(Destination.editor())
                        }
                    )
                }

                composable(
                    route = Destination.Web,
                    arguments = listOf(
                        navArgument("title") {
                            type = NavType.StringType
                            defaultValue = "Link"
                        },
                        navArgument("url") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    WebMediaScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUrl = backStackEntry.arguments?.getString("url").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Destination.Video,
                    arguments = listOf(
                        navArgument("title") {
                            type = NavType.StringType
                            defaultValue = "Video"
                        },
                        navArgument("uri") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    VideoViewerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUri = backStackEntry.arguments?.getString("uri").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Destination.Audio,
                    arguments = listOf(
                        navArgument("title") {
                            type = NavType.StringType
                            defaultValue = "Audio"
                        },
                        navArgument("uri") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    @OptIn(ExperimentalMaterial3Api::class)
                    AudioPlayerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUri = backStackEntry.arguments?.getString("uri").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

fun getGreeting(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val prefix = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    return "$prefix, ${name.ifBlank { "Smart NoteMaster" }}"
}

private fun shareNote(context: Context, note: NoteDetails) {
    val text = buildString {
        appendLine(note.title)
        note.subject?.let { appendLine("Subject: ${it.name}") }
        if (note.tagNames.isNotEmpty()) appendLine("Tags: ${note.tagNames.joinToString(" ") { tag -> "#$tag" }}")
        if (note.summary.isNotBlank()) {
            appendLine()
            appendLine("Summary")
            appendLine(note.summary)
        }
        if (note.body.isNotBlank()) {
            appendLine()
            appendLine("Content")
            appendLine(note.body)
        }
        if (note.attachments.isNotEmpty()) {
            appendLine()
            appendLine("Attachments")
            note.attachments.forEach { attachment ->
                appendLine("- ${attachment.title}${attachment.linkUrl?.let { " ($it)" } ?: ""}")
            }
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share note"))
}

private fun openExternally(context: Context, rawUri: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(rawUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
