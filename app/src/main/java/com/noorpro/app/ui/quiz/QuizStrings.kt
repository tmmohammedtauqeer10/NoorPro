package com.noorpro.app.ui.quiz

import com.noorpro.app.data.quiz.QuizCat

/** Quiz UI text in English, Urdu, Hindi and Arabic. Question text is bundled in English and translated online on demand. */
object QuizStrings {
    val languages = listOf("en" to "English", "ur" to "اردو", "hi" to "हिन्दी", "ar" to "العربية")
    fun norm(lang: String?): String = when (lang?.lowercase()?.take(2)) { "ur" -> "ur"; "hi" -> "hi"; "ar" -> "ar"; else -> "en" }
    fun rtl(lang: String) = lang == "ur" || lang == "ar"

    private val t: Map<String, List<String>> = mapOf(
        //            en, ur, hi, ar
        "title" to listOf("Islamic Quiz", "اسلامی کوئز", "इस्लामी क्विज़", "مسابقة إسلامية"),
        "subtitle" to listOf("Learn something true every day", "ہر دن کچھ سچا سیکھیں", "हर दिन कुछ सच्चा सीखें", "تعلّم شيئًا صحيحًا كل يوم"),
        "daily" to listOf("Today's quiz", "آج کا کوئز", "आज का क्विज़", "مسابقة اليوم"),
        "daily_body" to listOf("10 questions from every section. Same set for everyone today.", "ہر حصے سے 10 سوالات۔ آج سب کے لیے ایک جیسے۔", "हर हिस्से से 10 सवाल। आज सबके लिए एक जैसे।", "10 أسئلة من كل الأقسام، نفسها للجميع اليوم."),
        "start" to listOf("Start", "شروع کریں", "शुरू करें", "ابدأ"),
        "done_today" to listOf("Done today", "آج مکمل", "आज पूरा", "أُنجز اليوم"),
        "play_again" to listOf("Play again", "دوبارہ کھیلیں", "फिर से खेलें", "العب مجددًا"),
        "streak" to listOf("Day streak", "دنوں کا سلسلہ", "दिन लगातार", "أيام متتالية"),
        "xp" to listOf("XP", "XP", "XP", "نقاط"),
        "accuracy" to listOf("Accuracy", "درستگی", "सटीकता", "الدقة"),
        "categories" to listOf("Sections", "حصے", "विभाग", "الأقسام"),
        "questions" to listOf("questions", "سوالات", "सवाल", "سؤالًا"),
        "best" to listOf("Best", "بہترین", "सर्वश्रेष्ठ", "الأفضل"),
        "level" to listOf("Difficulty", "مشکل", "कठिनाई", "الصعوبة"),
        "mixed" to listOf("Mixed", "ملا جلا", "मिश्रित", "متنوّع"),
        "easy" to listOf("Easy", "آسان", "आसान", "سهل"),
        "medium" to listOf("Medium", "درمیانہ", "मध्यम", "متوسط"),
        "hard" to listOf("Hard", "مشکل", "कठिन", "صعب"),
        "timer" to listOf("Timer (30 s)", "ٹائمر (30 سیکنڈ)", "टाइमर (30 सेकंड)", "المؤقت (30 ث)"),
        "madhhab" to listOf("Fiqh follows", "فقہ کے مطابق", "फ़िक़्ह के अनुसार", "الفقه على"),
        "language" to listOf("Language", "زبان", "भाषा", "اللغة"),
        "review" to listOf("Review mistakes", "غلطیوں کا جائزہ", "गलतियाँ दोहराएँ", "مراجعة الأخطاء"),
        "review_body" to listOf("Questions you got wrong come back until you get them right.", "غلط جوابات والے سوال تب تک واپس آئیں گے جب تک درست نہ ہوں۔", "गलत सवाल तब तक लौटेंगे जब तक सही न हों।", "تعود الأسئلة التي أخطأت فيها حتى تجيبها صحيحًا."),
        "no_mistakes" to listOf("No mistakes to review", "جائزے کے لیے کوئی غلطی نہیں", "दोहराने को कोई गलती नहीं", "لا أخطاء للمراجعة"),
        "ai_title" to listOf("More questions with Noor AI", "نور AI سے مزید سوالات", "नूर AI से और सवाल", "أسئلة إضافية من نور AI"),
        "ai_body" to listOf("Creates 5 new questions for a section. Each answer is checked a second time; unclear ones are dropped.", "ایک حصے کے لیے 5 نئے سوال۔ ہر جواب دوبارہ جانچا جاتا ہے؛ غیر واضح ہٹا دیے جاتے ہیں۔", "एक विभाग के लिए 5 नए सवाल। हर उत्तर दोबारा जाँचा जाता है; अस्पष्ट हटा दिए जाते हैं।", "ينشئ 5 أسئلة جديدة لقسم، ويُتحقق من كل إجابة مرة ثانية ويُحذف غير الواضح."),
        "generate" to listOf("Generate", "بنائیں", "बनाएँ", "أنشئ"),
        "generating" to listOf("Writing and checking…", "لکھے اور جانچے جا رہے ہیں…", "लिखे और जाँचे जा रहे हैं…", "جارٍ الكتابة والتحقق…"),
        "next" to listOf("Next", "اگلا", "अगला", "التالي"),
        "finish" to listOf("See results", "نتیجہ دیکھیں", "परिणाम देखें", "النتيجة"),
        "correct" to listOf("Correct", "درست", "सही", "صحيح"),
        "wrong" to listOf("Not quite", "درست نہیں", "सही नहीं", "ليس تمامًا"),
        "time_up" to listOf("Time's up", "وقت ختم", "समय समाप्त", "انتهى الوقت"),
        "source" to listOf("Source", "ماخذ", "स्रोत", "المصدر"),
        "view_of" to listOf("view", "مسلک", "मत", "مذهب"),
        "question" to listOf("Question", "سوال", "सवाल", "سؤال"),
        "of" to listOf("of", "از", "में से", "من"),
        "result" to listOf("Your result", "آپ کا نتیجہ", "आपका परिणाम", "نتيجتك"),
        "earned" to listOf("XP earned", "حاصل XP", "मिले XP", "نقاط مكتسبة"),
        "to_review" to listOf("To review", "دہرانے کے لیے", "दोहराने के लिए", "للمراجعة"),
        "done" to listOf("Done", "مکمل", "पूरा", "تم"),
        "translating" to listOf("Translating…", "ترجمہ ہو رہا ہے…", "अनुवाद हो रहा है…", "جارٍ الترجمة…"),
        "english_only" to listOf("", "یہ سوال انگریزی میں ہے — ترجمے کے لیے انٹرنیٹ سے جڑیں۔", "यह सवाल अंग्रेज़ी में है — अनुवाद के लिए इंटरनेट से जुड़ें।", "هذا السؤال بالإنجليزية — اتصل بالإنترنت للترجمة."),
        "machine_tr" to listOf("", "ترجمہ نور AI کا ہے؛ ماخذ اصل حوالہ ہے۔", "अनुवाद नूर AI का है; स्रोत मूल संदर्भ है।", "الترجمة من نور AI؛ والمصدر هو المرجع الأصلي."),
        "offline_ok" to listOf("Works offline", "آف لائن بھی", "ऑफ़लाइन भी", "يعمل دون اتصال"),
        "kids_note" to listOf("Kids mode: simple questions only", "بچوں کا موڈ: صرف آسان سوالات", "बच्चों का मोड: केवल आसान सवाल", "وضع الأطفال: أسئلة بسيطة فقط"),
        "ai_offline" to listOf("Connect to the internet to generate questions.", "سوال بنانے کے لیے انٹرنیٹ سے جڑیں۔", "सवाल बनाने के लिए इंटरनेट से जुड़ें।", "اتصل بالإنترنت لإنشاء الأسئلة."),
        "ai_added" to listOf("new questions added", "نئے سوالات شامل", "नए सवाल जोड़े गए", "أسئلة جديدة أضيفت"),
        "ai_none" to listOf("No question passed the check. Try again later.", "کوئی سوال جانچ میں پاس نہیں ہوا۔ بعد میں کوشش کریں۔", "कोई सवाल जाँच में पास नहीं हुआ। बाद में कोशिश करें।", "لم يجتز أي سؤال التحقق. حاول لاحقًا."),
        "ai_badge" to listOf("Noor AI", "نور AI", "नूर AI", "نور AI"),
        "all_correct" to listOf("Ma sha Allah — every answer correct.", "ما شاء اللہ — سب جواب درست۔", "माशाअल्लाह — सब जवाब सही।", "ما شاء الله — كل الإجابات صحيحة."),
        "quit" to listOf("Quit", "چھوڑیں", "छोड़ें", "خروج"),
    )

    fun s(key: String, lang: String): String {
        val row = t[key] ?: return key
        return row[when (norm(lang)) { "ur" -> 1; "hi" -> 2; "ar" -> 3; else -> 0 }].ifBlank { row[0] }
    }

    fun cat(c: QuizCat, lang: String): String = when (norm(lang)) {
        "ur" -> when (c) { QuizCat.QURAN -> "قرآن"; QuizCat.SEERAH -> "سیرت"; QuizCat.HADITH -> "حدیث"; QuizCat.FIQH -> "فقہ"; QuizCat.AQEEDAH -> "عقیدہ"
            QuizCat.HISTORY -> "اسلامی تاریخ"; QuizCat.AWLIYA -> "اولیاء و تصوف"; QuizCat.DUAS -> "دعائیں"; QuizCat.KIDS -> "بچوں کے لیے" }
        "hi" -> when (c) { QuizCat.QURAN -> "क़ुरआन"; QuizCat.SEERAH -> "सीरत"; QuizCat.HADITH -> "हदीस"; QuizCat.FIQH -> "फ़िक़्ह"; QuizCat.AQEEDAH -> "अक़ीदा"
            QuizCat.HISTORY -> "इस्लामी इतिहास"; QuizCat.AWLIYA -> "औलिया और तसव्वुफ़"; QuizCat.DUAS -> "दुआएँ"; QuizCat.KIDS -> "बच्चों के लिए" }
        "ar" -> when (c) { QuizCat.QURAN -> "القرآن"; QuizCat.SEERAH -> "السيرة"; QuizCat.HADITH -> "الحديث"; QuizCat.FIQH -> "الفقه"; QuizCat.AQEEDAH -> "العقيدة"
            QuizCat.HISTORY -> "التاريخ الإسلامي"; QuizCat.AWLIYA -> "الأولياء والتصوف"; QuizCat.DUAS -> "الأدعية"; QuizCat.KIDS -> "للأطفال" }
        else -> when (c) { QuizCat.QURAN -> "Quran"; QuizCat.SEERAH -> "Seerah"; QuizCat.HADITH -> "Hadith"; QuizCat.FIQH -> "Fiqh"; QuizCat.AQEEDAH -> "Aqeedah"
            QuizCat.HISTORY -> "Islamic history"; QuizCat.AWLIYA -> "Awliya & Tasawwuf"; QuizCat.DUAS -> "Duas"; QuizCat.KIDS -> "Kids" }
    }

    fun catBlurb(c: QuizCat): String = when (c) {
        QuizCat.QURAN -> "Surahs, revelation, prophets"; QuizCat.SEERAH -> "Life of the Prophet ﷺ"; QuizCat.HADITH -> "Sayings and hadith sciences"
        QuizCat.FIQH -> "Follows your madhhab"; QuizCat.AQEEDAH -> "Ash'ari · Maturidi"; QuizCat.HISTORY -> "Caliphs to empires"
        QuizCat.AWLIYA -> "Masters of the heart"; QuizCat.DUAS -> "Duas for every moment"; QuizCat.KIDS -> "Simple and fun"
    }

    fun level(l: Int?, lang: String) = s(when (l) { 1 -> "easy"; 2 -> "medium"; 3 -> "hard"; else -> "mixed" }, lang)
}
