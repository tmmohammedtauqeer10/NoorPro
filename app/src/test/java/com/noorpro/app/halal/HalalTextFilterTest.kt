package com.noorpro.app.halal

import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.data.halal.HalalTextFilter.Action
import com.squareup.moshi.JsonReader
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Runs the shared server vectors (functions/halal-vectors.json) through the Kotlin port. */
class HalalTextFilterTest {
    companion object {
        private fun file(vararg candidates: String) = candidates.map(::File).first { it.exists() }
        val assets = file("src/main/assets/halal/words.json", "app/src/main/assets/halal/words.json")
        val server = file("../functions/halal-words.json", "functions/halal-words.json")
        val vectors = file("../functions/halal-vectors.json", "functions/halal-vectors.json")

        @BeforeClass @JvmStatic fun load() { HalalTextFilter.load(assets.readText()) }

        data class Case(val text: String, val action: String, val masked: String?)
        fun cases(): List<Case> {
            val r = JsonReader.of(Buffer().writeUtf8(vectors.readText()))
            val out = mutableListOf<Case>()
            r.beginObject()
            while (r.hasNext()) {
                if (r.nextName() != "cases") { r.skipValue(); continue }
                r.beginArray()
                while (r.hasNext()) {
                    var t = ""; var a = ""; var m: String? = null
                    r.beginObject()
                    while (r.hasNext()) when (r.nextName()) { "text" -> t = r.nextString(); "action" -> a = r.nextString(); "masked" -> m = r.nextString(); else -> r.skipValue() }
                    r.endObject(); out += Case(t, a, m)
                }
                r.endArray()
            }
            r.endObject()
            return out
        }
    }

    private fun action(t: String) = HalalTextFilter.check(t).action

    @Test fun wordListIsByteIdenticalToServer() = assertTrue(assets.readBytes().contentEquals(server.readBytes()))

    @Test fun allSharedVectorsMatchServer() {
        val cs = cases()
        assertTrue(cs.size >= 38)
        val fails = cs.mapNotNull { c ->
            val r = HalalTextFilter.check(c.text)
            val got = r.action.name.lowercase()
            when {
                got != c.action -> "${c.text}: expected ${c.action} got $got"
                c.masked != null && HalalTextFilter.mask(c.text, r) != c.masked -> "${c.text}: masked '${HalalTextFilter.mask(c.text, r)}'"
                else -> null
            }
        }
        assertEquals(fails.joinToString("\n"), 0, fails.size)
    }

    @Test fun englishAndIslamicVocabularyIsAllowed() {
        assertEquals(Action.ALLOW, action("Assalamu alaikum, JazakAllah khair"))
        assertEquals(Action.ALLOW, action("Pork is haram; Surah Al-Kafirun"))
        assertEquals(Action.ALLOW, action("Scunthorpe assessment"))
        assertEquals(Action.ALLOW, action(""))
        assertEquals(Action.MASK, action("this is shit"))
    }

    @Test fun romanUrdu() {
        assertEquals(Action.BLOCK, action("madarchod"))
        assertEquals(Action.BLOCK, action("teri maa ki"))
        assertEquals(Action.MASK, action("tu harami hai"))
    }

    @Test fun hindi() {
        assertEquals(Action.BLOCK, action("भेनचोद"))
        assertEquals(Action.MASK, action("यह कमीना है"))
    }

    @Test fun arabicAndUrduScript() {
        assertEquals(Action.BLOCK, action("شَرْمُوطَة"))
        assertEquals(Action.BLOCK, action("الشيعة كفار"))
        assertEquals(Action.MASK, action("وہ حرامی ہے"))
    }

    @Test fun obfuscation() {
        assertEquals(Action.BLOCK, action("m4d4rch0d"))
        assertEquals(Action.BLOCK, action("b h e n c h o d"))
        assertEquals(Action.BLOCK, action("ma\u200Bdar\u200Cchod"))
        assertEquals(Action.MASK, action("F.U.C.K you"))
        assertEquals(Action.MASK, action("fuuuuck"))
        assertEquals("f••• this", HalalTextFilter.mask("f*ck this"))
    }

    @Test fun sectarianAndScamAreQueuedOrBlocked() {
        assertEquals(Action.BLOCK, action("Shia are kafir"))
        assertEquals(Action.QUEUE, action("send me your OTP for the prize"))
    }

    @Test fun friendlyMessages() {
        val r = HalalTextFilter.check("madarchod")
        assertTrue(r.blocked)
        assertTrue(r.message.contains("Halal community guidelines"))
        assertEquals("", HalalTextFilter.check("salam").message)
        assertTrue(HalalTextFilter.check("send me your OTP for the prize").message.contains("moderator"))
    }
}
