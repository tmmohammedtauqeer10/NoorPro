package com.noorpro.app.audio.models

/**
 * Allowed licenses for Al Noor Audio catalog tracks.
 * See docs/AL_NOOR_AUDIO.md — commercial / unknown provenance is rejected.
 */
enum class LicenseType {
    PUBLIC_DOMAIN,
    CC0,
    CC_BY,
    CC_BY_SA,
    PERMISSION_LETTER,
    /** Source's published terms explicitly allow free streaming in apps (e.g. Islamic Network CDN). */
    SOURCE_TERMS,
}
