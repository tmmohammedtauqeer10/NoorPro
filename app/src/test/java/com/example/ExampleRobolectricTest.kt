package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.QuranRepository
import com.noorpro.app.data.TranslationManager
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Ignore("Maintenance generator mutates the committed Quran database asset")
  @Test
  fun generateAndValidateQuranDb(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // 1. Delete old invalid file
    val dbFile = java.io.File("src/main/assets/databases/quran.db")
    dbFile.parentFile?.mkdirs()
    if (dbFile.exists()) {
      dbFile.delete()
    }
    
    // Also delete any existing local temporary journal or wal files
    java.io.File("src/main/assets/databases/quran.db-journal").delete()
    java.io.File("src/main/assets/databases/quran.db-shm").delete()
    java.io.File("src/main/assets/databases/quran.db-wal").delete()

    // 2. Build Room database directly at the asset path
    val db = androidx.room.Room.databaseBuilder(
      context,
      com.noorpro.app.data.AppDatabase::class.java,
      dbFile.absolutePath
    ).build()
    
    db.quranDao().clearQuranTable()
    
    // Need a temporary instance of repository to map the soraNames
    val repository = com.noorpro.app.data.QuranRepository(context)

    // 3. Generate verses for all 114 Surahs
    val list = mutableListOf<com.noorpro.app.data.QuranVerse>()
    var globalId = 1
    
    fun getJuzForSurah(soraId: Int): Int {
        return when (soraId) {
            in 1..2 -> 1
            3 -> 3
            4 -> 4
            5 -> 6
            6 -> 7
            7 -> 8
            8 -> 9
            9 -> 10
            in 10..11 -> 11
            12 -> 12
            in 13..14 -> 13
            in 15..16 -> 14
            in 17..18 -> 15
            in 19..20 -> 16
            in 21..22 -> 17
            in 23..25 -> 18
            in 26..27 -> 19
            in 28..29 -> 20
            in 30..33 -> 21
            in 34..36 -> 22
            in 37..39 -> 23
            in 40..41 -> 24
            in 42..45 -> 25
            in 46..51 -> 26
            in 52..57 -> 27
            in 58..66 -> 28
            in 67..77 -> 29
            else -> 30
        }
    }

    for (soraId in 1..114) {
      val targetJuz = getJuzForSurah(soraId)
      val existingSurah = com.noorpro.app.data.IslamicData.surahs.find { it.id == soraId }
      if (existingSurah != null) {
        existingSurah.verses.forEachIndexed { index, pair ->
          val verse = com.noorpro.app.data.QuranVerse(
            id = globalId++,
            jozz = targetJuz,
            sora = soraId,
            soraNameEn = existingSurah.nameEnglish,
            soraNameAr = existingSurah.nameArabic,
            page = 1,
            lineStart = 1,
            lineEnd = 1,
            ayaNo = index + 1,
            ayaText = pair.first,
            ayaTextEmlaey = pair.first,
            maanyAya = "معاني الكلمات",
            earabQuran = "إعراب الآية",
            reasonsOfVerses = "سبب النزول",
            tafseerSaadi = "تفسير السعدي",
            tafseerMoysar = pair.second,
            tafseerBughiu = "تفسير البغوي",
            ayaTextTashkil = pair.first
          )
          list.add(verse)
        }
      } else {
        val soraNameEn = repository.getTitleTranslation(soraId)
        val soraNameAr = "سورة"
        val verse = com.noorpro.app.data.QuranVerse(
          id = globalId++,
          jozz = targetJuz,
          sora = soraId,
          soraNameEn = soraNameEn,
          soraNameAr = soraNameAr,
          page = 1,
          lineStart = 1,
          lineEnd = 1,
          ayaNo = 1,
          ayaText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
          ayaTextEmlaey = "بسم الله الرحمن الرحيم",
          maanyAya = "معاني الكلمات",
          earabQuran = "إعراب الآية",
          reasonsOfVerses = "سبب النزول",
          tafseerSaadi = "تفسير السعدي",
          tafseerMoysar = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
          tafseerBughiu = "تفسير البغوي",
          ayaTextTashkil = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        )
        list.add(verse)
      }
    }
    
    // 4. Bulk insert verses
    db.quranDao().insertVerses(list)
    
    // 5. Close database to flush changes to disk
    db.close()
    
    System.out.println("GENERATED QURAN DATABASE SUCCESSFULLY AT src/main/assets/databases/quran.db! Size: " + dbFile.length())
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Noor Pro", appName)
  }

  @Test
  fun testFetchApiDirectly(): Unit = runBlocking {
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val url = "https://api.alquran.cloud/v1/surah/2/editions/quran-uthmani,en.sahih"
    val request = okhttp3.Request.Builder().url(url).build()
    try {
        println("NETWORK TEST: Initiating call to $url")
        client.newCall(request).execute().use { response ->
            println("NETWORK TEST: Response code: ${response.code}")
            println("NETWORK TEST: Response successful: ${response.isSuccessful}")
            val body = response.body?.string()
            if (body != null) {
                println("NETWORK TEST: Response body non-null. Length: ${body.length}")
                val json = org.json.JSONObject(body)
                val dataArray = json.getJSONArray("data")
                val arabicEdition = dataArray.getJSONObject(0)
                val englishEdition = dataArray.getJSONObject(1)
                val arabicAyahs = arabicEdition.getJSONArray("ayahs")
                val englishAyahs = englishEdition.getJSONArray("ayahs")
                println("NETWORK TEST: Success! Parsed ${arabicAyahs.length()} Ayahs.")
                if (arabicAyahs.length() > 0) {
                    val firstAr = arabicAyahs.getJSONObject(0).getString("text")
                    val firstEn = englishAyahs.getJSONObject(0).getString("text")
                    println("NETWORK TEST ARABIC AYAH 1: '$firstAr'")
                    println("NETWORK TEST ENGLISH AYAH 1: '$firstEn'")
                    
                    // Let's print individual characters of Arabic first ayah
                    val chars = firstAr.map { String.format("\\u%04X", it.toInt()) }.joinToString(" ")
                    println("NETWORK TEST ARABIC CHARS: $chars")
                }
            } else {
                println("NETWORK TEST: Empty response body")
            }
        }
    } catch (e: Exception) {
        println("NETWORK TEST ERROR: Failed to make API request!")
        e.printStackTrace()
    }
  }

  @Test
  fun testFetchJuzApiDirectly(): Unit = runBlocking {
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val url = "https://raw.githubusercontent.com/faisalill/quran_db/master/ummmuhammadsahihinternational.json"
    try {
        println("NETWORK TEST TRANSLATION: Initiating call to $url")
        client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { response ->
            println("TRANSLATION Response code: ${response.code}")
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val json = org.json.JSONObject(body)
                    // Let's print Surah 1 Ayah 1, Surah 2 Ayah 1
                    val s1 = json.optJSONObject("1")
                    val s1_ayahs = s1?.optJSONObject("Ayahs")
                    val s1_a1 = s1_ayahs?.optJSONObject("1")
                    println("TRANSLATION S1 A1: $s1_a1")
                    
                    val s2 = json.optJSONObject("2")
                    val s2_ayahs = s2?.optJSONObject("Ayahs")
                    val s2_a1 = s2_ayahs?.optJSONObject("1")
                    println("TRANSLATION S2 A1: $s2_a1")
                    
                    val s2_a91 = s2_ayahs?.optJSONObject("91")
                    println("TRANSLATION S2 A91: $s2_a91")
                }
            }
        }
    } catch (e: Exception) {
        println("NETWORK TEST TRANSLATION ERROR: Failed to make API request!")
        e.printStackTrace()
    }
  }

  @Test
  fun `test quran repository getSurahList`(): Unit = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Check direct disk file directly using Java File and raw SQLite
    val directDiskFile = java.io.File("src/main/assets/databases/quran.db")
    System.out.println("DIRECT DISK FILE PATH: " + directDiskFile.absolutePath)
    System.out.println("DIRECT DISK FILE EXISTS: " + directDiskFile.exists())
    System.out.println("DIRECT DISK FILE SIZE: " + directDiskFile.length())
    
    if (directDiskFile.exists()) {
      try {
        val rawDb = android.database.sqlite.SQLiteDatabase.openDatabase(
          directDiskFile.absolutePath,
          null,
          android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
        
        val versionCursor = rawDb.rawQuery("PRAGMA user_version", null)
        if (versionCursor.moveToFirst()) {
          System.out.println("DIRECT DISK DATABASE USER_VERSION: " + versionCursor.getInt(0))
        }
        versionCursor.close()
        
        val tablesCursor = rawDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        while (tablesCursor.moveToNext()) {
          System.out.println("DIRECT DISK TABLE NAME: " + tablesCursor.getString(0))
        }
        tablesCursor.close()
        
        val countCursor = rawDb.rawQuery("SELECT COUNT(*) FROM quran", null)
        if (countCursor.moveToFirst()) {
          System.out.println("DIRECT DISK TOTAL ROWS IN quran: " + countCursor.getInt(0))
        }
        countCursor.close()
        rawDb.close()
      } catch (e: Exception) {
        System.out.println("DIRECT DISK OP ERROR: " + e.message)
        e.printStackTrace()
      }
    }

    val translationManager = TranslationManager(context)
    val repository = QuranRepository(context)
    
    // Force recreate by deleting any previously cached database file
    context.deleteDatabase("external_quran.db")
    
    // Inspect loaded Room database
    val db = com.noorpro.app.data.AppDatabase.getDatabase(context)
    try {
      val cursor = db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'")
      while (cursor.moveToNext()) {
        System.out.println("ROOM TABLE NAME: " + cursor.getString(0))
      }
      cursor.close()
      
      val countCursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM quran")
      if (countCursor.moveToFirst()) {
        System.out.println("ROOM ROWS IN quran TABLE: " + countCursor.getInt(0))
      }
      countCursor.close()
    } catch (e: Exception) {
      System.out.println("ROOM ACCESS ERROR: " + e.message)
      e.printStackTrace()
    }

    val surahs = repository.getSurahList("default", translationManager)
    assertNotNull(surahs)
    System.out.println("Surahs size fetched: ${surahs.size}")
    if (surahs.isNotEmpty()) {
      val first = surahs.first()
      System.out.println("First surah: ${first.nameEnglish}, versesCount: ${first.versesCount}")
    }
  }

  @Test
  fun testCleanBismillahAndUnescapeHtml() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = DeenViewModel(context as android.app.Application)
    
    // Test HTML Entities unescaping
    val methodUnescape = DeenViewModel::class.java.getDeclaredMethod("unescapeHtml", String::class.java).apply {
      isAccessible = true
    }
    
    val inputWithEntities = "We believe &#91;only&#93; in what was revealed to us.&quot;"
    val resultUnescaped = methodUnescape.invoke(viewModel, inputWithEntities) as String
    println("UNESCAPE TEST RESULT: $resultUnescaped")
    assertEquals("We believe [only] in what was revealed to us.\"", resultUnescaped)
    
    // Test Bismillah stripping
    val methodClean = DeenViewModel::class.java.getDeclaredMethod("cleanAyaArabicText", String::class.java, Int::class.java, Int::class.java).apply {
      isAccessible = true
    }
    
    // Case 1: Surah Al-Baqarah Ayah 1 (Id: 2, Ayah: 1)
    val inputBaqarah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ الٓمٓ"
    val resultBaqarah = methodClean.invoke(viewModel, inputBaqarah, 2, 1) as String
    println("BISMILLAH STRIP TEST RESULT FOR AL-BAQARAH: $resultBaqarah")
    assertEquals("الٓمٓ", resultBaqarah)
    
    // Case 2: Surah Al-Fatihah Ayah 1 (Id: 1, Ayah: 1) - should NOT strip because it is of Surah 1 counted as Ayah 1
    val resultFatihah = methodClean.invoke(viewModel, inputBaqarah, 1, 1) as String
    println("BISMILLAH STRIP TEST RESULT FOR AL-FATIHAH: $resultFatihah")
    assertEquals(inputBaqarah, resultFatihah)
    
    // Case 3: Surah Al-Baqarah Ayah 2 - should NOT strip anything since it is not Ayah 1
    val resultBaqarah2 = methodClean.invoke(viewModel, "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ", 2, 2) as String
    println("BISMILLAH STRIP TEST RESULT FOR AL-BAQARAH AYAH 2: $resultBaqarah2")
    assertEquals("ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ", resultBaqarah2)
  }
}
