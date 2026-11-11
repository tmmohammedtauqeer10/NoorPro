package com.noorpro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Dark theme: same "Sacred Serenity" family as light, translated into deep
// emerald-navy surfaces with warm cream text and soft gold accents.
val DeepNightBlueStatic = Color(0xFF101B18) // raised cards / sheets
val NightBackgroundStatic = Color(0xFF07110F) // app background
val SlateCardStatic = Color(0xFF17241F) // tonal layer / chips
val MatteGold = Color(0xFFD4AF37) // primary
val CustomSecondary = Color(0xFF2A4F43) // secondary emerald
val TextPrimaryStatic = Color(0xFFFAF4E8)

// "Sacred Serenity" light theme — deep emerald + soft gold on warm cream (Stitch design system).
val LightBackgroundStatic = Color(0xFFFCFAFA)   // warm cream background (Surface 1)
val LightSurfaceStatic = Color(0xFFFFFFFF)      // pure white cards (Surface 2)
val LightCardStatic = Color(0xFFF1F5F3)         // subtle tonal layer
val CustomPrimaryLightStatic = Color(0xFF064E3B) // deep emerald primary
val TextPrimaryLightStatic = Color(0xFF0D1C2F)  // slate on-surface
val TextSecondaryLightStatic = Color(0xFF404944) // on-surface-variant

val LightGold = Color(0xFFD4AF37) // Use same gold for consistency
val GlowGold = Color(0xFFFFD700)
val DarkEmerald = Color(0xFF0C241E)

val GlassOverlay = Color(0x18D4AF37) // warm translucent depth in both themes
val GlassBorder = Color(0x45D4AF37) // visible, restrained gold outline

val TextSecondaryStatic = Color(0xFFC6BCAB)

// Dynamic Composable Color Adapters! This makes the UI work perfectly in Light mode
// without rewriting all the files calling these constants.
val NightBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val DeepNightBlue: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val SlateCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val TextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val FajrColor = Color(0xFF33D1FF)
val SunriseColor = Color(0xFFFF9E00)
val DhuhrColor = Color(0xFFFFD60A)
val AsrColor = Color(0xFFE07A5F)
val MaghribColor = Color(0xFF8338EC)
val IshaColor = Color(0xFF3A86C8)

// ---- Theme-aware tokens (respect the app's System/Light/Dark setting via MaterialTheme) ----
/** True when the active app theme is dark (use this, never isSystemInDarkTheme(), inside screens). */
val isAppDark: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Gold accent with enough contrast in both modes (bright gold on dark, deep gold on light). */
val AppGold: Color
    @Composable get() = if (isAppDark) Color(0xFFD4AF37) else Color(0xFF8A6A00)

/** Ink colour for Quran Arabic text. */
val QuranInk: Color
    @Composable get() = if (isAppDark) Color(0xFFF3EBD9) else Color(0xFF14231D)

/** Tajweed rule colours with separate light/dark variants (readable on cream and on deep green). */
fun tajweedColor(rule: String?, dark: Boolean): Color = when (rule) {
    "ghunna" -> if (dark) Color(0xFFFFB04D) else Color(0xFFC46A00)
    "ikhfa" -> if (dark) Color(0xFFCE93D8) else Color(0xFF8E44AD)
    "idgham" -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
    "iqlab" -> if (dark) Color(0xFF90CAF9) else Color(0xFF1E5FBF)
    "izhar" -> if (dark) Color(0xFF4DD0C1) else Color(0xFF00796B)
    "qalqala" -> if (dark) Color(0xFFFF8A80) else Color(0xFFC62828)
    "madd" -> if (dark) Color(0xFFF48FB1) else Color(0xFFAD1457)
    "tafkheem" -> if (dark) Color(0xFF9FA8DA) else Color(0xFF283593)
    "tarqeeq" -> if (dark) Color(0xFF81D4FA) else Color(0xFF0277BD)
    "waqf" -> if (dark) Color(0xFFBDBDBD) else Color(0xFF616161)
    "hamzawasl" -> if (dark) Color(0xFFB0BEC5) else Color(0xFF607D8B)
    "special" -> if (dark) Color(0xFFFFD54F) else Color(0xFF9A6B00)
    else -> Color.Unspecified
}

private val arabicRun = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF](?:[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF\\s،]*[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF])?")

/**
 * Wraps Arabic runs inside a mostly-Latin sentence in Unicode first-strong isolates (FSI … PDI) so
 * letters like "(ي ن م و)" keep their order and the surrounding brackets/punctuation stay in place.
 */
fun isolateArabic(text: String): String {
    val arabicCount = text.count { it in '\u0600'..'\u06FF' }
    val latinCount = text.count { it in 'A'..'Z' || it in 'a'..'z' }
    if (arabicCount == 0 || latinCount == 0) return text
    return arabicRun.replace(text) { "\u2068" + it.value + "\u2069" }
}
