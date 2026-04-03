package com.gihansgamage.notemaster.feature.viewer

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.gihansgamage.notemaster.util.LinkClassifier

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebMediaScreen(
    title: String,
    encodedUrl: String,
    onBack: () -> Unit,
) {
    val rawUrl = LinkClassifier.normalizeUrl(Uri.decode(encodedUrl))
    val displayTitle = Uri.decode(title).ifBlank { "Web viewer" }
    val embedUrl = LinkClassifier.toYouTubeEmbedUrl(rawUrl)
    val isYouTubeVideo = embedUrl != null
    val targetUrl = embedUrl ?: rawUrl

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(displayTitle) },
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
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                if (isYouTubeVideo) {
                    val html = """
                        <!doctype html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
                            <style>
                                html, body { margin: 0; padding: 0; background: black; height: 100%; }
                                iframe { border: 0; width: 100%; height: 100%; }
                            </style>
                        </head>
                        <body>
                            <iframe
                                src="$targetUrl"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()

                    val htmlHash = html.hashCode().toString()
                    if (webView.tag != htmlHash) {
                        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                        webView.tag = htmlHash
                    }
                } else if (webView.url != targetUrl) {
                    webView.loadUrl(targetUrl)
                }
            },
        )
    }
}
