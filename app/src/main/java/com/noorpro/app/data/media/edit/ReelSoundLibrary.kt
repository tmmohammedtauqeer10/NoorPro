package com.noorpro.app.data.media.edit

import android.content.Context
import com.noorpro.app.audio.catalog.FreeAudioCatalog

/**
 * 1.1.9 reel sound library (Instagram-style "Add sound"). Built only from Al-Noor Audio tracks whose
 * licence allows reuse in a new video (public domain / CC0 / CC BY, see [ReelSounds.eligible]) plus the
 * creator's own original sound. No music, no YouTube, no stream-only recitations.
 */
data class LibrarySound(
    val sound: ReelSound,
    val category: String,
    val group: String,
    val durationMs: Long,
    /** All Al-Noor library audio is voice only (no instruments); shown as a halal tag. */
    val tag: String = "Voice only",
)

object ReelSoundLibrary {
    const val ALL = "All"
    const val DUAS = "Duas & azkar"
    const val QURANIC = "Quranic duas"
    const val TAFSIR = "Tafsir & reminders"
    const val ORIGINAL = "Original"
    val categories = listOf(ALL, DUAS, QURANIC, TAFSIR)

    @Volatile private var cache: List<LibrarySound>? = null

    fun load(ctx: Context): List<LibrarySound> = cache ?: runCatching {
        val duas = FreeAudioCatalog.duaGroups(ctx).flatMap { g ->
            g.tracks.mapNotNull { t ->
                ReelSounds.from(t)?.let { LibrarySound(it, if (t.id.startsWith("dua-Q")) QURANIC else DUAS, g.title, t.durationMs) }
            }
        }
        val tafsir = FreeAudioCatalog.scholars(ctx).flatMap { s ->
            s.surahs.flatMap { g -> g.tracks.mapNotNull { t -> ReelSounds.from(t)?.let { LibrarySound(it, TAFSIR, "${s.name} · ${g.title}", t.durationMs) } } }
        }
        (duas + tafsir).distinctBy { it.sound.url }
    }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) cache = it }

    /** Search title / artist / group; every word must match somewhere. */
    fun filter(items: List<LibrarySound>, query: String, category: String): List<LibrarySound> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return items.filter { s ->
            (category == ALL || s.category == category) && words.all { w ->
                s.sound.title.lowercase().contains(w) || s.sound.artist.lowercase().contains(w) || s.group.lowercase().contains(w)
            }
        }
    }

    /** The creator's own audio (picked from the phone). Only shown on their reel; not offered to others. */
    fun original(uri: String, fileName: String, creator: String) = ReelSound(
        trackId = "own-${uri.hashCode().toUInt()}", title = fileName.substringBeforeLast('.').ifBlank { "Original sound" }.take(48),
        artist = creator.ifBlank { "You" }.take(30), url = uri, licenseLabel = "Original sound", sourceUrl = ""
    )

    fun isOriginal(s: ReelSound) = s.trackId.startsWith("own-")

    fun findByCredit(items: List<LibrarySound>, credit: String): ReelSound? =
        credit.takeIf { it.isNotBlank() }?.let { c -> items.firstOrNull { it.sound.credit == c }?.sound }
}

/** What a published reel says about its sound (parsed from sourceReference "… · Sound: <credit>"). */
data class ReelSoundTag(val credit: String, val title: String, val artist: String, val original: Boolean) {
    /** Groups reels on the sound page. */
    val key get() = credit.lowercase()

    companion object {
        fun parse(sourceReference: String, creatorName: String): ReelSoundTag {
            val credit = sourceReference.split(" · ").firstOrNull { it.startsWith("Sound: ") }?.removePrefix("Sound: ")?.trim().orEmpty()
            if (credit.isBlank()) {
                val who = creatorName.ifBlank { "creator" }
                return ReelSoundTag("", "Original audio", who, true)
            }
            val title = credit.substringBefore(" — ").trim()
            val artist = credit.substringAfter(" — ", "").substringBeforeLast(" (").trim()
            val original = credit.endsWith("(Original sound)") || artist.isBlank()
            return ReelSoundTag(credit, title, artist.ifBlank { creatorName.ifBlank { "creator" } }, original)
        }
    }

    val display get() = "$title · $artist"
}
