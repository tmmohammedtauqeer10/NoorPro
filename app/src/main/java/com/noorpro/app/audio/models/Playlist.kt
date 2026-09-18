package com.noorpro.app.audio.models

data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val trackIds: List<String> = emptyList(),
    val isSystem: Boolean = false,
)
