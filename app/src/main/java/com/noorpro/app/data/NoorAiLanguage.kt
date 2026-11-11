package com.noorpro.app.data

/**
 * 1.2.0 (113): automatic answer-language detection for Noor AI. Mirrors the noorpro-ai Worker's
 * `lang.js`, so the header label and offline messages match the language the Worker answers in.
 */
object NoorAiLanguage {
    const val AUTO = "auto"

    private val urduOnly = Regex("[\u0679\u0688\u0691\u06BA\u06BE\u06C1\u06C3\u06D2\u06D3]")
    private val urduLean = Regex("[\u067E\u0686\u0698\u06AF\u06A9\u06CC]")
    private val urduWords = Regex("(ہے|میں|کے|کی|کا|کیا|نہیں|مجھے|کر)")
    private val arabicOnlyLetters = Regex("[ةىأإآ]")
    private val romanUrdu = ("kya kia hai hain hy he mujhe muje mujhko mera meri mere tum aap ap apko kaise kese kaisay kyun kyon kyu " +
        "nahi nahin nai nhi karna karo karein karen kar raha rahi hota hoti hogi hoga mein main me bhi aur ya ke ka ki ko se par pe " +
        "batao bataen bataiye bataye chahiye chahte sakta sakti sakte liye wala wali kab kahan kitna kitni jab tab agar lekin magar " +
        "namaz namaaz roza rozay wazu wuzu dua duaa gunah sawab jannat jahannam bare baare baray bara janna jaanna jaanana").split(" ").toSet()
    private val hinglish = setOf("kripya", "bhagwan", "dhanyavad")

    /** Display names (native + English) for every code the Worker can answer in. */
    val names = linkedMapOf(
        "en" to "English", "ur" to "اردو Urdu", "ur-Latn" to "Roman Urdu", "hi" to "हिन्दी Hindi", "hi-Latn" to "Hinglish",
        "ar" to "العربية Arabic", "bn" to "বাংলা Bengali", "ta" to "தமிழ் Tamil", "ml" to "മലയാളം Malayalam", "te" to "తెలుగు Telugu",
    )

    /** Short English label for the chat header, e.g. "Urdu". */
    fun shortName(code: String): String = names[code]?.substringAfterLast(' ') ?: "English"

    fun detect(text: String): String {
        var arabic = 0; var deva = 0; var beng = 0; var tamil = 0; var mal = 0; var tel = 0; var latin = 0
        for (c in text) when (c.code) {
            in 0x0600..0x06FF, in 0x0750..0x077F, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> arabic++
            in 0x0900..0x097F -> deva++
            in 0x0980..0x09FF -> beng++
            in 0x0B80..0x0BFF -> tamil++
            in 0x0D00..0x0D7F -> mal++
            in 0x0C00..0x0C7F -> tel++
            in 'A'.code..'Z'.code, in 'a'.code..'z'.code -> latin++
        }
        val best = maxOf(arabic, deva, beng, tamil, mal, tel, latin)
        if (best == 0) return "en"
        return when (best) {
            arabic -> when {
                urduOnly.containsMatchIn(text) -> "ur"
                urduLean.containsMatchIn(text) && urduWords.containsMatchIn(text) -> "ur"
                urduLean.containsMatchIn(text) && !arabicOnlyLetters.containsMatchIn(text) -> "ur"
                else -> "ar"
            }
            deva -> "hi"
            beng -> "bn"
            tamil -> "ta"
            mal -> "ml"
            tel -> "te"
            else -> {
                val words = text.lowercase().split(Regex("[^a-z']+")).filter { it.isNotEmpty() }
                val hits = words.count { it in romanUrdu }
                if (words.isNotEmpty() && hits >= 2 && hits.toDouble() / words.size >= 0.2) {
                    if (words.any { it in hinglish }) "hi-Latn" else "ur-Latn"
                } else "en"
            }
        }
    }

    /** The language an answer will use: an explicit choice wins, otherwise the question's language. */
    fun resolve(setting: String, prompt: String): String =
        if (setting.isBlank() || setting == AUTO || setting !in names) detect(prompt) else setting

    /** Shown when Noor AI can't reach any server; never dumps unrelated quotes. */
    fun unavailable(code: String): String = when (code) {
        "ur" -> "نور اے آئی اس وقت سرور سے رابطہ نہیں کر پا رہا۔ براہِ کرم انٹرنیٹ چیک کریں اور تھوڑی دیر بعد دوبارہ بھیجیں۔"
        "ur-Latn" -> "Noor AI is waqt server se rabta nahi kar pa raha. Internet check karein aur thori der baad dobara bhejein."
        "hi" -> "नूर एआई अभी सर्वर से जुड़ नहीं पा रहा है। कृपया इंटरनेट जाँचें और थोड़ी देर बाद फिर से भेजें।"
        "hi-Latn" -> "Noor AI abhi server se jud nahi pa raha. Internet check karein aur thodi der baad phir bhejein."
        "ar" -> "لا يستطيع نور AI الاتصال بالخادم الآن. يُرجى التحقق من الإنترنت وإعادة الإرسال بعد قليل."
        "bn" -> "নূর এআই এই মুহূর্তে সার্ভারে সংযোগ করতে পারছে না। ইন্টারনেট দেখে একটু পরে আবার পাঠান।"
        else -> "Noor AI is unavailable right now because it can't reach its server. Please check your internet and tap send again in a little while."
    }

    /** Heading for the offline "related passages" block (only used for strong matches). */
    fun offlineHeading(code: String): String = when (code) {
        "ur", "ur-Latn" -> if (code == "ur") "نور اے آئی ابھی آف لائن ہے۔ آپ کے فون میں محفوظ قرآن و حدیث سے متعلقہ حوالے:" else "Noor AI abhi offline hai. Aap ke phone mein mehfooz Quran o Hadith se mutalliqa hawale:"
        "hi" -> "नूर एआई अभी ऑफ़लाइन है। आपके फ़ोन में सहेजे क़ुरआन और हदीस से संबंधित अंश:"
        "ar" -> "نور AI غير متصل الآن. نصوص ذات صلة من القرآن والحديث المحفوظة على هاتفك:"
        else -> "Noor AI is offline right now. Closely related passages saved on your phone:"
    }

    val allUnavailable: Set<String> by lazy { (names.keys + "en").map { unavailable(it) }.toSet() }
}
