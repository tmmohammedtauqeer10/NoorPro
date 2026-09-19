package com.example.audio.models

data class Artist(
    val id: String,
    val name: String,
    val bio: String? = null,
    val imageUrl: String? = null,
)
