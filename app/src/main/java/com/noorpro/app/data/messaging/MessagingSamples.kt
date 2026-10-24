package com.noorpro.app.data.messaging

/** 1.1.3 sample data for the messaging design review (renders + a debug preview route). */
object MessagingSamples {
    const val ME = "me"
    private const val H = 3_600_000L
    val now: Long get() = System.currentTimeMillis()

    val people = listOf(
        MsgPerson("u1", "Amina Yusuf", "amina.reflects", "img://p1", following = true, followsMe = true, sharedGroups = 2, interests = listOf("Tajweed")),
        MsgPerson("u2", "Bilal Hassan", "bilal.h", "", following = true, followsMe = true, sharedGroups = 1),
        MsgPerson("u3", "Sumayyah Khan", "sumayyah", "img://p2", following = false, followsMe = true, sharedGroups = 1),
        MsgPerson("u4", "Yusuf Ali", "yusuf.ali", "", following = true, followsMe = false),
        MsgPerson("u5", "Khadija Rahman", "khadija.r", "", sharedGroups = 3, interests = listOf("Seerah")),
        MsgPerson("u6", "Amina Siddiqui", "amina.s", "", interests = listOf("Arabic")),
        MsgPerson("u7", "Imran Qureshi", "imran.q", "", followsMe = true),
        MsgPerson("u8", "Hafsa Noor", "hafsa.noor", "img://p3", sharedGroups = 2, interests = listOf("Hifz")),
        MsgPerson("u9", "Amina", "amina", "", interests = listOf("Quran")),
    )

    fun chats(now: Long = this.now) = listOf(
        MsgChat("c1", "u1", "Amina Yusuf", "amina.reflects", "img://p1", "Assalamu alaikum! Are you joining the halaqah tonight?", false, 0, now - 4 * 60_000, unread = 2, online = true, pinned = true),
        MsgChat("c2", "u2", "Bilal Hassan", "bilal.h", "", "Voice message", true, 3, now - 38 * 60_000, online = true, pinned = true, voice = true),
        MsgChat("c3", "u3", "Sumayyah Khan", "sumayyah", "img://p2", "JazakAllahu khayran for the notes 🌿", false, 0, now - 2 * H, typing = true),
        MsgChat("c4", "u4", "Yusuf Ali", "yusuf.ali", "", "See you at Jumu'ah in sha Allah", true, 2, now - 5 * H),
        MsgChat("c5", "u7", "Imran Qureshi", "imran.q", "", "Sent the Ramadan planner PDF", false, 0, now - 26 * H, unread = 1),
        MsgChat("c6", "u8", "Hafsa Noor", "hafsa.noor", "img://p3", "Ameen, may Allah make it easy", true, 3, now - 50 * H),
    )

    fun groups(now: Long = this.now) = listOf(
        NoorGroup("g1", "Fajr Quran Circle", "Juz 30 with tajweed after Fajr, every day. Gentle, beginner friendly.", "img://mosque", "quran",
            listOf("tajweed", "juz30", "fajr"), GroupVisibility.PUBLIC_REQUEST, ME, listOf("u1"), listOf(ME, "u1", "u2", "u3", "u5", "u8"), 128,
            "Recording of today's session is up", "Amina", now - 9 * 60_000, unread = 5, pinnedNote = "Halaqah tonight after Isha · Surah Al-Mulk", onlineCount = 6),
        NoorGroup("g2", "Masjid An-Noor Community", "Announcements, iftar rota and volunteering for our local masjid.", "img://rehal", "masjid",
            listOf("hyderabad", "iftar"), GroupVisibility.PUBLIC_OPEN, "u2", emptyList(), listOf(ME, "u2", "u4"), 842,
            "Iftar volunteers needed this Friday", "Bilal", now - 3 * H, unread = 0, muted = true),
        NoorGroup("g3", "Sisters' Seerah Study", "Weekly reading of Ar-Raheeq Al-Makhtum.", "", "sisters",
            listOf("seerah"), GroupVisibility.PRIVATE, "u5", listOf("u3"), listOf(ME, "u3", "u5"), 36,
            "Next chapter: The Hijrah", "Khadija", now - 20 * H, unread = 1),
    )

    fun discover() = groups() + listOf(
        NoorGroup("d6", "Hifz Buddies", "Pair up with a revision partner and memorise Juz Amma together.", "img://rehal", "quran",
            listOf("hifz", "juzamma"), GroupVisibility.PUBLIC_OPEN, "x6", emptyList(), emptyList(), 4_480),
        NoorGroup("d7", "Tajweed Corrections", "Send a 30-second recitation, get gentle feedback from qualified teachers.", "", "quran",
            listOf("tajweed", "recitation"), GroupVisibility.PUBLIC_REQUEST, "x7", emptyList(), emptyList(), 1_760),
        NoorGroup("d1", "Hadith a Day", "One authentic hadith daily with short commentary from Riyad as-Salihin.", "img://rehal", "hadith",
            listOf("riyadussalihin", "daily"), GroupVisibility.PUBLIC_OPEN, "x1", emptyList(), emptyList(), 12_400),
        NoorGroup("d2", "Reverts Support Circle", "A warm, private-feeling space for new Muslims — ask anything.", "", "reverts",
            listOf("newmuslim", "salah"), GroupVisibility.PUBLIC_REQUEST, "x2", emptyList(), emptyList(), 2_310),
        NoorGroup("d3", "Ramadan 1448 Goals", "Track Quran, qiyam and sadaqah goals together.", "img://mosque", "ramadan",
            listOf("ramadan", "goals"), GroupVisibility.PUBLIC_OPEN, "x3", emptyList(), emptyList(), 5_870),
        NoorGroup("d4", "Hajj & Umrah 2027 Prep", "Checklists, duas and travel tips for pilgrims.", "", "hajj",
            listOf("umrah", "hajj2027"), GroupVisibility.PUBLIC_OPEN, "x4", emptyList(), emptyList(), 940),
        NoorGroup("d5", "Quran Arabic Basics", "Understand the words you recite — 10 minutes a day.", "", "arabic",
            listOf("arabic", "vocab"), GroupVisibility.PUBLIC_OPEN, "x5", emptyList(), emptyList(), 3_120),
    )

    val groupMembers = listOf(
        MsgMember(ME, "Tauqeer", "tauqeer", "", GroupRole.OWNER, true),
        MsgMember("u1", "Amina Yusuf", "amina.reflects", "img://p1", GroupRole.ADMIN, true),
        MsgMember("u2", "Bilal Hassan", "bilal.h", "", GroupRole.MEMBER, true),
        MsgMember("u3", "Sumayyah Khan", "sumayyah", "img://p2", GroupRole.MEMBER),
        MsgMember("u5", "Khadija Rahman", "khadija.r", "", GroupRole.MEMBER),
        MsgMember("u8", "Hafsa Noor", "hafsa.noor", "img://p3", GroupRole.MEMBER),
    )
}
