package com.noorpro.app

object BuildConfig {
    const val DEBUG: Boolean = false
    const val APPLICATION_ID: String = "com.noorpro.app"
    const val BUILD_TYPE: String = "release"
    const val VERSION_CODE: Int = 3
    const val VERSION_NAME: String = "1.0.2"
    // NEVER use Google sample units (ca-app-pub-394025609…) in release.
    // Real NoorPro banner units (app id ca-app-pub-9239932537224257~4622879088 is in the manifest).
    const val ADMOB_BOTTOM_BANNER: String = "ca-app-pub-9239932537224257/6312829655"
    const val ADMOB_FEED_BANNER: String = "ca-app-pub-9239932537224257/1833519437"
    const val GOOGLE_WEB_CLIENT_ID: String = "684547719535-njganphl6n5jg7r4ebo88v3472hiqfl9.apps.googleusercontent.com"
    const val YOUTUBE_API_KEY: String = "AIzaSyAQy7RrAmrneAVYwcc7ATVJoRXEqpMzn90"
}
