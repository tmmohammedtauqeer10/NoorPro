package com.example.audio.models

/**
 * Structured license/attribution block shown on Now Playing and track detail.
 * Every Al Noor Audio track must carry a clear free/public-domain license ΓÇö
 * see docs/AL_NOOR_AUDIO.md. Commercial / unknown provenance is rejected.
 */
data class LicenseAttribution(
    val license: LicenseType,
    val attributionText: String,
    val sourceUrl: String,
    val licenseUrl: String? = null,
) {
    fun displayLine(): String = buildString {
        append(attributionText)
        append(" ┬╖ ")
        append(license.name.replace('_', ' '))
        if (!licenseUrl.isNullOrBlank()) {
            append(" ┬╖ ")
            append(licenseUrl)
        }
    }

    companion object {
        fun from(track: Track) = LicenseAttribution(
            license = track.license,
            attributionText = track.attributionText,
            sourceUrl = track.sourceUrl,
            licenseUrl = track.licenseUrl,
        )
    }
}
