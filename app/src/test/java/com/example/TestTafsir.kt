package com.example

import org.junit.Test
import java.net.URL
import java.util.Scanner

class TestTafsir {
    @Test
    fun testTafsirApi() {
        try {
            val url = URL("https://api.quran.com/api/v4/tafsirs/169/by_ayah/2:1")
            val connection = url.openConnection().apply {
                connectTimeout = 3000
                readTimeout = 3000
            }
            val s = Scanner(connection.getInputStream()).useDelimiter("\\A")
            println("TAFSIR_OUTPUT: " + (if (s.hasNext()) s.next() else ""))
        } catch (e: Exception) {
            println("Skipping network check in testTafsirApi: " + e.message)
        }
    }
}
