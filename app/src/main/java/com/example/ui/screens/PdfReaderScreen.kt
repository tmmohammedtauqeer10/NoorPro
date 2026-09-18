package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var barsVisible by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    
    var webViewUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.currentPdfUrl) {
        // Programmatically pre-create the WebView WebAssembly code cache directory
        // to silence the chromium filesystem warning log.
        try {
            val wasmDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmDir.exists()) {
                wasmDir.mkdirs()
            }
        } catch (e: Exception) {
            // Silence exceptions safely
        }

        val url = viewModel.currentPdfUrl
        if (url.contains("drive.google.com/uc?export=download&id=")) {
            val id = url.substringAfter("id=").substringBefore("&")
            webViewUrl = "https://drive.google.com/file/d/$id/preview"
        } else {
            webViewUrl = "https://docs.google.com/gview?embedded=true&url=${URLEncoder.encode(url, "UTF-8")}"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = { 
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it 
                                    webViewRef?.findAllAsync(it)
                                },
                                placeholder = { Text("Search book...", color = MaterialTheme.colorScheme.onSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(viewModel.currentPdfTitle, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1) 
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                                webViewRef?.clearMatches()
                            } else {
                                viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD) 
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = { webViewRef?.findNext(true) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { webViewRef?.findNext(false) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (webViewUrl.isNotEmpty()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false 
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }
                            }
                            webChromeClient = WebChromeClient()
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        if (webView.url != webViewUrl) {
                            isLoading = true
                            webView.loadUrl(webViewUrl)
                        }
                        webViewRef = webView
                    }
                )
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading book from cloud...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
