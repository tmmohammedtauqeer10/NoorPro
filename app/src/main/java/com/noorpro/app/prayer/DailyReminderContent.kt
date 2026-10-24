package com.noorpro.app.prayer

/** Short, well-known ayat and duas for the daily reminder (translation only; reference included). */
object DailyReminderContent {
    data class Item(val text: String, val reference: String)

    val items = listOf(
        Item("Indeed, with hardship comes ease.", "Quran 94:6"),
        Item("So remember Me; I will remember you.", "Quran 2:152"),
        Item("And whoever relies upon Allah - He is sufficient for him.", "Quran 65:3"),
        Item("Indeed, Allah is with the patient.", "Quran 2:153"),
        Item("Verily, in the remembrance of Allah do hearts find rest.", "Quran 13:28"),
        Item("Allah does not burden a soul beyond what it can bear.", "Quran 2:286"),
        Item("My Lord, increase me in knowledge.", "Quran 20:114"),
        Item("Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.", "Quran 2:201"),
        Item("Call upon Me; I will respond to you.", "Quran 40:60"),
        Item("Do not despair of the mercy of Allah.", "Quran 39:53"),
        Item("Allah is sufficient for us, and He is the best disposer of affairs. (Hasbunallahu wa ni'mal wakeel)", "Quran 3:173"),
        Item("And He is with you wherever you are.", "Quran 57:4"),
        Item("Indeed, Allah loves those who rely upon Him.", "Quran 3:159"),
        Item("Our Lord, do not let our hearts deviate after You have guided us, and grant us mercy from Yourself.", "Quran 3:8"),
    )

    fun forDay(dayOfYear: Int): Item = items[Math.floorMod(dayOfYear, items.size)]
}
