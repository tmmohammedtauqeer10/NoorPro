package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    @Json(name = "question_id") val questionId: String,
    val text: String,
    val options: List<String>,
    @Json(name = "correct_index") val correctIndex: Int
)

@JsonClass(generateAdapter = true)
data class QuizCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    val difficulty: String,
    @Json(name = "points_per_question") val pointsPerQuestion: Int,
    val questions: List<QuizQuestion>
)

@JsonClass(generateAdapter = true)
data class QuizResponse(
    val status: String,
    @Json(name = "total_categories") val totalCategories: Int,
    val categories: List<QuizCategory>
)

val MOCK_QUIZ_JSON = """
{
  "status": "success",
  "total_categories": 2,
  "categories": [
    {
      "category_id": "seerah_01",
      "category_name": "Seerah (Life of the Prophet)",
      "difficulty": "Easy",
      "points_per_question": 50,
      "questions": [
        {
          "question_id": "q_see_001",
          "text": "In which city was Prophet Muhammad (PBUH) born?",
          "options": ["Medina", "Mecca", "Taif", "Jerusalem"],
          "correct_index": 1
        },
        {
          "question_id": "q_see_002",
          "text": "What was the name of the mountain where the Prophet (PBUH) received the first revelation?",
          "options": ["Mount Uhud", "Mount Safa", "Mount Hira", "Mount Sinai"],
          "correct_index": 2
        }
      ]
    },
    {
      "category_id": "fiqh_01",
      "category_name": "Basic Fiqh (Jurisprudence)",
      "difficulty": "Medium",
      "points_per_question": 50,
      "questions": [
        {
          "question_id": "q_fiq_001",
          "text": "How many obligatory (Fard) acts are there in Wudu according to the Hanafi school?",
          "options": ["3", "4", "6", "5"],
          "correct_index": 1
        }
      ]
    }
  ]
}
""".trimIndent()
