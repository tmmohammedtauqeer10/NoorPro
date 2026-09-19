package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class Surah(
    val id: Int,
    val nameEnglish: String,
    val nameArabic: String,
    val englishTranslation: String,
    val versesCount: Int,
    val type: String,
    val verses: List<Pair<String, String>> // Pair of (Arabic text, English translation)
)

data class PrayerTime(
    val name: String,
    val time: String, // format "HH:mm"
    val color: Color,
    val isNotificationEnabled: Boolean = true
)

data class Reciter(
    val id: Int,
    val name: String,
    val description: String,
    val prefixUrl: String
)

data class HijriEvent(
    val name: String,
    val dateHijri: String,
    val daysRemaining: Int,
    val secondaryColor: Color
)

data class Azkar(
    val id: Int,
    val category: String, // "Morning" or "Evening"
    val title: String,
    val arabic: String,
    val translation: String,
    val reference: String,
    val targetCount: Int
)

object IslamicData {
    val reciters = listOf(
        Reciter(1, "Mishary Alafasy", "Crystal clear high notes & calm rhythm", "https://everyayah.com/data/Alafasy_128kbps/"),
        Reciter(2, "Abdul Rahman Al-Sudais", "Heartfelt & commanding Hijazi recitation", "https://everyayah.com/data/Abdurrahmaan_As-Sudais_192kbps/"),
        Reciter(3, "Maher Al-Muaiqly", "Deep, soulful and meditative", "https://everyayah.com/data/MaherAlMuaiqly128kbps/"),
        Reciter(4, "Saud Al-Shuraim", "Swift, rhythmic recitation", "https://everyayah.com/data/Saood_ash-Shuraym_128kbps/"),
        Reciter(5, "Abdul Basit (Murattal)", "Classic, powerful and resonant tone", "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/"),
        Reciter(6, "Abdul Basit (Mujawwad)", "Slow, melodic and expansive style", "https://everyayah.com/data/Abdul_Basit_Mujawwad_128kbps/"),
        Reciter(7, "Mahmoud Khaleel Al-Husary", "Precise Tajweed and rhythmic pacing", "https://everyayah.com/data/Husary_128kbps/"),
        Reciter(8, "Muhammad Al-Minshawy", "Beautiful, emotional and crying tone", "https://everyayah.com/data/Minshawy_Murattal_128kbps/"),
        Reciter(9, "Abu Bakr Ash-Shaatree", "Melancholic and emotionally moving", "https://everyayah.com/data/Abu_Bakr_Ash-Shaatree_128kbps/"),
        Reciter(10, "Muhammad Ayyoub", "Classical Hijazi spiritual tone", "https://everyayah.com/data/Muhammad_Ayyoub_128kbps/"),
        Reciter(11, "Hani Ar-Rifai", "Heart-trembling and tearful", "https://everyayah.com/data/Hani_Rifai_192kbps/"),
        Reciter(12, "Abdullah Basfar", "Calm, precise, and measured", "https://everyayah.com/data/Abdullah_Basfar_192kbps/")
    )

    val hijriEvents = listOf(
        HijriEvent("Day of Arafah", "9 Dhul-Hijjah 1447", 0, MatteGold),
        HijriEvent("Eid al-Adha", "10 Dhul-Hijjah 1447", 1, FajrColor),
        HijriEvent("Islamic New Year", "1 Muharram 1448", 20, SunriseColor),
        HijriEvent("Ashura", "10 Muharram 1448", 29, MaghribColor),
        HijriEvent("Mawlid al-Nabi", "12 Rabi' al-Awwal 1448", 90, IshaColor),
        HijriEvent("Ramadan Begines", "1 Ramadan 1448", 260, GlowGold),
        HijriEvent("Eid al-Fitr", "1 Shawwal 1448", 290, MatteGold)
    )

    val azkarList = listOf(
        // MORNING AZKAR
        Azkar(
            id = 1,
            category = "Morning",
            title = "Ayat al-Kursi",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth...",
            reference = "Recite once in the morning. Protection from jinns until evening. [Al-Baqarah 255]",
            targetCount = 1
        ),
        Azkar(
            id = 2,
            category = "Morning",
            title = "Praise & Ultimate Devotion",
            arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            translation = "We have entered a new day and with it all dominion belongs to Allah. Praise is to Allah. None has the right to be worshiped but Allah alone, having no partner...",
            reference = "Recite once. Bestows immense gratitude and alignment of purpose. [Muslim]",
            targetCount = 1
        ),
        Azkar(
            id = 3,
            category = "Morning",
            title = "Sayyid-ul-Istighfar (The Master Supplication)",
            arabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            translation = "O Allah, You are my Lord, there is no deity except You. You created me and I am Your servant, and I am faithful to Your covenant and promise as much as I can...",
            reference = "If recited with conviction during day and passes away before evening, enters Paradise. [Bukhari]",
            targetCount = 1
        ),
        Azkar(
            id = 4,
            category = "Morning",
            title = "Protection from Harm",
            arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            translation = "In the name of Allah with Whose name nothing can harm on earth or in the heaven, and He is the All-Hearing, the All-Knowing.",
            reference = "Recite 3 times. Whosoever recites this will not suffer any sudden affliction. [Abu Dawud]",
            targetCount = 3
        ),
        Azkar(
            id = 9,
            category = "Morning",
            title = "Surah Al-Ikhlas",
            arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
            translation = "Say: He is Allah, the One; Allah, the Eternal Refuge. He neither begets nor is born, and none is comparable to Him.",
            reference = "Recite 3 times in the morning. Together with Al-Falaq and An-Nas, it suffices as protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 10,
            category = "Morning",
            title = "Surah Al-Falaq",
            arabic = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            translation = "Say: I seek refuge in the Lord of daybreak from the evil of what He created, from darkness, sorcery, and envy.",
            reference = "Recite 3 times in the morning for protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 11,
            category = "Morning",
            title = "Surah An-Nas",
            arabic = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
            translation = "Say: I seek refuge in the Lord, King, and God of mankind from the evil of the retreating whisperer.",
            reference = "Recite 3 times in the morning for protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 12,
            category = "Morning",
            title = "Contentment with Allah, Islam & the Prophet",
            arabic = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ نَبِيًّا",
            translation = "I am pleased with Allah as my Lord, Islam as my religion, and Muhammad as my Prophet.",
            reference = "Recite 3 times in the morning. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 13,
            category = "Morning",
            title = "Well-being in This Life and the Hereafter",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            translation = "O Allah, I ask You for pardon and well-being in this life and the Hereafter.",
            reference = "A comprehensive morning supplication for pardon and protection. [Abu Dawud, Ibn Majah]",
            targetCount = 1
        ),
        Azkar(
            id = 14,
            category = "Morning",
            title = "By Allah We Enter the Morning",
            arabic = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
            translation = "O Allah, by You we enter the morning and evening, by You we live and die, and to You is the resurrection.",
            reference = "Recite once in the morning. [Tirmidhi]",
            targetCount = 1
        ),

        // EVENING AZKAR
        Azkar(
            id = 5,
            category = "Evening",
            title = "Ayat al-Kursi (Evening)",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth...",
            reference = "Recite once in the evening. Protected from jinns until morning. [Al-Baqarah 255]",
            targetCount = 1
        ),
        Azkar(
            id = 6,
            category = "Evening",
            title = "Gratitude & Completion",
            arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            translation = "We have entered the evening and with it all dominion belongs to Allah. Praise is to Allah. None has the right to be worshiped but Allah alone...",
            reference = "Recite once. Bestows peace of mind for the dark hours. [Muslim]",
            targetCount = 1
        ),
        Azkar(
            id = 7,
            category = "Evening",
            title = "Atonement & Tasbih",
            arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            translation = "Glory be to Allah and His is the praise.",
            reference = "Recite 100 times. Sins are forgiven even if they were like the foam of the sea. [Bukhari]",
            targetCount = 100
        ),
        Azkar(
            id = 8,
            category = "Evening",
            title = "Curing affliction",
            arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            translation = "I seek refuge in the perfect words of Allah from the evil of that which He has created.",
            reference = "Recite 3 times. No harm or poisonous insect bite will hurt during the night. [Ahmad]",
            targetCount = 3
        ),
        Azkar(
            id = 15,
            category = "Evening",
            title = "Surah Al-Ikhlas",
            arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
            translation = "Say: He is Allah, the One; Allah, the Eternal Refuge. He neither begets nor is born, and none is comparable to Him.",
            reference = "Recite 3 times in the evening. Together with Al-Falaq and An-Nas, it suffices as protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 16,
            category = "Evening",
            title = "Surah Al-Falaq",
            arabic = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            translation = "Say: I seek refuge in the Lord of daybreak from the evil of what He created, from darkness, sorcery, and envy.",
            reference = "Recite 3 times in the evening for protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 17,
            category = "Evening",
            title = "Surah An-Nas",
            arabic = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
            translation = "Say: I seek refuge in the Lord, King, and God of mankind from the evil of the retreating whisperer.",
            reference = "Recite 3 times in the evening for protection. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 18,
            category = "Evening",
            title = "Contentment with Allah, Islam & the Prophet",
            arabic = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ نَبِيًّا",
            translation = "I am pleased with Allah as my Lord, Islam as my religion, and Muhammad as my Prophet.",
            reference = "Recite 3 times in the evening. [Abu Dawud, Tirmidhi]",
            targetCount = 3
        ),
        Azkar(
            id = 19,
            category = "Evening",
            title = "Well-being in This Life and the Hereafter",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            translation = "O Allah, I ask You for pardon and well-being in this life and the Hereafter.",
            reference = "A comprehensive evening supplication for pardon and protection. [Abu Dawud, Ibn Majah]",
            targetCount = 1
        ),
        Azkar(
            id = 20,
            category = "Evening",
            title = "By Allah We Enter the Evening",
            arabic = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ",
            translation = "O Allah, by You we enter the evening and morning, by You we live and die, and to You is the return.",
            reference = "Recite once in the evening. [Tirmidhi]",
            targetCount = 1
        )
    )

    val juzAyahsCount = mapOf(
        1 to 148, 2 to 111, 3 to 126, 4 to 131, 5 to 124,
        6 to 110, 7 to 149, 8 to 142, 9 to 159, 10 to 127,
        11 to 151, 12 to 170, 13 to 154, 14 to 227, 15 to 185,
        16 to 269, 17 to 190, 18 to 202, 19 to 339, 20 to 171,
        21 to 178, 22 to 169, 23 to 357, 24 to 175, 25 to 246,
        26 to 195, 27 to 399, 28 to 137, 29 to 431, 30 to 564
    )
    
    val surahAyahsCount = mapOf(
        1 to 7, 2 to 286, 3 to 200, 4 to 176, 5 to 120,
        6 to 165, 7 to 206, 8 to 75, 9 to 129, 10 to 109,
        11 to 123, 12 to 111, 13 to 43, 14 to 52, 15 to 99,
        16 to 128, 17 to 111, 18 to 110, 19 to 98, 20 to 135,
        21 to 112, 22 to 78, 23 to 118, 24 to 64, 25 to 77,
        26 to 227, 27 to 93, 28 to 88, 29 to 69, 30 to 60,
        31 to 34, 32 to 30, 33 to 73, 34 to 54, 35 to 45,
        36 to 83, 37 to 182, 38 to 88, 39 to 75, 40 to 85,
        41 to 54, 42 to 53, 43 to 89, 44 to 59, 45 to 37,
        46 to 35, 47 to 38, 48 to 29, 49 to 18, 50 to 45,
        51 to 60, 52 to 49, 53 to 62, 54 to 55, 55 to 78,
        56 to 96, 57 to 29, 58 to 22, 59 to 24, 60 to 13,
        61 to 14, 62 to 11, 63 to 11, 64 to 18, 65 to 12,
        66 to 12, 67 to 30, 68 to 52, 69 to 52, 70 to 44,
        71 to 28, 72 to 28, 73 to 20, 74 to 56, 75 to 40,
        76 to 31, 77 to 50, 78 to 40, 79 to 46, 80 to 42,
        81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22,
        86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20,
        91 to 15, 92 to 21, 93 to 11, 94 to 8, 95 to 8,
        96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11,
        101 to 11, 102 to 8, 103 to 3, 104 to 9, 105 to 5,
        106 to 4, 107 to 7, 108 to 3, 109 to 6, 110 to 3,
        111 to 5, 112 to 4, 113 to 5, 114 to 6
    )
    
    val surahs = listOf(
        Surah(
            id = 1,
            nameEnglish = "Al-Fatihah",
            nameArabic = "الفاتحة",
            englishTranslation = "The Opening",
            versesCount = 7,
            type = "Meccan",
            verses = listOf(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" to "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ" to "[All] praise is [due] to Allah, Lord of the worlds -",
                "الرَّحْمَٰنِ الرَّحِيمِ" to "The Entirely Merciful, the Especially Merciful,",
                "مَالِكِ يَوْمِ الدِّينِ" to "Sovereign of the Day of Recompense.",
                "إِيَّاكُ نَعْبُدُ وَإِيَّاكُ نَسْتَعِينُ" to "It is You we worship and You we ask for help.",
                "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ" to "Guide us to the straight path -",
                "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ" to "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray."
            )
        ),
        Surah(
            id = 18,
            nameEnglish = "Al-Kahf",
            nameArabic = "الكهف",
            englishTranslation = "The Cave",
            versesCount = 110,
            type = "Meccan",
            verses = listOf(
                "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل| لَهُ عِوَجًا" to "[All] praise is [due] to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.",
                "قَيِّمًا لِيُنْذِرَ بَأْسًا شَدِيدًا مِنْ لَدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا" to "Directly pointing, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward.",
                "مَاكِثِينَ فِيهِ أَبَدًا" to "In which they will remain forever.",
                "وَيُنْذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا" to "And to warn those who say, 'Allah has taken a son.'",
                "مَا لَهُمْ بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ إِنْ يَقُولُونَ إِلَّا كَذِبًا" to "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie."
            )
        ),
        Surah(
            id = 36,
            nameEnglish = "Ya-Sin",
            nameArabic = "يس",
            englishTranslation = "Ya Seen",
            versesCount = 83,
            type = "Meccan",
            verses = listOf(
                "يس" to "Ya, Seen.",
                "وَالْقُرْآنِ الْحَكِيمِ" to "By the wise Qur'an,",
                "إِنَّكَ لَمِنَ الْمُرْسَلِينَ" to "Indeed you, [O Muhammad], are from among the messengers,",
                "عَلَىٰ صِرَاطٍ مُسْتَقِيمٍ" to "On a straight path.",
                "تَنْزِيلَ الْعَزِيزِ الرَّحِيمِ" to "[This is] a revelation of the Exalted in Might, the Merciful,"
            )
        ),
        Surah(
            id = 55,
            nameEnglish = "Ar-Rahman",
            nameArabic = "الرحمن",
            englishTranslation = "The Beneficent",
            versesCount = 78,
            type = "Medinan",
            verses = listOf(
                "الرَّحْمَٰنُ" to "The Most Merciful",
                "عَلَّمَ الْقُرْآنَ" to "Taught the Qur'an,",
                "خَلَقَ الْإِنْسَانَ" to "Created man,",
                "عَلَّمَهُ الْبَيَانَ" to "Taught him eloquence.",
                "الشَّمْسُ وَالْقَمَرُ بِحُسْبَانٍ" to "The sun and the moon [move] by precise calculation,"
            )
        ),
        Surah(
            id = 67,
            nameEnglish = "Al-Mulk",
            nameArabic = "الملك",
            englishTranslation = "The Sovereignty",
            versesCount = 30,
            type = "Meccan",
            verses = listOf(
                "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ" to "Blessed is He in whose hand is dominion, and He is over all things competent -",
                "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا وَهُوَ الْعَزِيزُ الْغَفُورُ" to "[He] who created death and life to test you [as to] which of you is best in deed - and He is the Exalted in Might, the Forgiving -",
                "الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ فَارْجِعِ الْبَصَرَ هَلْ تَرَىٰ مِن فُطُورٍ" to "[And] who created seven heavens in layers. You do not see in the creation of the Most Merciful any inconsistency. So return [your] vision; do you see any breaks?"
            )
        ),
        Surah(
            id = 112,
            nameEnglish = "Al-Ikhlas",
            nameArabic = "الإخلاص",
            englishTranslation = "The Sincerity",
            versesCount = 4,
            type = "Meccan",
            verses = listOf(
                "قُلْ هُوَ اللَّهُ أَحَدٌ" to "Say, 'He is Allah, [who is] One,",
                "اللَّهُ الصَّمَدُ" to "Allah, the Eternal Refuge.",
                "لَمْ يَلِدْ وَلَمْ يُولَدْ" to "He neither begets nor is He begotten,",
                "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ" to "And there is none co-equal or comparable unto Him.'"
            )
        ),
        Surah(
            id = 113,
            nameEnglish = "Al-Falaq",
            nameArabic = "الفلق",
            englishTranslation = "The Daybreak",
            versesCount = 5,
            type = "Meccan",
            verses = listOf(
                "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ" to "Say, 'I seek refuge in the Lord of daybreak",
                "مِنْ شَرِّ مَا خَلَقَ" to "From the evil of that which He created",
                "وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ" to "And from the evil of darkness when it settles",
                "مِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ" to "And from the evil of the blowers in knots",
                "وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ" to "And from the evil of an envier when he envies.'"
            )
        ),
        Surah(
            id = 114,
            nameEnglish = "An-Nas",
            nameArabic = "الناس",
            englishTranslation = "The Mankind",
            versesCount = 6,
            type = "Meccan",
            verses = listOf(
                "قُلْ أَعُوذُ بِرَبِّ النَّاسِ" to "Say, 'I seek refuge in the Lord of mankind,",
                "مَلِكِ النَّاسِ" to "The Sovereign of mankind,",
                "إِلَٰهِ النَّاسِ" to "The God of mankind,",
                "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ" to "From the evil of the retreating whisperer -",
                "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ" to "Who whispers [evil] into the breasts of mankind -",
                "مِنَ الْجِنَّةِ وَالنَّاسِ" to "From among the jinn and mankind.'"
            )
        )
    )

    val defaultPrayers = listOf(
        PrayerTime("Fajr", "04:35", FajrColor),
        PrayerTime("Sunrise", "05:58", SunriseColor),
        PrayerTime("Dhuhr", "12:15", DhuhrColor),
        PrayerTime("Asr", "15:38", AsrColor),
        PrayerTime("Maghrib", "18:28", MaghribColor),
        PrayerTime("Isha", "19:48", IshaColor)
    )

    val quranQuotes = listOf(
        "Indeed, with hardship [will be] ease. (94:6)" to "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
        "So remember Me; I will remember you. (2:152)" to "فَاذْكُرُونِي أَذْكُرْكُمْ",
        "And Allah is the best of providers. (62:11)" to "وَاللَّهُ خَيْرُ الرَّازِقِينَ",
        "My Mercy encompasses all things. (7:156)" to "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ"
    )
}
