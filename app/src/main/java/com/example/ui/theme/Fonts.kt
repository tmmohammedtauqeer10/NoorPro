package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.R

/**
 * "Sacred Serenity" design-system fonts (bundled static TTFs in res/font).
 * Libre Caslon Text → spiritual/heading content. Plus Jakarta Sans → functional/body UI.
 */
val LibreCaslon = FontFamily(
    Font(R.font.librecaslontext_regular, FontWeight.Normal),
    Font(R.font.librecaslontext_regular, FontWeight.Medium),
    Font(R.font.librecaslontext_regular, FontWeight.SemiBold),
    Font(R.font.librecaslontext_bold, FontWeight.Bold),
    Font(R.font.librecaslontext_bold, FontWeight.ExtraBold),
    Font(R.font.librecaslontext_bold, FontWeight.Black),
    Font(R.font.librecaslontext_italic, FontWeight.Normal, FontStyle.Italic),
)

val PlusJakarta = FontFamily(
    Font(R.font.plusjakartasans_regular, FontWeight.Normal),
    Font(R.font.plusjakartasans_medium, FontWeight.Medium),
    Font(R.font.plusjakartasans_semibold, FontWeight.SemiBold),
    Font(R.font.plusjakartasans_bold, FontWeight.Bold),
    Font(R.font.plusjakartasans_extrabold, FontWeight.ExtraBold),
    Font(R.font.plusjakartasans_extrabold, FontWeight.Black),
)
