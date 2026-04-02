package com.gihansgamage.notemaster.feature.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RenderedPdfPage(
    val bitmap: Bitmap,
    val pageCount: Int,
)

private data class PdfViewerState(
    val page: RenderedPdfPage? = null,
    val hasError: Boolean = false,
)

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
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val viewerState by produceState(initialValue = PdfViewerState(), encodedUri, currentPage) {
        value = withContext(Dispatchers.IO) {
            val renderedPage = renderPdfPage(context, decodedUri, currentPage)
            PdfViewerState(
                page = renderedPage,
                hasError = renderedPage == null,
            )
        }
    }

    val pageCount = viewerState.page?.pageCount ?: 1
    val visiblePage = currentPage.coerceIn(0, pageCount - 1)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(Uri.decode(title).ifBlank { "PDF viewer" })
                        Text(
                            text = "Page ${visiblePage + 1} / $pageCount",
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                windowInsets = WindowInsets.statusBars,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val page = viewerState.page
            if (page != null) {
                Image(
                    bitmap = page.bitmap.asImageBitmap(),
                    contentDescription = "PDF page",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (viewerState.hasError) {
                Text(
                    text = "Couldn't open this PDF in the in-app viewer.",
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { onOpenExternal(decodedUri.toString()) }) {
                    Text("Open with another app")
                }
            } else {
                CircularProgressIndicator()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { currentPage = (visiblePage - 1).coerceAtLeast(0) },
                    enabled = visiblePage > 0 && page != null,
                ) {
                    Text("Previous page")
                }
                Button(
                    onClick = { currentPage = (visiblePage + 1).coerceAtMost(pageCount - 1) },
                    enabled = visiblePage < pageCount - 1 && page != null,
                ) {
                    Text("Next page")
                }
            }
        }
    }
}

private fun renderPdfPage(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
): RenderedPdfPage? {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                    val safePage = pageIndex.coerceIn(0, renderer.pageCount - 1)
                renderer.openPage(safePage).use { page ->
                    val width = (page.width * 1.5).toInt()
                    val height = (page.height * 1.5).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    RenderedPdfPage(bitmap = bitmap, pageCount = renderer.pageCount)
                }
            }
        }
    }.getOrNull()
}
