package com.noorpro.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.noorpro.app.R
import com.noorpro.app.data.OfflineQuranAssets

/** Bundled Naskh font that covers every Quranic mark used by the normal Quran reader (system fonts on some phones do not). */
val QuranArabicFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))

/** Arabic Quran text ready for display: Indo-Pak private-use marks mapped to standard Unicode, nothing dropped. */
fun quranDisplayText(s: String): String = OfflineQuranAssets.indoPakToUnicode(s)

/** Full-contrast colour for Arabic Quran text (near-black on light, cream on dark). */
@Composable
fun quranArabicColor(): Color = stitchText()
