package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme =
  darkColorScheme(
    primary = MatteGold,
    secondary = CustomSecondary,
    tertiary = DarkEmerald,
    background = NightBackgroundStatic,
    surface = DeepNightBlueStatic,
    onPrimary = NightBackgroundStatic,
    onSecondary = TextPrimaryStatic,
    onBackground = TextPrimaryStatic,
    onSurface = TextPrimaryStatic,
    surfaceVariant = SlateCardStatic,
    onSurfaceVariant = TextSecondaryStatic,
    outline = Color(0xFF506158),
    outlineVariant = Color(0xFF2B3C35),
    primaryContainer = Color(0xFF26351F),
    onPrimaryContainer = Color(0xFFF5E6B2),
    secondaryContainer = Color(0xFF14362D),
    onSecondaryContainer = Color(0xFFDDF4EA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CustomPrimaryLightStatic,        // deep emerald #064E3B
    secondary = Color(0xFF735C00),             // dark gold
    tertiary = Color(0xFF645F40),
    background = LightBackgroundStatic,         // cream #FCFAFA
    surface = LightSurfaceStatic,               // white
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLightStatic,      // slate #0D1C2F
    onSurface = TextPrimaryLightStatic,
    surfaceVariant = LightCardStatic,
    onSurfaceVariant = TextSecondaryLightStatic,
    outline = Color(0xFFBFC9C3),
    outlineVariant = Color(0xFFE2E8F0),
    primaryContainer = Color(0xFFE4F1EC),       // emerald 10% tint (chips)
    onPrimaryContainer = Color(0xFF064E3B),
    secondaryContainer = Color(0xFFFED65B),     // soft gold (badges)
    onSecondaryContainer = Color(0xFF745C00),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  
  val view = LocalView.current
  if (!view.isInEditMode) {
      SideEffect {
          val window = (view.context as Activity).window
          window.statusBarColor = colorScheme.background.toArgb()
          window.navigationBarColor = colorScheme.background.toArgb()
          androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
              isAppearanceLightStatusBars = !darkTheme
              isAppearanceLightNavigationBars = !darkTheme
          }
      }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    // Default any unstyled Text() to Plus Jakarta Sans so the whole app picks up the
    // Sacred Serenity body font; headings opt into Libre Caslon explicitly.
    CompositionLocalProvider(
      LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = PlusJakarta)
    ) {
      content()
    }
  }
}
