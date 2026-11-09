package com.noorpro.app.v119

import com.noorpro.app.data.ChatErrors
import com.noorpro.app.data.media.edit.LibrarySound
import com.noorpro.app.data.media.edit.ReelSound
import com.noorpro.app.data.media.edit.ReelSoundLibrary
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.data.media.edit.ReelSoundTag
import org.junit.Assert.*
import org.junit.Test

class ChatAndSoundTest {
    @Test fun permissionDeniedDetectedAndHidden() {
        assertTrue(ChatErrors.isPermissionDenied(RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")))
        assertFalse(ChatErrors.isPermissionDenied(RuntimeException("UNAVAILABLE")))
        assertFalse(ChatErrors.isPermissionDenied(null))
        val m = ChatErrors.friendly("PERMISSION_DENIED: Missing or insufficient permissions.", null)!!
        assertFalse(m.contains("PERMISSION_DENIED"))
        assertEquals("x", ChatErrors.friendly(null, "x"))
        assertEquals("Network down", ChatErrors.friendly("Network down", null))
    }

    @Test fun retryBackoffCapped() {
        assertEquals(2_000L, ChatErrors.retryDelayMs(0)); assertEquals(10_000L, ChatErrors.retryDelayMs(50))
    }

    @Test fun chatMetaOmitsMembersOnRetry() {
        val rest = mapOf<String, Any>("lastMessage" to "hi")
        val full = ChatErrors.chatMeta(true, listOf("a", "b"), mapOf("a" to "A"), rest)
        assertEquals(listOf("a", "b"), full["participantUids"]); assertEquals("hi", full["lastMessage"])
        val retry = ChatErrors.chatMeta(false, listOf("a", "b"), mapOf("a" to "A"), rest)
        assertFalse(retry.containsKey("participantUids")); assertFalse(retry.containsKey("participantNames"))
    }

    private fun lib(title: String, artist: String, cat: String, group: String) =
        LibrarySound(ReelSound("t-$title", title, artist, "https://x/$title.mp3", "Public Domain", ""), cat, group, 30_000)

    @Test fun libraryFilterByWordsAndCategory() {
        val items = listOf(
            lib("Morning dua", "Hisn al-Muslim", ReelSoundLibrary.DUAS, "Morning"),
            lib("Rabbana atina", "Quranic duas", ReelSoundLibrary.QURANIC, "Forgiveness"),
            lib("Al-Fatiha · part 1", "Wahiduddin Khan", ReelSoundLibrary.TAFSIR, "1. Al-Fatiha"),
        )
        assertEquals(3, ReelSoundLibrary.filter(items, "", ReelSoundLibrary.ALL).size)
        assertEquals(listOf("Morning dua"), ReelSoundLibrary.filter(items, "morning HISN", ReelSoundLibrary.ALL).map { it.sound.title })
        assertEquals(1, ReelSoundLibrary.filter(items, "", ReelSoundLibrary.TAFSIR).size)
        assertTrue(ReelSoundLibrary.filter(items, "music", ReelSoundLibrary.ALL).isEmpty())
        assertEquals("t-Morning dua", ReelSoundLibrary.findByCredit(items, items[0].sound.credit)?.trackId)
        assertNull(ReelSoundLibrary.findByCredit(items, ""))
    }

    private fun track(l: com.noorpro.app.audio.models.LicenseType, attr: String) = com.noorpro.app.audio.models.Track(
        "id", "T", "a", "A", "https://cdn.example.org/t.mp3", 1000, license = l, attributionText = attr, sourceUrl = "")

    @Test fun libraryIsNeverCreativeCommons() {
        assertNotNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.PUBLIC_DOMAIN, "Hisn · Public Domain Mark 1.0")))
        assertNotNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.PIXABAY, "Pixabay creators · Pixabay Content License")))
        assertNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.CC_BY, "Ubaid · CC BY 4.0")))
        assertNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.CC0, "x · CC0")))
        assertNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.PUBLIC_DOMAIN, "x · Creative Commons Attribution")))
        assertNull(com.noorpro.app.data.media.edit.ReelSounds.from(track(LicenseType.SOURCE_TERMS, "Islamic Network")))
        val items = listOf(lib("a", "b", "Nasheeds (voice only)", "g"), lib("c", "d", ReelSoundLibrary.DUAS, "g"))
        assertEquals(listOf(ReelSoundLibrary.ALL, "Nasheeds (voice only)", ReelSoundLibrary.DUAS), ReelSoundLibrary.categories(items))
    }

    @Test fun soundTagParsing() {
        val s = ReelSound("x", "Morning dua", "Hisn al-Muslim", "https://x", "Public Domain", "")
        val t = ReelSoundTag.parse("Al-Baqarah 2:255 · Sound: ${s.credit}", "Ali")
        assertEquals("Morning dua", t.title); assertEquals("Hisn al-Muslim", t.artist); assertFalse(t.original)
        assertEquals(s.credit, t.credit)
        val none = ReelSoundTag.parse("", "Ali")
        assertTrue(none.original); assertEquals("Original audio · Ali", none.display)
        val free = ReelSoundTag.parse("Sound: Recitation by Sheikh X", "Ali")
        assertTrue(free.original); assertEquals("Recitation by Sheikh X", free.title)
        val own = ReelSoundLibrary.original("file:///c/own.m4a", "my nasheed.m4a", "Ali")
        assertTrue(ReelSoundLibrary.isOriginal(own)); assertEquals("my nasheed", own.title)
        assertTrue(ReelSoundTag.parse("Sound: ${own.credit}", "Ali").original)
    }
}
