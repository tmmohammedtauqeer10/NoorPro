package com.noorpro.app.data

object QuranMetaData {
    val surahNamesEn = listOf(
        "Al-Faatiha", "Al-Baqara", "Aal-i-Imraan", "An-Nisaa", "Al-Maaida", "Al-An'aam", "Al-A'raaf", "Al-Anfaal", "At-Tawba", "Yunus",
        "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Israa", "Al-Kahf", "Maryam", "Taa-Haa",
        "Al-Anbiyaa", "Al-Hajj", "Al-Muminoon", "An-Noor", "Al-Furqaan", "Ash-Shu'araa", "An-Naml", "Al-Qasas", "Al-Ankaboot", "Ar-Room",
        "Luqman", "As-Sajda", "Al-Ahzaab", "Saba", "Faatir", "Yaseen", "As-Saaffaat", "Saad", "Az-Zumar", "Ghafir",
        "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhaan", "Al-Jaathiya", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujuraat", "Qaaf",
        "Adh-Dhaariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahmaan", "Al-Waaqia", "Al-Hadid", "Al-Mujaadila", "Al-Hashr", "Al-Mumtahana",
        "As-Saff", "Al-Jumu'a", "Al-Munaafiqoon", "At-Taghaabun", "At-Talaaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haaqqa", "Al-Ma'aarij",
        "Nooh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyaama", "Al-Insaan", "Al-Mursalaat", "An-Naba", "An-Naazi'aat", "Abasa",
        "At-Takwir", "Al-Infitaar", "Al-Mutaffifin", "Al-Inshiqaaq", "Al-Burooj", "At-Taariq", "Al-A'laa", "Al-Ghaashiya", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Lail", "Ad-Dhuhaa", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyina", "Az-Zalzala", "Al-Aadiyaat",
        "Al-Qaari'a", "At-Takaathur", "Al-Asr", "Al-Humaza", "Al-Fil", "Quraish", "Al-Maa'un", "Al-Kawthar", "Al-Kaafiroon", "An-Nasr",
        "Al-Masad", "Al-Ikhlaas", "Al-Falaq", "An-Naas"
    )
    val surahNamesAr = listOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )
    val surahTypes = listOf(
        "Meccan", "Medinan", "Medinan", "Medinan", "Medinan", "Meccan", "Meccan", "Medinan", "Medinan", "Meccan",
        "Meccan", "Meccan", "Medinan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Medinan", "Meccan", "Medinan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Meccan", "Medinan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Medinan", "Medinan", "Medinan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Medinan", "Meccan", "Medinan", "Medinan", "Medinan", "Medinan",
        "Medinan", "Medinan", "Medinan", "Medinan", "Medinan", "Medinan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Medinan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Medinan", "Medinan", "Meccan",
        "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Meccan", "Medinan",
        "Meccan", "Meccan", "Meccan", "Meccan"
    )
    val surahTranslations = listOf(
        "The Opening", "The Cow", "The Family of Imraan", "The Women", "The Table", "The Cattle", "The Heights", "The Spoils of War", "The Repentance", "Jonas",
        "Hud", "Joseph", "The Thunder", "Abraham", "The Rock", "The Bee", "The Night Journey", "The Cave", "Mary", "Taa-Haa",
        "The Prophets", "The Pilgrimage", "The Believers", "The Light", "The Criterion", "The Poets", "The Ant", "The Stories", "The Spider", "The Romans",
        "Luqman", "The Prostration", "The Clans", "Sheba", "The Originator", "Yaseen", "Those drawn up in Ranks", "The letter Saad", "The Groups", "The Forgiver",
        "Explained in detail", "Consultation", "Ornaments of gold", "The Smoke", "Crouching", "The Dunes", "Muhammad", "The Victory", "The Inner Apartments", "The letter Qaaf",
        "The Winnowing Winds", "The Mount", "The Star", "The Moon", "The Beneficent", "The Inevitable", "The Iron", "The Pleading Woman", "The Exile", "She that is to be examined",
        "The Ranks", "Friday", "The Hypocrites", "Mutual Disillusion", "Divorce", "The Prohibition", "The Sovereignty", "The Pen", "The Reality", "The Ascending Stairways",
        "Noah", "The Jinn", "The Enshrouded One", "The Cloaked One", "The Resurrection", "Man", "The Emissaries", "The Announcement", "Those who drag forth", "He frowned",
        "The Overthrowing", "The Cleaving", "Defrauding", "The Splitting Open", "The Constellations", "The Morning Star", "The Most High", "The Overwhelming", "The Dawn", "The City",
        "The Sun", "The Night", "The Morning Hours", "The Consolation", "The Fig", "The Clot", "The Power, Fate", "The Evidence", "The Earthquake", "The Chargers",
        "The Calamity", "Competition", "The Declining Day, Epoch", "The Traducer", "The Elephant", "Quraysh", "Almsgiving", "Abundance", "The Disbelievers", "Divine Support",
        "The Palm Fibre", "Sincerity", "The Dawn", "Mankind"
    )
}
