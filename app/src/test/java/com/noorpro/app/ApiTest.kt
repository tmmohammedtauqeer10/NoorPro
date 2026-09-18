package com.noorpro.app
import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.noorpro.app.data.GithubApiService

class ApiTest {
    @Test
    fun fetchLibrary() {
        runBlocking {
            try {
                val lib = GithubApiService.fetchLibraryCatalogSafely()
                println("SUCCESS_LIB: " + lib.size)
            } catch(e: Exception) {
                println("ERROR_LIB: \${e.message} \${e.javaClass.name}")
                e.printStackTrace()
            }
            try {
                val quiz = GithubApiService.fetchQuizDataSafely()
                println("SUCCESS_QUIZ: " + quiz.size)
            } catch(e: Exception) {
                println("ERROR_QUIZ: \${e.message} \${e.javaClass.name}")
                e.printStackTrace()
            }
        }
    }
}
