package com.noorpro.app.islamic

import com.noorpro.app.ui.screens.Reels2Flags

/**
 * 1.1.6 Islamic feature switches. Registered in the same Remote Config map as reels_v2_enabled
 * ([Reels2Flags], loaded once at startup) and all default OFF, so nothing changes for users until
 * a flag is turned on in the Firebase console.
 */
object IslamicFlags {
    const val CALENDAR = "islamic_calendar_enabled"
    const val GROUP_KHATM = "group_khatm_enabled"
    const val HIFZ_COACH = "hifz_coach_enabled"
    const val MASJID_PAGES = "masjid_pages_enabled"
    const val FAMILY_KIDS = "family_kids_enabled"
    const val DEEN_GOALS = "daily_deen_goals_enabled"

    val ALL = listOf(CALENDAR, GROUP_KHATM, HIFZ_COACH, MASJID_PAGES, FAMILY_KIDS, DEEN_GOALS)

    // 1.1.6 social & audio features (docs/SOCIAL_AUDIO_1.1.6.md), all default OFF.
    const val DUA_REQUESTS = "dua_requests_enabled"
    const val HALAQA_ROOMS = "halaqa_rooms_enabled"
    const val ASK_SCHOLAR = "ask_scholar_enabled"
    const val CHALLENGES = "challenges_enabled"
    const val SHARE_CARDS = "share_cards_enabled"
    const val QURAN_OFFLINE = "quran_offline_enabled"
    const val AUDIO_ROUTINES = "audio_routines_enabled"
    const val ADHAN_VOICES = "adhan_voices_enabled"
    const val CAR_MODE = "car_mode_enabled"
    const val BAYAN_PODCASTS = "bayan_podcasts_enabled"

    val SOCIAL_AUDIO = listOf(DUA_REQUESTS, HALAQA_ROOMS, ASK_SCHOLAR, CHALLENGES, SHARE_CARDS, QURAN_OFFLINE, AUDIO_ROUTINES, ADHAN_VOICES, CAR_MODE, BAYAN_PODCASTS)

    fun isOn(key: String): Boolean = Reels2Flags.isOn(key)
}
