package com.noorpro.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/*
 * 1.1.1 composer support: destinations, limits, #/@ autocomplete helpers, poll + extra-field
 * building, Remote Config feature flags for the fields that need the 1.1.1 backend, local drafts,
 * and reel trim / cover helpers.
 */

enum class ComposerDestination(val label: String) { FEED("Ummah feed"), REEL("Reel"), STORY("Story") }

object ComposerLimits {
    /** Matches the ummah_submissions rule (caption.size() <= 2000). */
    const val FEED_CHARS = 2000
    /** Stories store at most 300 characters (StoryRepository). */
    const val STORY_CHARS = 300
    const val MAX_PHOTOS = 10
    const val POLL_MIN = 2
    const val POLL_MAX = 4
    const val POLL_OPTION_CHARS = 80
    const val REEL_MAX_MS = 180_000L
    fun chars(destination: ComposerDestination) = if (destination == ComposerDestination.STORY) STORY_CHARS else FEED_CHARS
}

/** Same topics as the feed chips. Stored as the post's category. */
val composerTopics = listOf("Reflections", "Quran", "Hadith", "Duas", "Reminders", "Community")

/** Features that only work once the 1.1.1 rules + publisher are deployed (Remote Config, default off). */
object ComposerFlags {
    const val KEY_POLLS = "composer_polls_enabled"
    const val KEY_VISIBILITY = "composer_visibility_enabled"
    const val KEY_LOCATION = "composer_location_enabled"
    const val KEY_AI_LABEL = "composer_ai_label_enabled"
    /** Tests / previews force values here. */
    @Volatile var override: Map<String, Boolean>? = null
    @Volatile private var loaded: Map<String, Boolean> = emptyMap()
    @Volatile private var started = false

    fun isOn(key: String): Boolean = override?.get(key) ?: loaded[key] ?: false
    val polls get() = isOn(KEY_POLLS)
    val visibility get() = isOn(KEY_VISIBILITY)
    val location get() = isOn(KEY_LOCATION)
    val aiLabel get() = isOn(KEY_AI_LABEL)

    fun load() {
        if (started) return
        started = true
        runCatching {
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val keys = listOf(KEY_POLLS, KEY_VISIBILITY, KEY_LOCATION, KEY_AI_LABEL)
            // No setDefaultsAsync here: it would replace other features' defaults. Missing keys read as false.
            fun apply() { loaded = keys.associateWith { rc.getBoolean(it) } }
            apply()
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

/** The #tag / @mention being typed at the cursor, if any. */
data class ComposerToken(val symbol: Char, val query: String, val start: Int, val end: Int)

object ComposerText {
    private val tokenChar = Regex("""[\p{L}\p{N}_.]""")

    fun activeToken(text: String, cursor: Int): ComposerToken? {
        val c = cursor.coerceIn(0, text.length)
        var i = c
        while (i > 0 && tokenChar.matches(text[i - 1].toString())) i--
        if (i == 0) return null
        val symbol = text[i - 1]
        if (symbol != '#' && symbol != '@') return null
        if (i - 1 > 0 && !text[i - 2].isWhitespace()) return null
        return ComposerToken(symbol, text.substring(i, c), i - 1, c)
    }

    /** Replace the active token with [completion] (which includes its # or @) plus a space. */
    fun complete(text: String, token: ComposerToken, completion: String): Pair<String, Int> {
        val tail = text.substring(token.end).let { t -> t.dropWhile { tokenChar.matches(it.toString()) } }
        val insert = completion + if (tail.startsWith(" ")) "" else " "
        val out = text.substring(0, token.start) + insert + tail
        return out to (token.start + insert.length)
    }

    /** Insert a bare symbol at the cursor, adding a leading space when needed. */
    fun insertSymbol(text: String, cursor: Int, symbol: Char): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val pre = if (c > 0 && !text[c - 1].isWhitespace()) " " else ""
        val out = text.substring(0, c) + pre + symbol + text.substring(c)
        return out to (c + pre.length + 1)
    }

    private val tagRegex = Regex("""(?<![\w#])#([\p{L}\p{N}_]{2,40})""")

    val starterTags = listOf("#Quran", "#Sabr", "#Shukr", "#Dua", "#Jumuah", "#Hadith", "#Reminder", "#Salah", "#Dhikr", "#Ramadan")

    /** Hashtag suggestions: tags already used in the feed (most used first), then starters. */
    fun hashtagSuggestions(prefix: String, feedCaptions: List<String>, limit: Int = 8): List<String> {
        val all = feedCaptions.flatMap { c -> tagRegex.findAll(c).map { "#" + it.groupValues[1] }.toList() }
        val groups = all.groupBy { it.lowercase() } // first spelling wins for display
        val ranked = groups.values.sortedByDescending { it.size }.map { it.first() } + starterTags
        val p = prefix.lowercase()
        return ranked.distinctBy { it.lowercase() }.filter { it.lowercase().removePrefix("#").startsWith(p) }.take(limit)
    }
}

data class ComposerPoll(val options: List<String> = listOf("", ""), val durationHours: Int = 24) {
    val cleanOptions get() = options.map { it.trim() }.filter { it.isNotEmpty() }
    val isValid get() = cleanOptions.size >= ComposerLimits.POLL_MIN && cleanOptions.distinctBy { it.lowercase() }.size == cleanOptions.size
}

/** Optional submission fields from the composer. Only fields whose flag is on are sent. */
fun composerExtraFields(
    poll: ComposerPoll?, aiLabel: Boolean, location: String, followersOnly: Boolean,
    flags: ComposerFlags = ComposerFlags, now: Long = System.currentTimeMillis()
): Map<String, Any> = buildMap {
    if (flags.polls && poll != null && poll.isValid) {
        put("poll", mapOf(
            "options" to poll.cleanOptions.take(ComposerLimits.POLL_MAX).map { it.take(ComposerLimits.POLL_OPTION_CHARS) },
            "endsAt" to com.google.firebase.Timestamp(java.util.Date(now + poll.durationHours.coerceIn(1, 168) * 3_600_000L))
        ))
    }
    if (flags.aiLabel && aiLabel) put("aiLabel", true)
    if (flags.location && location.isNotBlank()) put("locationName", location.trim().take(80))
    if (flags.visibility && followersOnly) put("visibility", "followers")
}

/** A locally saved draft. Media is copied into app storage so it survives restarts. */
data class ComposerDraft(
    val id: String,
    val destination: ComposerDestination,
    val text: String,
    val topic: String,
    val arabicText: String = "",
    val reference: String = "",
    val media: List<String> = emptyList(),
    val isVideo: Boolean = false,
    val pollOptions: List<String> = emptyList(),
    val pollHours: Int = 24,
    val aiLabel: Boolean = false,
    val location: String = "",
    val followersOnly: Boolean = false,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val coverMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    /** 1.1.3 editor pro: editing steps (EditCodec JSON) so the editors reopen where you left off. */
    val editJson: String = ""
) {
    val preview get() = text.ifBlank { arabicText }.ifBlank { if (media.isNotEmpty()) (if (isVideo) "Video" else "${media.size} photo(s)") else "Empty draft" }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("destination", destination.name); put("text", text); put("topic", topic)
        put("arabicText", arabicText); put("reference", reference); put("media", JSONArray(media)); put("isVideo", isVideo)
        put("pollOptions", JSONArray(pollOptions)); put("pollHours", pollHours); put("aiLabel", aiLabel); put("location", location)
        put("followersOnly", followersOnly); put("trimStartMs", trimStartMs); put("trimEndMs", trimEndMs); put("coverMs", coverMs)
        put("updatedAt", updatedAt); if (editJson.isNotEmpty()) put("editJson", editJson)
    }

    companion object {
        fun fromJson(o: JSONObject): ComposerDraft? = runCatching {
            fun list(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
            ComposerDraft(
                id = o.getString("id"),
                destination = runCatching { ComposerDestination.valueOf(o.optString("destination")) }.getOrDefault(ComposerDestination.FEED),
                text = o.optString("text"), topic = o.optString("topic", "Reflections"),
                arabicText = o.optString("arabicText"), reference = o.optString("reference"), media = list("media"),
                isVideo = o.optBoolean("isVideo"), pollOptions = list("pollOptions"), pollHours = o.optInt("pollHours", 24),
                aiLabel = o.optBoolean("aiLabel"), location = o.optString("location"), followersOnly = o.optBoolean("followersOnly"),
                trimStartMs = o.optLong("trimStartMs"), trimEndMs = o.optLong("trimEndMs"), coverMs = o.optLong("coverMs"),
                updatedAt = o.optLong("updatedAt"), editJson = o.optString("editJson")
            )
        }.getOrNull()
    }
}

/** Drafts live in private SharedPreferences (JSON) + files under filesDir/composer_drafts. */
class ComposerDraftStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("composer_drafts", Context.MODE_PRIVATE)
    private val root = File(app.filesDir, "composer_drafts")

    fun all(): List<ComposerDraft> = runCatching {
        val arr = JSONArray(prefs.getString("drafts", "[]"))
        (0 until arr.length()).mapNotNull { ComposerDraft.fromJson(arr.getJSONObject(it)) }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    /** Saves (or replaces) a draft, copying content:// media into private storage. Call off the main thread. */
    fun save(draft: ComposerDraft): ComposerDraft {
        val dir = File(root, draft.id).apply { mkdirs() }
        fun inDir(s: String) = Uri.parse(s).let { it.scheme == "file" && it.path?.startsWith(dir.absolutePath) == true } || s.startsWith(dir.absolutePath)
        fun copy(s: String, tag: String, ext: String): String? {
            if (inDir(s)) return s
            return runCatching {
                val out = File(dir, "${tag}_${System.nanoTime()}$ext")
                val input = if (s.startsWith("/")) File(s).inputStream() else app.contentResolver.openInputStream(Uri.parse(s))!!
                input.use { i -> out.outputStream().use { i.copyTo(it) } }
                if (s.startsWith("/")) out.absolutePath else Uri.fromFile(out).toString()
            }.getOrNull()
        }
        val moved = mutableMapOf<String, String>()
        val kept = draft.media.mapIndexedNotNull { i, s -> copy(s, "m_$i", if (draft.isVideo) ".mp4" else ".jpg")?.also { moved[s] = it } }
        // Editing steps: originals, extra reel clips and voiceover takes travel with the draft.
        val edits = com.noorpro.app.data.media.edit.EditCodec.draft(draft.editJson)
        val editJson = if (edits.isEmpty) "" else com.noorpro.app.data.media.edit.EditCodec.draft(edits.mapFiles { f ->
            moved.getOrPut(f) { copy(f, "e", when { f.endsWith(".wav") -> ".wav"; draft.isVideo -> ".mp4"; else -> ".jpg" }) ?: f }
        })
        val saved = draft.copy(media = kept, updatedAt = System.currentTimeMillis(), editJson = editJson)
        val keep = (kept + moved.values).map { it.substringAfterLast('/') }.toSet()
        dir.listFiles()?.filter { f -> f.name !in keep }?.forEach { it.delete() }
        write(all().filter { it.id != draft.id } + saved)
        return saved
    }

    fun delete(id: String) {
        write(all().filter { it.id != id })
        File(root, id).deleteRecursively()
    }

    private fun write(list: List<ComposerDraft>) {
        prefs.edit().putString("drafts", JSONArray(list.sortedByDescending { it.updatedAt }.take(20).map { it.toJson() }).toString()).apply()
    }
}

/** Reel editing: length probe, frame grab for the cover, and media3 Transformer trim. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ReelEditor {
    fun durationMs(context: Context, uri: Uri): Long = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(context, uri)
        val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L; r.release(); d
    }.getOrDefault(0L)

    fun frameAt(context: Context, uri: Uri, ms: Long, maxSide: Int = 720): android.graphics.Bitmap? = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(context, uri)
        val bmp = if (android.os.Build.VERSION.SDK_INT >= 27)
            r.getScaledFrameAtTime(ms * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxSide, maxSide)
        else r.getFrameAtTime(ms * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        r.release(); bmp
    }.getOrNull()

    fun saveCover(context: Context, bitmap: android.graphics.Bitmap): File? = runCatching {
        val f = File(context.cacheDir, "reel_cover_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }; f
    }.getOrNull()

    /** True when the chosen range actually cuts something off. */
    fun needsTrim(startMs: Long, endMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && (startMs > 250 || (endMs in 1 until durationMs - 250))

    /** Cut [startMs, endMs] into a new MP4 (same resolution, H.264/AAC). Callbacks on the main thread. */
    fun trim(context: Context, uri: Uri, startMs: Long, endMs: Long, onProgress: (Float) -> Unit, onDone: (File?) -> Unit) {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val out = File(context.cacheDir, "reel_trim_${System.currentTimeMillis()}.mp4")
        main.post {
            runCatching {
                val item = androidx.media3.common.MediaItem.Builder().setUri(uri)
                    .setClippingConfiguration(androidx.media3.common.MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs.coerceAtLeast(0)).setEndPositionMs(endMs).build())
                    .build()
                var finished = false
                val transformer = androidx.media3.transformer.Transformer.Builder(context.applicationContext)
                    .setVideoMimeType(androidx.media3.common.MimeTypes.VIDEO_H264)
                    .setAudioMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC)
                    .addListener(object : androidx.media3.transformer.Transformer.Listener {
                        override fun onCompleted(c: androidx.media3.transformer.Composition, r: androidx.media3.transformer.ExportResult) { finished = true; onDone(out) }
                        override fun onError(c: androidx.media3.transformer.Composition, r: androidx.media3.transformer.ExportResult, e: androidx.media3.transformer.ExportException) { finished = true; out.delete(); onDone(null) }
                    }).build()
                transformer.start(androidx.media3.transformer.EditedMediaItem.Builder(item).build(), out.absolutePath)
                val holder = androidx.media3.transformer.ProgressHolder()
                main.post(object : Runnable {
                    override fun run() {
                        if (finished) return
                        if (transformer.getProgress(holder) == androidx.media3.transformer.Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                        main.postDelayed(this, 300)
                    }
                })
            }.onFailure { onDone(null) }
        }
    }
}

/** Captions the feed already loaded (in memory only) — used for hashtag suggestions, no extra reads. */
object ComposerTagSource {
    @Volatile var captions: List<String> = emptyList()
}

/**
 * Why the Post button is disabled, or null when the post can go out.
 * Rules require a caption or Arabic text on feed posts and reels.
 */
fun composerBlocker(
    destination: ComposerDestination, text: String, arabic: String,
    photos: Int, hasVideo: Boolean, pollOpen: Boolean, poll: ComposerPoll,
    trimStartMs: Long = 0L, trimEndMs: Long = 0L, pollsEnabled: Boolean = ComposerFlags.polls
): String? {
    val limit = ComposerLimits.chars(destination)
    if (text.length > limit) return "Keep it under $limit characters"
    return when (destination) {
        ComposerDestination.FEED -> when {
            text.isBlank() && arabic.isBlank() -> "Write something to share"
            pollOpen && pollsEnabled && !poll.isValid -> "Add at least two different poll options"
            else -> null
        }
        ComposerDestination.REEL -> when {
            !hasVideo -> "Choose or record a video"
            trimEndMs > 0 && trimEndMs - trimStartMs < 1000 -> "Keep at least 1 second"
            text.isBlank() && arabic.isBlank() -> "Add a caption"
            else -> null
        }
        ComposerDestination.STORY -> if (photos == 0 && !hasVideo) "Add a photo or video for your story" else null
    }
}

fun formatClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * 1.1.2: one-shot destination for the next composer launch (e.g. "Your story" + in the Ummah
 * stories row opens the composer with Story preselected). Consumed once by the create screen.
 */
object ComposerLaunch {
    @Volatile private var pending: ComposerDestination? = null
    fun open(destination: ComposerDestination) { pending = destination }
    fun consume(): ComposerDestination? = pending.also { pending = null }
}
