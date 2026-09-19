package com.example

import org.junit.Test
import java.net.URL
import java.util.Scanner

class TestSurahApi {
    @Test
    fun testSurahApi() {
        try {
            val url = URL("https://api.alquran.cloud/v1/quran/en.ahmedraza")
            // Set connection and read timeout if possible, or wrap safely in catch
            val connection = url.openConnection().apply {
                connectTimeout = 3000
                readTimeout = 3000
            }
            val s = Scanner(connection.getInputStream()).useDelimiter("\\A")
            val result = if (s.hasNext()) s.next() else ""
            println("SURAH_OUTPUT: " + result.substring(0, Math.min(300, result.length)))
        } catch (e: Exception) {
            println("Skipping network check in testSurahApi: " + e.message)
        }
    }
}
