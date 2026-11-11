package com.noorpro.app.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Streams the credited, licensed original only after an explicit tap. Never autoplays. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun LecturePlayer(media: LicensedLecture) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by rememberSaveable(media.url) { mutableStateOf(false) }
    var position by rememberSaveable(media.url) { mutableLongStateOf(0L) }
    var failure by remember(media.url) { mutableStateOf(false) }
    if (!started) {
        Button(onClick = { started = true }, modifier = Modifier.fillMaxWidth()) { Text("Play lecture in NoorPro") }
        Text("Internet required · 19 min · approximately 97 MB for the full original video", style = MaterialTheme.typography.bodySmall)
        return
    }
    val player = remember(media.url) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(media.url))
            seekTo(position)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { failure = true }
            })
            prepare()
        }
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { position = player.currentPosition; player.pause() }
        }
        lifecycle.addObserver(observer)
        onDispose { position = player.currentPosition; lifecycle.removeObserver(observer); player.release() }
    }
    AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } },
        update = { it.player = player }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
    if (failure) {
        Text("The lecture could not play. Check your connection or open the credited source below.")
        TextButton(onClick = { failure = false; player.prepare() }) { Text("Retry video") }
    }
}
