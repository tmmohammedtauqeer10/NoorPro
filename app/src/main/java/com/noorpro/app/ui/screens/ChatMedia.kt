package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noorpro.app.data.media.RemoteVideoFrame

/** Keeps the last known aspect ratio per url so bubbles don't jump while scrolling. */
internal object ChatMediaAspect {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Float>()
    fun get(url: String): Float? = cache[url]
    fun put(url: String, r: Float) { if (r.isFinite() && r > 0f) cache[url] = r }
    /** WhatsApp-like clamp: tall media up to 3:4.6, wide up to 2:1. */
    fun clamp(r: Float): Float = r.coerceIn(0.65f, 2f)
}

/**
 * 1.0.83: chat media thumbnail that shows the whole picture/first frame at its own aspect
 * ratio (no hard 232x150 crop) and decodes at bubble resolution x density for a sharp image.
 */
@Composable
internal fun ChatMediaThumb(
    url: String,
    isVideo: Boolean,
    contentDescription: String?,
    width: Dp = 250.dp,
    knownAspect: Float? = null,
    background: Color = Color(0xFF1B1F22),
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val ctx = LocalContext.current
    var aspect by remember(url) { mutableFloatStateOf(knownAspect ?: ChatMediaAspect.get(url) ?: if (isVideo) 0.75f else 1f) }
    val request = remember(url, isVideo) {
        ImageRequest.Builder(ctx)
            .data(if (isVideo) RemoteVideoFrame(url) else url)
            .size(1080)
            .crossfade(true)
            .build()
    }
    Box(
        modifier.width(width).aspectRatio(ChatMediaAspect.clamp(aspect)).clip(RoundedCornerShape(10.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            // Crop only trims the clamped extremes; normal photos/videos show in full.
            contentScale = ContentScale.Crop,
            onSuccess = { s ->
                val sz = s.painter.intrinsicSize
                if (sz.width > 0f && sz.height > 0f) {
                    val r = sz.width / sz.height
                    ChatMediaAspect.put(url, r); aspect = r
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        overlay()
    }
}

/** Pinch-to-zoom / pan / double-tap image for the full-screen chat viewer. */
@Composable
internal fun ZoomableChatImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var ox by remember(url) { mutableFloatStateOf(0f) }
    var oy by remember(url) { mutableFloatStateOf(0f) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        if (scale > 1f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f }
    }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(url) {
                detectTapGestures(onDoubleTap = { _: Offset ->
                    if (scale > 1f) { scale = 1f; ox = 0f; oy = 0f } else scale = 2.5f
                })
            }
            .transformable(state)
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = ox; translationY = oy },
    )
}
