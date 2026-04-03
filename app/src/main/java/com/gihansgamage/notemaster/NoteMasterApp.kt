package com.gihansgamage.notemaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.data.model.NoteDetails
import com.gihansgamage.notemaster.feature.detail.NoteDetailScreen
import com.gihansgamage.notemaster.feature.editor.EditorScreen
import com.gihansgamage.notemaster.feature.home.HomeScreen
import com.gihansgamage.notemaster.feature.viewer.PdfViewerScreen
import com.gihansgamage.notemaster.feature.viewer.VideoViewerScreen
import com.gihansgamage.notemaster.feature.viewer.WebMediaScreen
import com.gihansgamage.notemaster.feature.viewer.TextViewerScreen
import com.gihansgamage.notemaster.feature.viewer.ImageViewerScreen
import com.gihansgamage.notemaster.feature.subject.SubjectDetailScreen
import com.gihansgamage.notemaster.feature.viewer.AudioPlayerScreen
import com.gihansgamage.notemaster.feature.viewer.BottomAudioBar
import com.gihansgamage.notemaster.ui.AppViewModelProvider
import com.gihansgamage.notemaster.ui.NoteMasterViewModel
import com.gihansgamage.notemaster.ui.theme.NoteMasterTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import kotlin.OptIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private object Destination {
    const val Home = "home"
    const val Editor = "editor?noteId={noteId}"
    const val Detail = "detail/{noteId}"
    const val Pdf = "pdf?title={title}&uri={uri}"
    const val Video = "video?title={title}&uri={uri}"
    const val Web = "web?title={title}&url={url}"
    const val Audio = "audio?title={title}&uri={uri}"
    const val Image = "image?title={title}&uri={uri}"
    const val Text = "text?title={title}&content={content}&attachmentId={attachmentId}"
    const val YouTube = "viewer/youtube?title={title}&url={url}"
    const val SubjectDetail = "subject_detail/{subjectId}"
    const val Welcome = "welcome"
    const val Settings = "settings"
    fun editor(noteId: Long? = null): String = "editor?noteId=${noteId ?: -1L}"
    fun detail(noteId: Long): String = "detail/$noteId"
    fun pdf(title: String, uri: String): String = "pdf?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun video(title: String, uri: String): String = "video?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun web(title: String, url: String): String = "web?title=${Uri.encode(title)}&url=${Uri.encode(url)}"
    fun audio(title: String, uri: String): String = "audio?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun image(title: String, uri: String): String = "image?title=${Uri.encode(title)}&uri=${Uri.encode(uri)}"
    fun text(title: String, content: String, attachmentId: Long? = null): String = 
        "text?title=${Uri.encode(title)}&content=${Uri.encode(content)}&attachmentId=${attachmentId ?: -1L}"
    fun youtube(title: String, url: String): String = "viewer/youtube?title=${Uri.encode(title)}&url=${Uri.encode(url)}"
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
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                viewModel.userMessages
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    if (userPreferences.isLoaded) {
        NoteMasterTheme(
            darkTheme = userPreferences.isDarkMode ?: isSystemInDarkTheme(),
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Destination.Home,
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
                        onToggleSubjectPinned = viewModel::toggleSubjectPinned,
                        onOpenSettings = {
                            navController.navigate(Destination.Settings)
                        },
                        onCreateSubject = viewModel::createSubject,
                        onDeleteSubject = viewModel::deleteSubject,
                        onRenameSubject = viewModel::updateSubject,
                    )
                }

                composable(Destination.Settings) {
                    com.gihansgamage.notemaster.feature.settings.SettingsScreen(
                        userName = userPreferences.userName,
                        isDarkMode = userPreferences.isDarkMode ?: isSystemInDarkTheme(),
                        onNameChange = viewModel::updateUserName,
                        onThemeToggle = viewModel::updateDarkMode,
                        onDeleteAllData = viewModel::deleteAllData,
                        onVisitWebsite = { openExternally(context, "https://gihansgamage.github.io/NoteMaster-web/") },
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
                        snackbarHostState = snackbarHostState,
                        onBack = { navController.popBackStack() },
                        onTitleChange = viewModel::updateTitle,
                        onBodyChange = viewModel::updateBody,
                        onTagsChange = viewModel::updateTags,
                        onSelectSubject = viewModel::selectSubject,
                        onCreateSubject = viewModel::createSubject,
                        onTogglePinned = viewModel::togglePinnedInEditor,
                        onAddAttachment = viewModel::addAttachment,
                        onAddTextMaterial = viewModel::addTextMaterial,
                        onAddLink = viewModel::addLink,
                        onRemoveAttachment = viewModel::removeAttachment,
                        onSave = {
                            viewModel.saveCurrentNote { savedId ->
                                navController.navigate(Destination.detail(savedId)) {
                                    popUpTo(Destination.Home)
                                }
                            }
                        },
                        onDelete = {
                            if (noteIdArg > 0) {
                                viewModel.deleteNote(noteIdArg) {
                                    navController.navigate(Destination.Home) {
                                        popUpTo(Destination.Home) { inclusive = true }
                                    }
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

                                AttachmentType.WEB_LINK -> {
                                    attachment.linkUrl?.let { url ->
                                        navController.navigate(Destination.web(attachment.title, url))
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

                                AttachmentType.IMAGE -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.image(attachment.title, uri))
                                    }
                                }

                                AttachmentType.TEXT -> {
                                    if (attachment.content != null) {
                                        navController.navigate(Destination.text(attachment.title, attachment.content, attachment.localId.toLongOrNull()))
                                    } else {
                                        attachment.uri?.let { openExternally(context, it) }
                                    }
                                }

                                else -> Unit
                            }
                        },
                        getToc = viewModel::getToc,
                        playbackState = playbackState,
                        onPlayAudio = viewModel::playAudio,
                        onToggleAudio = viewModel::toggleAudio
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
                                AttachmentType.WEB_LINK -> {
                                    attachment.linkUrl?.let { url ->
                                        navController.navigate(Destination.web(attachment.title, url))
                                    }
                                }
                                AttachmentType.YOUTUBE -> {
                                    attachment.linkUrl?.let { url ->
                                        navController.navigate(Destination.youtube(attachment.title, url))
                                    }
                                }
                                AttachmentType.AUDIO -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.audio(attachment.title, uri))
                                    }
                                }
                                AttachmentType.IMAGE -> {
                                    attachment.uri?.let { uri ->
                                        navController.navigate(Destination.image(attachment.title, uri))
                                    }
                                }
                                else -> {}
                            }
                        },
                        onCreateNote = { subjectId ->
                            viewModel.startNewNote()
                            viewModel.selectSubject(subjectId)
                            navController.navigate(Destination.editor())
                        },
                        playbackState = playbackState,
                        onPlayAudio = viewModel::playAudio,
                        onToggleAudio = viewModel::toggleAudio
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
                        onOpenExternal = { url -> openExternally(context, url) }
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
                        playbackState = playbackState,
                        onPlay = viewModel::playAudio,
                        onToggle = viewModel::toggleAudio,
                        onSeek = viewModel::seekAudio,
                        onSetSpeed = viewModel::setAudioSpeed,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Destination.Image,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("uri") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    com.gihansgamage.notemaster.feature.viewer.ImageViewerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUri = backStackEntry.arguments?.getString("uri").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Destination.Text,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("content") { type = NavType.StringType },
                        navArgument("attachmentId") { 
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { backStackEntry ->
                    val attachmentId = backStackEntry.arguments?.getLong("attachmentId") ?: -1L
                    TextViewerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        content = backStackEntry.arguments?.getString("content").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onSave = { newContent ->
                            if (attachmentId != -1L) {
                                viewModel.updateTextMaterial(attachmentId, newContent)
                            }
                        }
                    )
                }

                composable(
                    route = Destination.Web,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("url") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    WebMediaScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUrl = backStackEntry.arguments?.getString("url").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenExternal = { url -> openExternally(context, url) }
                    )
                }

                composable(
                    route = Destination.YouTube,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                        navArgument("url") { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    com.gihansgamage.notemaster.feature.viewer.YouTubeViewerScreen(
                        title = backStackEntry.arguments?.getString("title").orEmpty(),
                        encodedUrl = backStackEntry.arguments?.getString("url").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onOpenExternal = { url -> openExternally(context, url) }
                    )
                }
            }

            val isAudioScreen = currentRoute?.startsWith("audio") == true
            
            BottomAudioBar(
                state = playbackState.copy(isActive = playbackState.isActive && !isAudioScreen),
                onToggle = viewModel::toggleAudio,
                onClose = viewModel::stopAudio,
                onSetSpeed = viewModel::setAudioSpeed,
                isDark = userPreferences.isDarkMode ?: isSystemInDarkTheme(),
                onClick = {
                    if (playbackState.isActive) {
                        navController.navigate(Destination.audio(playbackState.currentTitle, playbackState.currentUri))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Above snackbar
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}
}

fun getGreeting(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val prefix = when (hour) {
        in 0..4 -> "It’s late night, ${name.ifBlank { "Smart NoteMaster" }}. Time for deep and creative thinking."
        in 5..11 -> "Good morning, ${name.ifBlank { "Smart NoteMaster" }}. Start your day with a clear mind."
        in 12..16 -> "Good afternoon, ${name.ifBlank { "Smart NoteMaster" }}. Keep your ideas moving forward."
        in 17..20 -> "Good evening, ${name.ifBlank { "Smart NoteMaster" }}. Time to reflect and organize your thoughts."
        else -> "Good night, ${name.ifBlank { "Smart NoteMaster" }}. Capture today’s final thoughts before you rest."
    }
    return prefix
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
