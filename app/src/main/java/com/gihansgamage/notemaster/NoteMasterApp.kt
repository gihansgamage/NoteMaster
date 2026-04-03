package com.gihansgamage.notemaster

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gihansgamage.notemaster.data.local.entity.AttachmentType
import com.gihansgamage.notemaster.feature.detail.NoteDetailScreen
import com.gihansgamage.notemaster.feature.editor.EditorScreen
import com.gihansgamage.notemaster.feature.home.HomeScreen
import com.gihansgamage.notemaster.feature.subject.SubjectDetailScreen
import com.gihansgamage.notemaster.feature.viewer.AudioPlayerScreen
import com.gihansgamage.notemaster.feature.viewer.BottomAudioBar
import com.gihansgamage.notemaster.feature.viewer.ImageViewerScreen
import com.gihansgamage.notemaster.feature.viewer.PdfViewerScreen
import com.gihansgamage.notemaster.feature.viewer.TextViewerScreen
import com.gihansgamage.notemaster.feature.viewer.VideoViewerScreen
import com.gihansgamage.notemaster.feature.viewer.WebMediaScreen
import com.gihansgamage.notemaster.feature.viewer.YouTubeViewerScreen
import com.gihansgamage.notemaster.ui.AppViewModelProvider
import com.gihansgamage.notemaster.ui.NoteMasterViewModel
import com.gihansgamage.notemaster.ui.navigation.Destination
import com.gihansgamage.notemaster.ui.theme.NoteMasterTheme
import com.gihansgamage.notemaster.util.openExternally
import com.gihansgamage.notemaster.util.shareNote
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NoteMasterApp(
    viewModel: NoteMasterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
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

    if (userPreferences.isLoaded) {
        val isDark = userPreferences.isDarkMode ?: isSystemInDarkTheme()
        NoteMasterTheme(darkTheme = isDark) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    isDark = isDark
                )

                val isAudioScreen = currentRoute?.startsWith("audio") == true
                BottomAudioBar(
                    state = playbackState.copy(isActive = playbackState.isActive && !isAudioScreen),
                    onToggle = viewModel::toggleAudio,
                    onClose = viewModel::stopAudio,
                    onSetSpeed = viewModel::setAudioSpeed,
                    isDark = isDark,
                    onClick = {
                        if (playbackState.isActive) {
                            navController.navigate(Destination.audio(playbackState.currentTitle, playbackState.currentUri))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: NoteMasterViewModel,
    snackbarHostState: SnackbarHostState,
    isDark: Boolean
) {
    val context = LocalContext.current
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val editorUiState by viewModel.editorUiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

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
                onOpenSettings = { navController.navigate(Destination.Settings) },
                onCreateSubject = viewModel::createSubject,
                onDeleteSubject = viewModel::deleteSubject,
                onRenameSubject = viewModel::updateSubject,
            )
        }

        composable(Destination.Settings) {
            com.gihansgamage.notemaster.feature.settings.SettingsScreen(
                userName = userPreferences.userName,
                isDarkMode = isDark,
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
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
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
                onShare = { currentNote -> shareNote(context, currentNote) },
                onOpenAttachment = { attachment ->
                    when (attachment.type) {
                        AttachmentType.PDF -> attachment.uri?.let { navController.navigate(Destination.pdf(attachment.title, it)) }
                        AttachmentType.VIDEO -> attachment.uri?.let { navController.navigate(Destination.video(attachment.title, it)) }
                        AttachmentType.WEB_LINK -> attachment.linkUrl?.let { navController.navigate(Destination.web(attachment.title, it)) }
                        AttachmentType.YOUTUBE -> attachment.linkUrl?.let { navController.navigate(Destination.web(attachment.title, it)) }
                        AttachmentType.AUDIO -> attachment.uri?.let { navController.navigate(Destination.audio(attachment.title, it)) }
                        AttachmentType.IMAGE -> attachment.uri?.let { navController.navigate(Destination.image(attachment.title, it)) }
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
                navArgument("title") { type = NavType.StringType; defaultValue = "PDF" },
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
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
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
                        AttachmentType.PDF -> attachment.uri?.let { navController.navigate(Destination.pdf(attachment.title, it)) }
                        AttachmentType.VIDEO -> attachment.uri?.let { navController.navigate(Destination.video(attachment.title, it)) }
                        AttachmentType.WEB_LINK -> attachment.linkUrl?.let { navController.navigate(Destination.web(attachment.title, it)) }
                        AttachmentType.YOUTUBE -> attachment.linkUrl?.let { navController.navigate(Destination.youtube(attachment.title, it)) }
                        AttachmentType.AUDIO -> attachment.uri?.let { navController.navigate(Destination.audio(attachment.title, it)) }
                        AttachmentType.IMAGE -> attachment.uri?.let { navController.navigate(Destination.image(attachment.title, it)) }
                        else -> {}
                    }
                },
                onCreateNote = { sId ->
                    viewModel.startNewNote()
                    viewModel.selectSubject(sId)
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
                navArgument("title") { type = NavType.StringType; defaultValue = "Link" },
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
                navArgument("title") { type = NavType.StringType; defaultValue = "Video" },
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
                navArgument("title") { type = NavType.StringType; defaultValue = "Audio" },
                navArgument("uri") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
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
            ImageViewerScreen(
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
                navArgument("attachmentId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            TextViewerScreen(
                title = backStackEntry.arguments?.getString("title").orEmpty(),
                content = backStackEntry.arguments?.getString("content").orEmpty(),
                onBack = { navController.popBackStack() },
                onSave = { newContent ->
                    val attachmentId = backStackEntry.arguments?.getLong("attachmentId") ?: -1L
                    if (attachmentId != -1L) {
                        viewModel.updateTextMaterial(attachmentId, newContent)
                    }
                }
            )
        }

        composable(
            route = Destination.YouTube,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            YouTubeViewerScreen(
                title = backStackEntry.arguments?.getString("title").orEmpty(),
                encodedUrl = backStackEntry.arguments?.getString("url").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenExternal = { url -> openExternally(context, url) }
            )
        }
    }
}
