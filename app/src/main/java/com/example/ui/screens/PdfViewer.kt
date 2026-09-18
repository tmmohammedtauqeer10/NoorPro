package com.example.ui.screens

import com.example.utils.CrashReporter

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import com.example.ui.theme.MatteGold

@Composable
fun PdfViewer(
    file: File,
    initialPage: Int,
    onPageChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val density = LocalDensity.current.density
    var pdfError by remember { mutableStateOf<String?>(null) }

    // Mutex to ensure thread-safe page rendering
    val renderMutex = remember { Mutex() }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor = descriptor
                pdfRenderer = PdfRenderer(descriptor)
                pageCount = pdfRenderer?.pageCount ?: 0
            } catch (e: Throwable) {
                CrashReporter.report(e)
                pdfError = "Corrupt or invalid PDF file"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            GlobalScope.launch(Dispatchers.IO) {
                renderMutex.withLock {
                    try {
                        pdfRenderer?.close()
                        fileDescriptor?.close()
                    } catch (e: Throwable) {
                        CrashReporter.report(e)
                    }
                }
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, pageCount) {
        if (pageCount > 0) {
            onPageChanged(listState.firstVisibleItemIndex, pageCount)
        }
    }

    if (pdfError != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = pdfError!!, color = Color.Red)
        }
    } else if (pageCount == 0 || pdfRenderer == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Gray)
        }
    } else {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = max(1f, scale * zoom)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(pageCount) { index ->
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                var renderError by remember { mutableStateOf(false) }

                LaunchedEffect(index) {
                    var newBmp: Bitmap? = null
                    renderError = false
                    try {
                        withContext(Dispatchers.IO) {
                            try {
                                renderMutex.withLock {
                                    val renderer = pdfRenderer ?: return@withLock
                                    var page: PdfRenderer.Page? = null
                                    try {
                                        page = renderer.openPage(index)
                                        val width = max(1, (page.width * density).toInt().coerceAtMost(800))
                                        val height = max(1, (page.height * density).toInt().coerceAtMost(1200))
                                        
                                        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        b.eraseColor(android.graphics.Color.WHITE)
                                        page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        newBmp = b
                                    } catch (e: Throwable) {
                                        CrashReporter.report(e)
                                        renderError = true
                                    } finally {
                                        try {
                                            page?.close()
                                        } catch (e: Throwable) {
                                            CrashReporter.report(e)
                                        }
                                    }
                                }
                            } catch (e: Throwable) {
                                CrashReporter.report(e)
                            }
                        }
                        bitmap = newBmp
                    } finally {
                        if (newBmp != null && bitmap != newBmp && !newBmp!!.isRecycled) {
                            newBmp!!.recycle()
                        }
                    }
                }

                DisposableEffect(index) {
                    onDispose {
                        bitmap = null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 1f / 1.4f)
                        .padding(bottom = 8.dp)
                        .background(Color.White)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Page $index",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (renderError) {
                        Text(
                            text = "Unable to render page ${index + 1}",
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // Placeholder while loading
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MatteGold
                        )
                    }
                }
            }
        }
    }
}
