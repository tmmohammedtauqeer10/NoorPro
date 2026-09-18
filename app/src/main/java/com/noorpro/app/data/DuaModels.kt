package com.noorpro.app.data

data class DuaItem(
    val id: Int,
    val title: String,
    val arabic: String,
    val english: String,
    val urdu: String,      // Kanz-ul-Iman style
    val romanUrdu: String,
    val targetCount: Int
)

data class DuaCategory(
    val id: String,
    val title: String,
    val description: String,
    val assetAnimation: String,
    val remoteUrl: String,
    val iconName: String,
    val duas: List<DuaItem>
)

object DuaData {
    val categories = listOf(
        DuaCategory(
            id = "morning",
            title = "Morning Invocations",
            description = "Al-Azkar as-Sabah to start your day with divine light",
            assetAnimation = "animations/morning.json",
            remoteUrl = "https://assets5.lottiefiles.com/packages/lf20_yg9b8m8g.json",
            iconName = "wb_sunny",
            duas = listOf(
                DuaItem(
                    id = 101,
                    title = "Seeking Protection from All Harm",
                    arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
                    english = "In the name of Allah, with Whose name nothing can cause harm in the earth nor in the heaven, and He is the All-Hearing, the All-Knowing.",
                    urdu = "اللہ کے نام سے جس کے نام کی برکت سے زمین اور آسمان میں کوئی چیز نقصان نہیں پہنچا سکتی اور وہی خوب سننے والا اور جاننے والا ہے۔",
                    romanUrdu = "Bismillahil-lazi la yadurru ma'as-mihi shai'un fil-ardi wa la fis-sama'i, wa Huwas-Sami'ul-'Alim.",
                    targetCount = 3
                ),
                DuaItem(
                    id = 102,
                    title = "Dua for General Well-being & Guidance",
                    arabic = "اللَّهُمَّ عافِنِي فِي بَدَنِي، اللَّهُمَّ عافِنِي فِي سَمْعِي، اللَّهُمَّ عافِنِي فِي بَصَرِي، لا إِلَهَ إِلاَّ أَنْتَ",
                    english = "O Allah, grant me health in my body. O Allah, grant me health in my hearing. O Allah, grant me health in my sight. There is no deity except You.",
                    urdu = "اے اللہ! میرے بدن میں تندرستی عطا فرما۔ اے اللہ! میرے کانوں میں عافیت بخش۔ اے اللہ! میری بینائی میں تندرستی دے۔ تیرے سوا کوئی معبود نہیں ہے۔",
                    romanUrdu = "Allahumma 'afini fi badani. Allahumma 'afini fi sam'i. Allahumma 'afini fi basari. La ilaha illa Anta.",
                    targetCount = 3
                ),
                DuaItem(
                    id = 103,
                    title = "Master of the Heavens — Morning Trust",
                    arabic = "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ",
                    english = "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You is the resurrection.",
                    urdu = "اے اللہ! تیری مدد سے ہم نے صبح کی اور تیری مدد سے شام کی، تیری ہی مدد سے ہم جیتے اور مرتے ہیں اور تیری ہی طرف اٹھ کر جانا ہے۔",
                    romanUrdu = "Allahumma bika asbahna wa bika amsayna wa bika nahya wa bika namutu wa ilaykan-nushur.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 104,
                    title = "Pleased with Allah, Islam & the Prophet ﷺ",
                    arabic = "رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
                    english = "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad (peace be upon him) as my Prophet.",
                    urdu = "میں راضی ہوں اللہ کے رب ہونے پر، اسلام کے دین ہونے پر اور محمد ﷺ کے نبی ہونے پر۔",
                    romanUrdu = "Raditu billahi Rabban, wa bil-Islami dinan, wa bi-Muhammadin sallallahu 'alayhi wa sallama Nabiyya.",
                    targetCount = 3
                )
            )
        ),
        DuaCategory(
            id = "evening",
            title = "Evening Invocations",
            description = "Al-Azkar al-Masaa for absolute tranquil sleep and peace",
            assetAnimation = "animations/evening.json",
            remoteUrl = "https://assets3.lottiefiles.com/packages/lf20_atbyunof.json",
            iconName = "dark_mode",
            duas = listOf(
                DuaItem(
                    id = 201,
                    title = "Praise & Complete Surrender to Allah",
                    arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
                    english = "We have entered the evening, and processing sovereignty remains with Allah. Praise be to Allah. There is no deity but Allah, alone, without partner.",
                    urdu = "ہم نے شام کی اور شام کے وقت سارا ملک اللہ ہی کا رہا اور سب تعریفیں اللہ کے لیے ہیں۔ اللہ کے سوا کوئی سچا معبود نہیں، وہ اکیلا ہے اس کا کوئی شریک نہیں ہے۔",
                    romanUrdu = "Amsayna wa amsal-mulku lillah, wal-hamdu lillah. La ilaha illallahu wahdahu la sharika lah.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 202,
                    title = "Seeking Refuge from Evil in Creation",
                    arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
                    english = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
                    urdu = "میں اللہ تعالیٰ کے کلماتِ تامہ کی پناہ چاہتا ہوں ان تمام چیزوں کے شر سے جو اس نے پیدا کی ہیں۔",
                    romanUrdu = "A'uzu bi-kalimatillahit-tammati min sharri ma khalaq.",
                    targetCount = 3
                )
            )
        ),
        DuaCategory(
            id = "mosque",
            title = "Prayer & Mosque",
            description = "Aura of light when walking to and entering the sacred space",
            assetAnimation = "animations/mosque.json",
            remoteUrl = "https://assets7.lottiefiles.com/packages/lf20_49r9krp3.json",
            iconName = "mosque",
            duas = listOf(
                DuaItem(
                    id = 301,
                    title = "Entering the Sacred Mosque",
                    arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
                    english = "O Allah, open for me the gates of Your mercy.",
                    urdu = "اے اللہ! میرے لیے اپنی رحمت کے دروازے کھول دے۔",
                    romanUrdu = "Allahummaf-tah li abwaba rahmatik.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 302,
                    title = "Exiting the Sacred Mosque",
                    arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
                    english = "O Allah, indeed I ask You of Your bounty.",
                    urdu = "اے اللہ! میں تجھ سے تیرے فضل کا سوال کرتا ہوں۔",
                    romanUrdu = "Allahumma inni as'aluka min fadlik.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "protection",
            title = "Protection & Healing",
            description = "Al-Ruqyah ash-Shar'eyah for safeguarding with divine shield",
            assetAnimation = "animations/protection.json",
            remoteUrl = "https://assets4.lottiefiles.com/packages/lf20_beu9z5u2.json",
            iconName = "security",
            duas = listOf(
                DuaItem(
                    id = 401,
                    title = "Healing of Pain & Affliction",
                    arabic = "أَذْهِبِ الْبَاسَ رَبَّ النَّاسِ، وَاشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
                    english = "Remove the severity, O Lord of mankind, and cure! You are the One Who cures. There is no cure but Your cure, a cure that leaves no illness.",
                    urdu = "اے انسانوں کے پالنے والے! تکلیف کو دور کر دے، شفا عطا فرما، تو ہی شفا دینے والا ہے۔ تیرے سوا کوئی شفا نہیں، ایسی شفا دے کہ بالکل بیماری نہ رہے۔",
                    romanUrdu = "Azhibil-ba'sa Rabban-nas, washfi Antash-Shafi, la shifa'a illa shifa'uk, shifa'an la yugadiru saqama.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 402,
                    title = "Seeking Refuge from Pain",
                    arabic = "أَعُوذُ بِعِزَّةِ اللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ",
                    english = "I seek refuge in the might and power of Allah from the evil of what I feel and fear.",
                    urdu = "میں اللہ کی عزت اور قدرت کی پناہ مانگتا ہوں اس برائی کے شر سے جسے میں پاتا ہوں اور ڈرتا ہوں۔",
                    romanUrdu = "A'uzu bi'izzatillahi wa qudratihi min sharri ma ajidu wa uhazir.",
                    targetCount = 7
                )
            )
        ),
        DuaCategory(
            id = "relief",
            title = "Anxiety & Relief",
            description = "Al-Faraj prayers to dispel grief and unlock locked doors",
            assetAnimation = "animations/relief.json",
            remoteUrl = "https://assets2.lottiefiles.com/packages/lf20_to8vkyr2.json",
            iconName = "mood",
            duas = listOf(
                DuaItem(
                    id = 501,
                    title = "Alleviating Distress & Anxiety",
                    arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ",
                    english = "O Allah, I seek refuge in You from anxiety and grief, helplessness and laziness, miserliness and cowardice.",
                    urdu = "اے اللہ! میں فکر اور غم سے، عاجزی اور سستی سے، بخل اور بزدلی سے تیری پناہ مانگتا ہوں۔",
                    romanUrdu = "Allahumma inni a'uzu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal, wal-bukhli wal-jubn.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 502,
                    title = "Dua of Prophet Yunus (AS)",
                    arabic = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                    english = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
                    urdu = "الٰہی تیرے سوا کوئی معبود نہیں تو پاک ہے، بیشک میں ہی قصوروار تھا۔",
                    romanUrdu = "La ilaha illa Anta subhanaka inni kuntu minaz-zalimin.",
                    targetCount = 33
                )
            )
        ),
        DuaCategory(
            id = "azkar",
            title = "Tasbeeh & Azkar",
            description = "Daily repetitive remembrance (Zikr) for spiritual elevation",
            assetAnimation = "animations/azkar.json",
            remoteUrl = "https://assets9.lottiefiles.com/packages/lf20_q5pk6p1k.json",
            iconName = "autorenew",
            duas = listOf(
                DuaItem(
                    id = 701,
                    title = "SubhanAllah",
                    arabic = "سُبْحَانَ اللَّهِ",
                    english = "Glory be to Allah",
                    urdu = "اللہ پاک ہے۔",
                    romanUrdu = "SubhanAllah",
                    targetCount = 33
                ),
                DuaItem(
                    id = 702,
                    title = "Alhamdulillah",
                    arabic = "الْحَمْدُ لِلَّهِ",
                    english = "All praise is to Allah",
                    urdu = "سب تعریفیں اللہ کے لیے ہیں۔",
                    romanUrdu = "Alhamdulillah",
                    targetCount = 33
                ),
                DuaItem(
                    id = 703,
                    title = "Allahu Akbar",
                    arabic = "اللَّهُ أَكْبَرُ",
                    english = "Allah is the Greatest",
                    urdu = "اللہ سب سے بڑا ہے۔",
                    romanUrdu = "Allahu Akbar",
                    targetCount = 34
                ),
                DuaItem(
                    id = 704,
                    title = "Astaghfirullah",
                    arabic = "أَسْتَغْفِرُ اللَّهَ",
                    english = "I seek forgiveness from Allah",
                    urdu = "میں اللہ سے بخشش مانگتا ہوں۔",
                    romanUrdu = "Astaghfirullah",
                    targetCount = 100
                ),
                DuaItem(
                    id = 705,
                    title = "La hawla wala quwwata",
                    arabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
                    english = "There is no might nor power except with Allah",
                    urdu = "گناہوں سے بچنے کی طاقت اور نیکی کرنے کی قوت صرف اللہ کی توفیق سے ہے۔",
                    romanUrdu = "La hawla wa la quwwata illa billah",
                    targetCount = 100
                ),
                DuaItem(
                    id = 706,
                    title = "Durood upon the Prophet ﷺ",
                    arabic = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ",
                    english = "O Allah, send blessings upon Muhammad and the family of Muhammad",
                    urdu = "اے اللہ! محمد ﷺ اور آلِ محمد پر درود بھیج۔",
                    romanUrdu = "Allahumma salli 'ala Muhammadin wa 'ala aali Muhammad",
                    targetCount = 100
                ),
                DuaItem(
                    id = 707,
                    title = "SubhanAllahi wa bihamdihi",
                    arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                    english = "Glory and praise be to Allah",
                    urdu = "اللہ پاک ہے اور سب تعریف اسی کے لیے ہے۔",
                    romanUrdu = "SubhanAllahi wa bihamdih",
                    targetCount = 100
                )
            )
        ),
        DuaCategory(
            id = "rizq",
            title = "Rizq & Sustenance",
            description = "Quranic and prophetic duas for lawful provision and barakah",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "restaurant",
            duas = listOf(
                DuaItem(
                    id = 1701,
                    title = "Ask Allah for Lawful Provision",
                    arabic = "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ",
                    english = "O Allah, suffice me with what You have made lawful against what You have made unlawful, and make me independent of all others besides You by Your grace.",
                    urdu = "اے اللہ! اپنے حلال کے ذریعے مجھے حرام سے بچا اور اپنے فضل سے مجھے اپنے سوا ہر ایک سے بے نیاز کر دے۔",
                    romanUrdu = "Allahumma-kfini bi-halalika 'an haramik, wa aghnini bi-fadlika 'amman siwak.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 1702,
                    title = "Barakah in Provision",
                    arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا طَيِّبًا وَعَمَلًا مُتَقَبَّلًا",
                    english = "O Allah, I ask You for beneficial knowledge, good provision, and accepted deeds.",
                    urdu = "اے اللہ! میں تجھ سے نفع بخش علم، پاکیزہ رزق اور مقبول عمل کا سوال کرتا ہوں۔",
                    romanUrdu = "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbala.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "hardship",
            title = "Hardship & Difficulty",
            description = "Steadfast duas for trials, debt, and moments of distress",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "mood",
            duas = listOf(
                DuaItem(
                    id = 1801,
                    title = "Relief from Debt & Worry",
                    arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَأَعُوذُ بِكَ مِنْ غَلَبَةِ الدَّيْنِ وَقَهْرِ الرِّجَالِ",
                    english = "O Allah, I seek refuge in You from anxiety and grief, and from being overcome by debt and overpowered by men.",
                    urdu = "اے اللہ! میں فکر اور غم سے تیری پناہ مانگتا ہوں، اور قرض کے غلبے اور لوگوں کے دباؤ سے تیری پناہ چاہتا ہوں۔",
                    romanUrdu = "Allahumma inni a'uzu bika minal-hammi wal-hazan, wa a'uzu bika min ghalabatid-dayni wa qahrir-rijal.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 1802,
                    title = "When Affairs Become Difficult",
                    arabic = "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
                    english = "O Allah, there is no ease except in what You make easy, and You make the difficult easy if You will.",
                    urdu = "اے اللہ! آسانی صرف وہی ہے جسے تو آسان بنا دے، اور تو چاہے تو مشکل کو بھی آسان کر دیتا ہے۔",
                    romanUrdu = "Allahumma la sahla illa ma ja'altahu sahla, wa Anta taj'alul-hazna iza shi'ta sahla.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "travel",
            title = "Travel & Journey",
            description = "Al-Safar protective shielding for the traveler and family",
            assetAnimation = "animations/travel.json",
            remoteUrl = "https://assets8.lottiefiles.com/packages/lf20_pdr9f6f6.json",
            iconName = "explore",
            duas = listOf(
                DuaItem(
                    id = 601,
                    title = "Mounting & Setting Out for Journey",
                    arabic = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ",
                    english = "Glorified is He Who has subjected this for us, and we could not have otherwise subdued it. And indeed, to our Lord we will return.",
                    urdu = "پاک ہے وہ ذات جس نے اس مٹی/سواری کو ہمارے قابو میں دے دیا حالانکہ ہم اسے قابو کرنے والے نہ تھے اور بیشک ہم اپنے رب ہی کی طرف لوٹنے والے ہیں۔",
                    romanUrdu = "Subhanal-lazi sakh-khara lana haza wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 602,
                    title = "Dua for Safe Return & Blessings",
                    arabic = "آيِبُونَ تَائِبُونَ عَابِدُونَ لِرَبِّنَا حَامِدُونَ",
                    english = "We return, repenting, worshiping, and praising our Lord.",
                    urdu = "ہم رجوع کرنے والے، توبہ کرنے والے، عبادت کرنے والے اور اپنے رب کی حمد بیان کرنے والے ہیں۔",
                    romanUrdu = "Ayibuna, ta'ibuna, 'abiduna, li-Rabbina hamidun.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "quranic",
            title = "Quranic Supplications",
            description = "Selected beautiful duas directly from the Holy Quran",
            assetAnimation = "animations/quranic.json",
            remoteUrl = "https://assets1.lottiefiles.com/packages/lf20_7wwknbdv.json",
            iconName = "mosque",
            duas = listOf(
                DuaItem(
                    id = 801,
                    title = "Dua for Success & Goodness in Both Worlds",
                    arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                    english = "Our Lord, give us in this world [that which is] good and in the Hereafter [that which is] good and protect us from the punishment of the Fire.",
                    urdu = "اے ہمارے پروردگار! ہمیں دنیا میں بھی بھلائی دے اور آخرت میں بھی بھلائی عطا فرما اور ہمیں عذاب جہنم سے بچا۔",
                    romanUrdu = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'azaban-nar.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 802,
                    title = "Dua for Steadfastness",
                    arabic = "رَبَّنَا لاَ تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً إِنَّكَ أَنتَ الْوَهَّابُ",
                    english = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy. Indeed, You are the Bestower.",
                    urdu = "اے ہمارے رب! ہمیں ہدایت دینے کے بعد ہمارے دلوں کو ٹیڑھا نہ کر اور ہمیں اپنے پاس سے رحمت عطا فرما، بیشک تو ہی بہت بڑا عطا کرنے والا ہے۔",
                    romanUrdu = "Rabbana la tuzigh qulubana ba'da iz hadaitana wa hab lana min ladunka rahmatan innaka Antal-Wahhab.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "prophetic",
            title = "Prophetic (Hadith)",
            description = "Authentic supplications taught by Prophet Muhammad (PBUH)",
            assetAnimation = "animations/hadith.json",
            remoteUrl = "https://assets5.lottiefiles.com/packages/lf20_atbyunof.json",
            iconName = "mood",
            duas = listOf(
                DuaItem(
                    id = 901,
                    title = "Sayyid ul-Istighfar (Chief of Forgiveness)",
                    arabic = "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لا يَغْفِرُ الذُّنُوبَ إِلا أَنْتَ",
                    english = "O Allah, You are my Lord, there is no deity but You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for none forgives sins except You.",
                    urdu = "اے اللہ! تو ہی میرا رب ہے، تیرے سوا کوئی معبود نہیں۔ تو نے مجھے پیدا کیا اور میں تیرا بندہ ہوں، اور میں اپنی طاقت کے مطابق تیرے عہد اور وعدے پر قائم ہوں۔ میں اپنے کیے کے شر سے تیری پناہ مانگتا ہوں۔ میں اپنے اوپر تیری نعمتوں کا اقرار کرتا ہوں اور اپنے گناہ کا اعتراف کرتا ہوں، لہٰذا مجھے بخش دے، کیونکہ تیرے سوا کوئی گناہوں کو نہیں بخش سکتا۔",
                    romanUrdu = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika masta-ta'tu... ",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "food",
            title = "Food & Gratitude",
            description = "Simple sunnah duas before and after meals",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "restaurant",
            duas = listOf(
                DuaItem(1001, "Before Eating", "بِسْمِ اللَّهِ", "In the name of Allah.", "اللہ کے نام سے۔", "Bismillah.", 1),
                DuaItem(1002, "After Eating", "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "Praise is to Allah who fed me this and provided it without any strength or power from me.", "سب تعریف اللہ کے لیے ہے جس نے مجھے یہ کھلایا۔", "Alhamdu lillahil-ladhi at'amani hadha wa razaqanihi min ghairi hawlin minni wa la quwwah.", 1)
            )
        ),
        DuaCategory(
            id = "family",
            title = "Family & Children",
            description = "Prayers for a peaceful home and righteous family",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "family",
            duas = listOf(
                DuaItem(1101, "Comfort in Family", "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا", "Our Lord, grant us comfort in our spouses and children and make us leaders for the righteous.", "اے ہمارے رب! ہمیں ہمارے گھر والوں سے آنکھوں کی ٹھنڈک عطا فرما۔", "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yun.", 1),
                DuaItem(1102, "Righteous Children", "رَبِّ هَبْ لِي مِنَ الصَّالِحِينَ", "My Lord, grant me righteous offspring.", "اے میرے رب! مجھے نیک اولاد عطا فرما۔", "Rabbi hab li minas-salihin.", 1)
            )
        ),
        DuaCategory(
            id = "daily_life",
            title = "Daily Life",
            description = "Short sunnah duas for everyday moments",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "explore",
            duas = listOf(
                DuaItem(1301, "Leaving Home", "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "In the name of Allah, I place my trust in Allah; there is no power or strength except through Allah.", "اللہ کے نام سے، میں نے اللہ پر بھروسا کیا، طاقت اور قوت صرف اللہ سے ہے۔", "Bismillah, tawakkaltu 'alallah, la hawla wa la quwwata illa billah.", 1),
                DuaItem(1302, "Entering Home", "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى رَبِّنَا تَوَكَّلْنَا", "In the name of Allah we enter, in the name of Allah we leave, and upon our Lord we rely.", "اللہ کے نام سے ہم داخل ہوئے، اللہ کے نام سے نکلے، اور اپنے رب پر بھروسا کیا۔", "Bismillahi walajna, wa bismillahi kharajna, wa 'ala Rabbina tawakkalna.", 1),
                DuaItem(1303, "Entering the Restroom", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ", "O Allah, I seek refuge in You from evil and impure beings.", "اے اللہ! میں ناپاک اور بری مخلوق سے تیری پناہ مانگتا ہوں۔", "Allahumma inni a'udhu bika minal-khubuthi wal-khaba'ith.", 1),
                DuaItem(1304, "Leaving the Restroom", "غُفْرَانَكَ", "I seek Your forgiveness.", "اے اللہ! میں تیری بخشش چاہتا ہوں۔", "Ghufranak.", 1)
            )
        ),
        DuaCategory(
            id = "knowledge",
            title = "Knowledge & Guidance",
            description = "Quranic prayers for wisdom, clarity, and steadfast faith",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "mosque",
            duas = listOf(
                DuaItem(1401, "Increase Me in Knowledge", "رَبِّ زِدْنِي عِلْمًا", "My Lord, increase me in knowledge.", "اے میرے رب! میرے علم میں اضافہ فرما۔", "Rabbi zidni 'ilma.", 1),
                DuaItem(1402, "Ease My Task and Speech", "رَبِّ اشْرَحْ لِي صَدْرِي ۝ وَيَسِّرْ لِي أَمْرِي ۝ وَاحْلُلْ عُقْدَةً مِن لِّسَانِي ۝ يَفْقَهُوا قَوْلِي", "My Lord, expand my chest, ease my task, and untie the knot from my tongue so people may understand my speech.", "اے میرے رب! میرا سینہ کھول دے، میرا کام آسان کر دے، اور میری زبان کی گرہ کھول دے۔", "Rabbish-rah li sadri, wa yassir li amri, wahlul 'uqdatan min lisani, yafqahu qawli.", 1),
                DuaItem(1403, "Guidance and Piety", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى", "O Allah, I ask You for guidance, piety, chastity, and contentment.", "اے اللہ! میں تجھ سے ہدایت، تقویٰ، پاک دامنی اور بے نیازی مانگتا ہوں۔", "Allahumma inni as'alukal-huda wat-tuqa wal-'afafa wal-ghina.", 1),
                DuaItem(1404, "Keep My Heart Firm", "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ", "O Turner of hearts, keep my heart firm upon Your religion.", "اے دلوں کو پھیرنے والے! میرے دل کو اپنے دین پر ثابت رکھ۔", "Ya Muqallibal-qulub, thabbit qalbi 'ala dinik.", 1)
            )
        ),
        DuaCategory(
            id = "sleep",
            title = "Sleep & Waking",
            description = "End the day in remembrance and begin with gratitude",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "bedtime",
            duas = listOf(
                DuaItem(1201, "Before Sleeping", "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "In Your name, O Allah, I die and I live.", "اے اللہ! تیرے نام کے ساتھ مرتا اور جیتا ہوں۔", "Bismika Allahumma amutu wa ahya.", 1),
                DuaItem(1202, "Upon Waking", "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "Praise is to Allah who gave us life after causing us to die, and to Him is the resurrection.", "سب تعریف اللہ کے لیے ہے جس نے ہمیں موت کے بعد زندگی دی۔", "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilaihin-nushur.", 1)
            )
        ),
        DuaCategory(
            id = "repentance",
            title = "Forgiveness & Repentance",
            description = "Powerful duas for tawbah, mercy, and a clean heart",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "security",
            duas = listOf(
                DuaItem(
                    id = 1501,
                    title = "Accept My Repentance",
                    arabic = "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
                    english = "My Lord, forgive me and accept my repentance; You are the Ever-Relenting, Most Merciful.",
                    urdu = "اے میرے رب! مجھے بخش دے اور میری توبہ قبول فرما، بے شک تو بہت توبہ قبول کرنے والا، نہایت رحم کرنے والا ہے۔",
                    romanUrdu = "Rabbi-ghfir li wa tub 'alayya innaka Antat-Tawwabur-Rahim.",
                    targetCount = 100
                ),
                DuaItem(
                    id = 1502,
                    title = "Night Of Forgiveness",
                    arabic = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
                    english = "O Allah, You are Pardoning and love pardon, so pardon me.",
                    urdu = "اے اللہ! تو معاف کرنے والا ہے، معافی کو پسند فرماتا ہے، پس مجھے معاف فرما۔",
                    romanUrdu = "Allahumma innaka 'afuwwun tuhibbul-'afwa fa'fu 'anni.",
                    targetCount = 3
                ),
                DuaItem(
                    id = 1503,
                    title = "Daily Istighfar",
                    arabic = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
                    english = "I seek Allah's forgiveness and turn to Him in repentance.",
                    urdu = "میں اللہ سے بخشش مانگتا ہوں اور اسی کی طرف رجوع کرتا ہوں۔",
                    romanUrdu = "Astaghfirullah wa atubu ilayh.",
                    targetCount = 100
                ),
                DuaItem(
                    id = 1504,
                    title = "A Heart Made Pure",
                    arabic = "اللَّهُمَّ طَهِّرْ قَلْبِي مِنَ النِّفَاقِ وَعَمَلِي مِنَ الرِّيَاءِ",
                    english = "O Allah, purify my heart from hypocrisy and my deeds from showing off.",
                    urdu = "اے اللہ! میرے دل کو نفاق سے اور میرے اعمال کو دکھاوے سے پاک فرما۔",
                    romanUrdu = "Allahumma tahhir qalbi minan-nifaqi wa 'amali minar-riya.",
                    targetCount = 1
                )
            )
        ),
        DuaCategory(
            id = "parents",
            title = "Parents & Mercy",
            description = "Quranic duas for parents, family, gratitude, and mercy",
            assetAnimation = "",
            remoteUrl = "",
            iconName = "family",
            duas = listOf(
                DuaItem(
                    id = 1601,
                    title = "Mercy For Parents",
                    arabic = "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
                    english = "My Lord, have mercy upon them as they raised me when I was small.",
                    urdu = "اے میرے رب! ان دونوں پر رحم فرما جیسا کہ انہوں نے بچپن میں میری پرورش کی۔",
                    romanUrdu = "Rabbir-hamhuma kama rabbayani saghira.",
                    targetCount = 3
                ),
                DuaItem(
                    id = 1602,
                    title = "Forgive My Parents",
                    arabic = "رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ",
                    english = "Our Lord, forgive me, my parents, and the believers on the Day the account is established.",
                    urdu = "اے ہمارے رب! مجھے، میرے والدین کو اور ایمان والوں کو حساب کے دن بخش دے۔",
                    romanUrdu = "Rabbana-ghfir li wa liwalidayya wa lil-mu'minina yawma yaqumul-hisab.",
                    targetCount = 1
                ),
                DuaItem(
                    id = 1603,
                    title = "Gratitude For Blessings",
                    arabic = "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَىٰ وَالِدَيَّ",
                    english = "My Lord, inspire me to be grateful for Your blessing which You bestowed upon me and my parents.",
                    urdu = "اے میرے رب! مجھے توفیق دے کہ میں تیری اس نعمت کا شکر ادا کروں جو تو نے مجھ پر اور میرے والدین پر فرمائی۔",
                    romanUrdu = "Rabbi awzi'ni an ashkura ni'mataka allati an'amta 'alayya wa 'ala walidayya.",
                    targetCount = 1
                )
            )
        )
    )
}
