package com.example.audio.models

/**
 * Catalog track for Al Noor Audio (nasheed / naat only).
 * Distinct from Quran [com.noorpro.app.ui.viewmodel.AudioQueueItem].
 */
data class Track(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val audioUrl: String,
    val durationMs: Long,
    val coverUrl: String? = null,
    val license: LicenseType,
    val attributionText: String,
    val sourceUrl: String,
    val licenseUrl: String? = null,
    val tags: List<String> = emptyList(),
    val language: String? = null,
)
