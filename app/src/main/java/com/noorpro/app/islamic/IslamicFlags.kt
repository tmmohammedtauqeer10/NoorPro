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

    fun isOn(key: String): Boolean = Reels2Flags.isOn(key)
}
