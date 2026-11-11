package com.noorpro.app.islamic.kids

/*
 * 1.1.6 Kids content. All English text below is written for NoorPro (own words), retelling what the
 * Qur'an and well-known hadith say, with references so parents can check. Arabic du'a texts are the
 * standard wording from the cited sources. Prophets are never pictured — cards use pattern art only.
 */

data class KidsSurah(val id: Int, val name: String, val arabic: String, val meaning: String, val note: String)

val KIDS_SURAHS = listOf(
    KidsSurah(1, "Al-Fatihah", "الفاتحة", "The Opening", "We recite it in every rak'ah of salah."),
    KidsSurah(112, "Al-Ikhlas", "الإخلاص", "Sincerity", "Allah is One, and nothing is like Him."),
    KidsSurah(113, "Al-Falaq", "الفلق", "The Daybreak", "Asking Allah to protect us from harm."),
    KidsSurah(114, "An-Nas", "الناس", "Mankind", "Asking Allah to protect us from bad whispers."),
    KidsSurah(108, "Al-Kawthar", "الكوثر", "Abundance", "The shortest surah — Allah's gift of plenty."),
    KidsSurah(103, "Al-'Asr", "العصر", "Time", "Use your time for good and be patient."),
    KidsSurah(110, "An-Nasr", "النصر", "The Help", "When help comes from Allah, say thank You."),
    KidsSurah(105, "Al-Fil", "الفيل", "The Elephant", "How Allah protected the Ka'bah."),
    KidsSurah(106, "Quraysh", "قريش", "Quraysh", "Thank Allah for food and safety."),
    KidsSurah(107, "Al-Ma'un", "الماعون", "Small Kindnesses", "Pray with care and help others."),
)

data class KidsDua(val id: String, val title: String, val arabic: String, val transliteration: String, val meaning: String, val source: String)

val KIDS_DUAS = listOf(
    KidsDua("eat", "Before eating", "بِسْمِ اللهِ", "Bismillah", "In the name of Allah.", "Abu Dawud, Tirmidhi"),
    KidsDua("ate", "After eating", "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ", "Alhamdu lillahil-ladhi at'amana wa saqana wa ja'alana muslimin",
        "All praise is for Allah who gave us food and drink and made us Muslims.", "Abu Dawud, Tirmidhi"),
    KidsDua("sleep", "Before sleeping", "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "Bismika Allahumma amutu wa ahya", "In Your name, O Allah, I sleep and I wake.", "Sahih al-Bukhari"),
    KidsDua("wake", "Waking up", "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur",
        "All praise is for Allah who woke us up after sleep, and to Him we return.", "Sahih al-Bukhari"),
    KidsDua("out", "Leaving home", "بِسْمِ اللهِ، تَوَكَّلْتُ عَلَى اللهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ", "Bismillah, tawakkaltu 'alallah, wa la hawla wa la quwwata illa billah",
        "In the name of Allah. I trust Allah. All strength comes from Allah.", "Abu Dawud, Tirmidhi"),
    KidsDua("parents", "For my parents", "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "Rabbir-hamhuma kama rabbayani saghira",
        "My Lord, have mercy on them as they looked after me when I was little.", "Qur'an 17:24"),
    KidsDua("learn", "Before learning", "رَبِّ زِدْنِي عِلْمًا", "Rabbi zidni 'ilma", "My Lord, increase me in knowledge.", "Qur'an 20:114"),
    KidsDua("sneeze", "After sneezing", "الْحَمْدُ لِلَّهِ", "Alhamdu lillah", "All praise is for Allah. (Others reply: Yarhamukallah — may Allah have mercy on you.)", "Sahih al-Bukhari"),
)

data class ProphetStory(val id: String, val name: String, val arabic: String, val paragraphs: List<String>, val lesson: String, val refs: String, val hue: Int)

val PROPHET_STORIES = listOf(
    ProphetStory("adam", "Prophet Adam", "آدم عليه السلام", listOf(
        "Allah created Adam, peace be upon him, the very first human, and taught him the names of all things — even more than the angels knew.",
        "Allah told the angels to bow to Adam out of respect. They all obeyed, except Iblis, who was proud and said he was better.",
        "Adam and his wife Hawwa lived in a beautiful Garden. They could enjoy everything, but Allah asked them not to go near one tree.",
        "Shaytan whispered to them again and again, and one day they ate from it. Straight away they felt sorry.",
        "They did not make excuses. They turned to Allah and said: \"Our Lord, we have wronged ourselves; if You do not forgive us and have mercy on us, we will be lost.\" And Allah forgave them.",
    ), "When we make a mistake, we say sorry to Allah. He loves to forgive.", "Qur'an 2:30–37, 7:11–23", 0),
    ProphetStory("nuh", "Prophet Nuh", "نوح عليه السلام", listOf(
        "Prophet Nuh, peace be upon him, lived among people who had forgotten Allah and worshipped idols.",
        "For hundreds of years he invited them — by day and by night, gently and kindly — to worship Allah alone. Only a few believed.",
        "Allah told Nuh to build a great ark. People laughed at him for building a ship on dry land, but he kept working patiently.",
        "When the flood came, Nuh, the believers and pairs of animals were safe inside the ark, which sailed on waves like mountains.",
        "At last the water went down and the ark came to rest on Mount Judi. Nuh and the believers thanked Allah.",
    ), "Keep doing what is right, even when others laugh. Patience wins in the end.", "Qur'an 11:25–48, 29:14, 71", 1),
    ProphetStory("ibrahim", "Prophet Ibrahim", "إبراهيم عليه السلام", listOf(
        "As a young man, Ibrahim, peace be upon him, looked at a bright star, then the moon, then the sun. Each one set and disappeared.",
        "\"I do not love things that set,\" he said. He knew the true Lord is Allah, who made the sky and earth and never disappears.",
        "His people prayed to statues. Ibrahim broke the idols but left the biggest one, so people would ask why it could not even speak.",
        "Angry, they threw him into a huge fire. But Allah said: \"O fire, be cool and safe for Ibrahim\" — and he walked out unharmed.",
        "Later, Ibrahim and his son Isma'il built the Ka'bah together, praying: \"Our Lord, accept this from us.\"",
    ), "Think carefully, trust Allah, and He will look after you.", "Qur'an 6:74–79, 21:51–70, 2:127", 2),
    ProphetStory("musa", "Prophet Musa", "موسى عليه السلام", listOf(
        "When Musa, peace be upon him, was a baby, Pharaoh was harming the boys of Bani Isra'il. Allah inspired his mother to place him in a basket on the river.",
        "The river carried him to Pharaoh's own palace. Allah kept His promise and brought baby Musa back to his mother to be fed and cared for.",
        "When Musa grew up, Allah spoke to him and sent him with his brother Harun to tell Pharaoh to stop being cruel.",
        "Musa felt nervous, so he made a du'a: \"My Lord, open my heart for me and make my task easy for me.\"",
        "When Pharaoh's army chased the believers to the sea, Musa struck the water with his staff, and Allah split the sea into a dry path. They crossed safely.",
    ), "When you feel scared or nervous, ask Allah for help — He keeps His promises.", "Qur'an 28:7–13, 20:9–36, 26:61–66", 3),
    ProphetStory("yunus", "Prophet Yunus", "يونس عليه السلام", listOf(
        "Prophet Yunus, peace be upon him, called his people to Allah, but they would not listen. He left the town before Allah had told him to.",
        "He boarded a crowded ship. A storm came, and Yunus ended up in the sea, where a big fish swallowed him.",
        "Inside the fish it was dark — in the night, in the deep sea, in the fish. Yunus did not give up. He called out:",
        "\"There is no god but You. Glory be to You. I have been one of the wrongdoers.\"",
        "Allah heard him. The fish placed him on the shore, a leafy plant grew to shade him, and when he returned, his whole town had believed.",
    ), "No matter how dark things feel, call on Allah. He always hears.", "Qur'an 21:87–88, 37:139–148, 10:98", 4),
)

data class MannersCard(val id: String, val title: String, val body: String, val ref: String)

val MANNERS_CARDS = listOf(
    MannersCard("salam", "Say salam first", "Greet family and friends with \"Assalamu 'alaykum\". Spreading salam spreads love.", "Sahih Muslim 54"),
    MannersCard("smile", "Smile", "A smile for someone is a sadaqah — a gift that costs nothing.", "Jami' at-Tirmidhi 1956"),
    MannersCard("eat", "Bismillah, right hand", "Say Bismillah, eat with your right hand, and eat from what is in front of you.", "Sahih al-Bukhari 5376"),
    MannersCard("parents", "Be kind to parents", "Speak gently to your parents and help them, even with small things.", "Qur'an 17:23–24"),
    MannersCard("truth", "Tell the truth", "Honesty leads to goodness. Tell the truth even when it's hard.", "Sahih al-Bukhari 6094"),
    MannersCard("clean", "Stay clean", "Wash, brush your teeth and keep your things tidy — cleanliness is part of iman.", "Sahih Muslim 223"),
    MannersCard("animals", "Be gentle to animals", "Allah forgave a man because he gave water to a thirsty dog. Kindness to animals matters.", "Sahih al-Bukhari 2363"),
    MannersCard("thanks", "Say thank you", "Thank people who help you. Whoever doesn't thank people hasn't thanked Allah.", "Sunan Abi Dawud 4811"),
)
