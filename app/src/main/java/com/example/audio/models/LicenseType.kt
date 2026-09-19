package com.example.audio.models

/**
 * Allowed licenses for Al Noor Audio catalog tracks.
 * See docs/AL_NOOR_AUDIO.md ΓÇö commercial / unknown provenance is rejected.
 */
enum class LicenseType {
    PUBLIC_DOMAIN,
    CC0,
    CC_BY,
    CC_BY_SA,
    PERMISSION_LETTER,
    /** Pixabay Content License (commercial app embedding OK). */
    PIXABAY,
}
