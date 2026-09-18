package com.noorpro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
