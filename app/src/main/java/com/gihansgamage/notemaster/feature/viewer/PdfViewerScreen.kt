package com.gihansgamage.notemaster.feature.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    title: String,
    encodedUri: String,
    onOpenExternal: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val decodedUri = Uri.parse(encodedUri)
    val displayTitle = title.ifBlank { "PDF viewer" }
    
    // Zoom/Pan State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    val currentPage by remember {
        derivedStateOf { 
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val firstMostlyVisible = visibleItems.firstOrNull { it.offset >= -(it.size / 2) }
            (firstMostlyVisible?.index ?: lazyListState.firstVisibleItemIndex) + 1
        }
    }

    val sharedPrefs = context.getSharedPreferences("pdf_bookmarks", android.content.Context.MODE_PRIVATE)
    var savedBookmark by remember { mutableIntStateOf(sharedPrefs.getInt(encodedUri, -1)) }

    // PDF Info State (Page Count)
    val pageCount by produceState(initialValue = 0) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(decodedUri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { it.pageCount }
                }
            }.getOrNull() ?: 0
        }
    }


    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(displayTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (savedBookmark != -1) {
                        IconButton(onClick = { 
                            coroutineScope.launch { lazyListState.scrollToItem(savedBookmark - 1) } 
                        }) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = "Go to bookmark")
                        }
                    }
                    IconButton(onClick = {
                        if (savedBookmark == currentPage) {
                            sharedPrefs.edit().remove(encodedUri).apply()
                            savedBookmark = -1
                        } else {
                            sharedPrefs.edit().putInt(encodedUri, currentPage).apply()
                            savedBookmark = currentPage
                        }
                    }) {
                        Icon(
                            if (savedBookmark == currentPage) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark this page"
                        )
                    }
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Jump to page")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .transformable(state = state)
        ) {
            if (pageCount > 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x.coerceIn(-500f * scale, 500f * scale),
                            translationY = offset.y.coerceIn(-1000f * scale, 1000f * scale)
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    state = lazyListState
                ) {
                    items(pageCount) { index ->
                        PdfPage(context, decodedUri, index)
                    }
                }

                // Page Indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (savedBookmark == currentPage) "⭐ $currentPage / $pageCount" else "$currentPage / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else if (pageCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Couldn't open this PDF.")
                    OutlinedButton(onClick = { onOpenExternal(decodedUri.toString()) }) {
                        Text("Open externally")
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column {
                    Text("Enter page number (1 - $pageCount)")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { jumpPageInput = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pageNum = jumpPageInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..pageCount) {
                            coroutineScope.launch {
                                lazyListState.scrollToItem(pageNum - 1)
                            }
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PdfPage(context: android.content.Context, uri: Uri, pageIndex: Int) {
    val pageState by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(pageIndex).use { page ->
                            val width = (page.width * 1.5).toInt()
                            val height = (page.height * 1.5).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }.getOrNull()
        }
    }

    if (pageState != null) {
        Image(
            bitmap = pageState!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

