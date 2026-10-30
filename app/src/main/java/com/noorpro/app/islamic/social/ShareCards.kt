package com.noorpro.app.islamic.social

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.noorpro.app.R
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.ComposerLimits
import com.noorpro.app.data.OfflineQuranAssets
import org.json.JSONObject
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.random.Random

/*
 * 1.1.6 Ayah / Hadith share cards (share_cards_enabled). Text comes only from what already ships in the app:
 * the bundled Quran (quran_core + bundled translations) and the bundled Sahih al-Bukhari / Sahih Muslim English
 * collections. Nothing is fetched or generated; every card carries its reference so others can verify it.
 */

enum class CardKind { AYAH, HADITH }

/** The words on a card. [arabic] is empty for hadith cards (the bundled Arabic includes the full chain). */
data class CardContent(val kind: CardKind, val arabic: String, val text: String, val reference: String, val ref: String) {
    val topic get() = if (kind == CardKind.AYAH) "Quran" else "Hadith"
}

enum class CardTranslation(val key: String, val label: String, val rtl: Boolean = false) {
    DEFAULT("en.ahmedraza", "English (default)"), SAHIH("en.sahih", "Sahih International"), URDU("ur.kanzuliman", "اردو · Kanz-ul-Iman", rtl = true);
}

enum class CardDesign(val label: String) { NOOR("Noor"), PARCHMENT("Parchment"), NIGHT("Night sky"), DAWN("Fajr"), MINIMAL("Minimal") }

enum class CardFormat(val label: String, val w: Int, val h: Int) { SQUARE("Square · feed", 1080, 1080), STORY("Story · 9:16", 1080, 1920) }

data class CardPick(val ref: String, val label: String)

object ShareCardLogic {
    val AYAH_PICKS = listOf(
        CardPick("94:6", "Ease"), CardPick("2:152", "Remember Me"), CardPick("13:28", "Hearts at rest"), CardPick("65:3", "Tawakkul"),
        CardPick("39:53", "Mercy"), CardPick("2:186", "I am near"), CardPick("3:139", "Do not grieve"), CardPick("93:5", "Contentment"),
        CardPick("16:128", "Ihsan"), CardPick("49:13", "Taqwa"), CardPick("20:114", "Knowledge"), CardPick("2:286", "Within capacity"),
    )
    val HADITH_PICKS = listOf(
        CardPick("Bukhari 1", "Intentions"), CardPick("Bukhari 13", "Brotherhood"), CardPick("Bukhari 6464", "Consistency"),
        CardPick("Bukhari 6114", "True strength"), CardPick("Bukhari 6125", "Make it easy"), CardPick("Bukhari 6412", "Health & time"),
        CardPick("Bukhari 6021", "Good is charity"), CardPick("Bukhari 5027", "Quran teachers"), CardPick("Bukhari 10", "A true Muslim"),
        CardPick("Muslim 6592", "Charity & humility"),
    )
    /** Only the two Sahih collections, so a card never needs a grading caveat. */
    val COLLECTIONS = mapOf("bukhari" to "Sahih al-Bukhari", "muslim" to "Sahih Muslim")

    private val AYAH_COUNTS = intArrayOf(7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6)

    /** "2:255", "2 : 255", "2.255" → (2, 255); null when out of range. */
    fun parseAyah(s: String): Pair<Int, Int>? {
        val m = Regex("^\\s*(\\d{1,3})\\s*[:.]\\s*(\\d{1,3})\\s*$").find(s) ?: return null
        val su = m.groupValues[1].toInt(); val ay = m.groupValues[2].toInt()
        if (su !in 1..114 || ay < 1 || ay > AYAH_COUNTS[su - 1]) return null
        return su to ay
    }

    /** "Bukhari 13", "muslim 6592", "Sahih Muslim #6592" → ("muslim", 6592). */
    fun parseHadith(s: String): Pair<String, Int>? {
        val t = s.lowercase()
        val c = COLLECTIONS.keys.firstOrNull { t.contains(it) } ?: return null
        val n = Regex("(\\d{1,5})").find(t)?.value?.toIntOrNull() ?: return null
        return if (n in 1..8000) c to n else null
    }

    /** Caption that travels with the image: the reference always, plus the text for screen readers / search. */
    fun caption(c: CardContent, destination: ComposerDestination? = null): String {
        val full = "${c.text}\n\n— ${c.reference}"
        val max = if (destination == ComposerDestination.STORY) ComposerLimits.STORY_CHARS else ComposerLimits.FEED_CHARS
        return if (full.length <= max) full else c.reference
    }

    fun labelFor(c: CardContent) = if (c.kind == CardKind.AYAH) "THE NOBLE QURAN" else "FROM THE SUNNAH"

    private val diac = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640\\uFEFF\\u0621]")
    internal fun skel(s: String) = diac.replace(s, "").replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace(" ", "")

    private val quoteChars = charArrayOf('"', '“', '”', ' ')

    /** Strips quotes the source already wraps the text in, so the card adds exactly one pair. */
    fun unquote(t: String): String = t.trim().trim(*quoteChars).trim()

    /** Light typographic tidy for bundled hadith text: space after "Narrated X:" and balanced quotes. */
    fun tidyHadith(t: String): String {
        var s = Regex(":(?=[A-Za-z'\"])").replace(t.trim(), ": ")
        s = Regex("\\s+").replace(s, " ")
        if (s.count { it == '"' } % 2 == 1) s += "\""
        return s
    }

    /** Removes a leading basmala from ayah 1 (except Al-Fatiha, whose basmala is ayah 1). */
    fun stripBasmala(text: String, basmala: String, surah: Int, ayah: Int): String {
        val t = text.trimStart('\uFEFF')
        if (ayah != 1 || surah == 1) return t
        val w = t.split(' ')
        return if (w.size > 4 && skel(w.take(4).joinToString(" ")) == skel(basmala)) w.drop(4).joinToString(" ") else t
    }
}

/** Reads card text from the bundled assets. Call off the main thread. */
object ShareCardSource {
    fun ayah(ctx: Context, surah: Int, ayah: Int, tr: CardTranslation = CardTranslation.DEFAULT): CardContent? = runCatching {
        val core = OfflineQuranAssets.loadCore(ctx)
        val a = core.ayahs.firstOrNull { it.surah == surah && it.ayah == ayah } ?: return null
        val basmala = core.ayahs.first().arabic
        val arabic = ShareCardLogic.stripBasmala(a.arabic, basmala, surah, ayah)
        val text = OfflineQuranAssets.bundledTranslation(ctx, tr.key, surah, ayah)?.trim() ?: a.english
        val name = core.surahNames.getOrNull(surah - 1)?.first ?: "Surah $surah"
        CardContent(CardKind.AYAH, arabic, text, "Surah $name $surah:$ayah", "$surah:$ayah")
    }.getOrNull()

    fun hadith(ctx: Context, collection: String, number: Int): CardContent? = runCatching {
        val name = ShareCardLogic.COLLECTIONS[collection] ?: return null
        val dir = "hadith/eng-$collection"
        for (f in ctx.assets.list(dir).orEmpty()) {
            val json = GZIPInputStream(ctx.assets.open("$dir/$f")).bufferedReader().use { it.readText() }
            val arr = JSONObject(json).optJSONArray("hadiths") ?: continue
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optInt("hadithnumber") == number) {
                    val t = o.optString("text").trim()
                    return if (t.isBlank()) null else CardContent(CardKind.HADITH, "", ShareCardLogic.tidyHadith(t), "$name $number", "${collection.replaceFirstChar { it.uppercase() }} $number")
                }
            }
        }
        null
    }.getOrNull()

    fun load(ctx: Context, ref: String, tr: CardTranslation = CardTranslation.DEFAULT): CardContent? =
        ShareCardLogic.parseAyah(ref)?.let { (s, a) -> ayah(ctx, s, a, tr) }
            ?: ShareCardLogic.parseHadith(ref)?.let { (c, n) -> hadith(ctx, c, n) }
}

/** Original NoorPro card artwork, drawn with Canvas so the preview is exactly the exported image. */
object ShareCardRenderer {
    data class Result(val bitmap: Bitmap, val fits: Boolean)

    private class Palette(val bgTop: Int, val bgBottom: Int, val arabic: Int, val text: Int, val accent: Int, val label: Int, val frame: Int)

    private fun palette(d: CardDesign) = when (d) {
        CardDesign.NOOR -> Palette(0xFF0B6243.toInt(), 0xFF07402C.toInt(), 0xFFFDF9F0.toInt(), 0xE6FDF9F0.toInt(), 0xFFD4AF37.toInt(), 0xFFD4AF37.toInt(), 0xFFD4AF37.toInt())
        CardDesign.PARCHMENT -> Palette(0xFFFDF9F0.toInt(), 0xFFF3E8D2.toInt(), 0xFF07402C.toInt(), 0xFF3B2F1E.toInt(), 0xFFA9842A.toInt(), 0xFF0B6243.toInt(), 0xFF0B6243.toInt())
        CardDesign.NIGHT -> Palette(0xFF0E1A2B.toInt(), 0xFF1D3150.toInt(), 0xFFFFFFFF.toInt(), 0xE0E8EEF7.toInt(), 0xFFE9C46A.toInt(), 0xFFE9C46A.toInt(), 0x55E9C46A)
        CardDesign.DAWN -> Palette(0xFFF8E7DA.toInt(), 0xFFE8B9A0.toInt(), 0xFF5A2A27.toInt(), 0xFF5A2A27.toInt(), 0xFF9C4A35.toInt(), 0xFF9C4A35.toInt(), 0x669C4A35)
        CardDesign.MINIMAL -> Palette(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF111111.toInt(), 0xFF3C3C3C.toInt(), 0xFF0B6243.toInt(), 0xFF0B6243.toInt(), 0xFFD4AF37.toInt())
    }

    private fun font(ctx: Context, id: Int, fallback: Typeface) = runCatching { ResourcesCompat.getFont(ctx, id) }.getOrNull() ?: fallback

    fun render(ctx: Context, c: CardContent, design: CardDesign, format: CardFormat, scale: Float = 1f,
               tr: CardTranslation = CardTranslation.DEFAULT): Result {
        val w = (format.w * scale).toInt(); val h = (format.h * scale).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(scale, scale)
        val W = format.w.toFloat(); val H = format.h.toFloat()
        val p = palette(design)
        background(cv, design, p, W, H)

        val naskh = font(ctx, R.font.noto_naskh_arabic, Typeface.SERIF)
        val caslon = font(ctx, R.font.librecaslontext_regular, Typeface.SERIF)
        val caslonItalic = font(ctx, R.font.librecaslontext_italic, Typeface.create(Typeface.SERIF, Typeface.ITALIC))
        val sans = font(ctx, R.font.plusjakartasans_bold, Typeface.DEFAULT_BOLD)
        val sansReg = font(ctx, R.font.plusjakartasans_medium, Typeface.DEFAULT)

        val side = if (design == CardDesign.MINIMAL) 150f else 130f
        val textW = (W - side * 2).toInt()
        val story = format == CardFormat.STORY
        val top = if (story) 300f else 170f
        val bottom = H - if (story) 320f else 190f

        // Label
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.label; textSize = 26f; typeface = sans; letterSpacing = 0.22f; textAlign = Paint.Align.CENTER }
        cv.drawText(ShareCardLogic.labelFor(c), W / 2, top - 40f, label)

        val urduMain = tr.rtl && c.kind == CardKind.AYAH
        var fits = false
        var layouts: List<Pair<StaticLayout, Float>> = emptyList()
        var f = 1f
        while (f >= 0.5f) {
            val out = mutableListOf<Pair<StaticLayout, Float>>()
            if (c.arabic.isNotBlank()) {
                val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.arabic; textSize = (if (story) 74f else 64f) * f; typeface = naskh }
                out += layout(c.arabic, ap, textW, rtl = true, spacing = 1.45f) to 34f * f
                if (design != CardDesign.MINIMAL) out += divider() to 30f * f
            }
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = p.text
                textSize = (if (c.kind == CardKind.HADITH) (if (story) 50f else 44f) else (if (story) 42f else 36f)) * f * (if (urduMain) 1.1f else 1f)
                typeface = if (urduMain) naskh else if (c.kind == CardKind.HADITH) caslon else caslonItalic
            }
            out += layout(if (c.kind == CardKind.HADITH) c.text else "“${ShareCardLogic.unquote(c.text)}”", tp, textW, rtl = urduMain, spacing = if (urduMain) 1.5f else 1.3f) to 0f
            val total = out.sumOf { (it.first.height + it.second).toDouble() }.toFloat()
            if (total <= bottom - top - 70f) { layouts = out; fits = true; break }
            layouts = out
            f -= 0.05f
        }

        val total = layouts.sumOf { (it.first.height + it.second).toDouble() }.toFloat()
        var y = top + ((bottom - top - 70f) - total).coerceAtLeast(0f) / 2
        for ((l, gap) in layouts) {
            if (l.text.toString() == DIVIDER) drawDivider(cv, W / 2, y + l.height / 2f, p.accent)
            else { cv.save(); cv.translate(side, y); l.draw(cv); cv.restore() }
            y += l.height + gap
        }
        // Reference
        val refP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent; textSize = 30f; typeface = sans; textAlign = Paint.Align.CENTER; letterSpacing = 0.04f }
        cv.drawText(c.reference, W / 2, (y + 46f).coerceAtMost(bottom + 20f), refP)

        // Footer wordmark
        val foot = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = p.label; alpha = 200; textSize = 24f; typeface = sansReg; textAlign = Paint.Align.CENTER; letterSpacing = 0.12f }
        val fy = H - if (story) 150f else 80f
        cv.drawCircle(W / 2 - 78f, fy - 8f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent })
        cv.drawText("NOORPRO", W / 2 + 8f, fy, foot)
        return Result(bmp, fits)
    }

    private const val DIVIDER = "\u2063"
    private fun divider(): StaticLayout = layout(DIVIDER, TextPaint().apply { textSize = 20f }, 10, false, 1f)

    private fun layout(text: String, paint: TextPaint, width: Int, rtl: Boolean, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, spacing)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(true)
            .build()

    private fun drawDivider(cv: Canvas, cx: Float, cy: Float, color: Int) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = 2.5f; alpha = 170 }
        cv.drawLine(cx - 150f, cy, cx - 34f, cy, line); cv.drawLine(cx + 34f, cy, cx + 150f, cy, line)
        star(cv, cx, cy, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    /** Eight-pointed star (two overlapping squares), the khatam motif used across NoorPro. */
    private fun star(cv: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        for (k in 0 until 16) {
            val ang = Math.PI / 8 * k - Math.PI / 2
            val rr = if (k % 2 == 0) r else r * 0.62f
            val x = cx + (rr * Math.cos(ang)).toFloat(); val y = cy + (rr * Math.sin(ang)).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); cv.drawPath(path, paint)
    }

    private fun background(cv: Canvas, d: CardDesign, p: Palette, W: Float, H: Float) {
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, H, p.bgTop, p.bgBottom, Shader.TileMode.CLAMP) }
        cv.drawRect(0f, 0f, W, H, bg)
        when (d) {
            CardDesign.NOOR -> {
                val lat = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x10FFFFFF; strokeWidth = 2f; style = Paint.Style.STROKE }
                var x = -H
                while (x < W + H) { cv.drawLine(x, 0f, x + H, H, lat); cv.drawLine(x + H, 0f, x, H, lat); x += 90f }
                val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.frame; style = Paint.Style.STROKE; strokeWidth = 3f }
                cv.drawRoundRect(RectF(48f, 48f, W - 48f, H - 48f), 36f, 36f, frame)
                frame.alpha = 90; cv.drawRoundRect(RectF(64f, 64f, W - 64f, H - 64f), 28f, 28f, frame)
                val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.frame }
                listOf(48f to 48f, W - 48f to 48f, 48f to H - 48f, W - 48f to H - 48f).forEach { (x0, y0) -> star(cv, x0, y0, 20f, gold) }
            }
            CardDesign.PARCHMENT -> {
                val vign = Paint().apply { shader = RadialGradient(W / 2, H / 2, maxOf(W, H) * 0.75f, intArrayOf(0x00000000, 0x22A9842A), null, Shader.TileMode.CLAMP) }
                cv.drawRect(0f, 0f, W, H, vign)
                val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.frame; style = Paint.Style.STROKE; strokeWidth = 4f }
                cv.drawRect(56f, 56f, W - 56f, H - 56f, frame)
                frame.strokeWidth = 1.5f; cv.drawRect(72f, 72f, W - 72f, H - 72f, frame)
                val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent }
                star(cv, W / 2, 56f, 22f, gold); star(cv, W / 2, H - 56f, 22f, gold)
            }
            CardDesign.NIGHT -> {
                val rnd = Random(116)
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                repeat(90) { dot.alpha = 60 + rnd.nextInt(150); cv.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H, 1.2f + rnd.nextFloat() * 2.4f, dot) }
                val moon = Path().apply { addCircle(W - 190f, 190f, 70f, Path.Direction.CW) }
                val cut = Path().apply { addCircle(W - 160f, 168f, 62f, Path.Direction.CW) }
                moon.op(cut, Path.Op.DIFFERENCE)
                cv.drawPath(moon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent })
                val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.frame; style = Paint.Style.STROKE; strokeWidth = 2f }
                cv.drawRoundRect(RectF(52f, 52f, W - 52f, H - 52f), 44f, 44f, frame)
            }
            CardDesign.DAWN -> {
                val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W / 2, H, W * 0.55f, intArrayOf(0x88FFF1C9.toInt(), 0x00FFF1C9), null, Shader.TileMode.CLAMP) }
                cv.drawCircle(W / 2, H, W * 0.55f, sun)
                val arch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.frame; style = Paint.Style.STROKE; strokeWidth = 3f }
                // Mihrab arch outline
                val path = Path().apply {
                    moveTo(80f, H - 70f); lineTo(80f, 330f)
                    quadTo(80f, 110f, W / 2, 70f); quadTo(W - 80f, 110f, W - 80f, 330f); lineTo(W - 80f, H - 70f)
                }
                cv.drawPath(path, arch)
            }
            CardDesign.MINIMAL -> {
                cv.drawRect(0f, 0f, 14f, H, Paint().apply { color = p.accent })
                cv.drawRect(W - 220f, 90f, W - 90f, 94f, Paint().apply { color = p.frame })
                cv.drawRect(90f, H - 94f, 220f, H - 90f, Paint().apply { color = p.frame })
            }
        }
    }
}

/** Saving and sending a finished card. */
object ShareCardExport {
    fun dir(ctx: Context) = File(ctx.cacheDir, "share_cards").apply { mkdirs() }

    fun savePng(ctx: Context, bmp: Bitmap): File {
        val f = File(dir(ctx), "noorpro_card_${System.currentTimeMillis()}.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        dir(ctx).listFiles()?.sortedByDescending { it.lastModified() }?.drop(6)?.forEach { it.delete() }
        return f
    }

    fun externalIntent(ctx: Context, file: File, c: CardContent): Intent {
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "— ${c.reference}\nShared from NoorPro").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Share card")
    }

    /** A ready composer draft with the card attached; the user still reviews and posts it. */
    fun draft(file: File, c: CardContent, destination: ComposerDestination, now: Long = System.currentTimeMillis()) = ComposerDraft(
        id = "card_$now", destination = destination, text = ShareCardLogic.caption(c, destination), topic = c.topic,
        media = listOf(Uri.fromFile(file).toString()),
    )
}

/** Lets another screen (challenges, readers) open the card maker on a given reference, consumed once. */
object ShareCardLaunch {
    @Volatile private var pending: String? = null
    fun open(ref: String) { pending = ref }
    fun consume(): String? = pending.also { pending = null }
}
