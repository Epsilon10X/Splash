package com.splash.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splash.offline.data.ThemeMode

val SplashLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3E7D28),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F0C3),
    onPrimaryContainer = Color(0xFF15360B),
    secondary = Color(0xFF4D6B42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2F4C8),
    onSecondaryContainer = Color(0xFF1C3D0C),
    tertiary = Color(0xFFB68312),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9ECC2),
    onTertiaryContainer = Color(0xFF453100),
    background = Color(0xFFFAF8F3),
    onBackground = Color(0xFF1D221C),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF1D221C),
    surfaceVariant = Color(0xFFEBE7DD),
    onSurfaceVariant = Color(0xFF565E54),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3EC),
    surfaceContainer = Color(0xFFEFEDE4),
    surfaceContainerHigh = Color(0xFFE8E6DC),
    surfaceContainerHighest = Color(0xFFDFDDD2),
    outline = Color(0xFF7B8278),
    outlineVariant = Color(0xFFD5D2C7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val SplashAmoledDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9BE06B),
    onPrimary = Color(0xFF143008),
    primaryContainer = Color(0xFF234717),
    onPrimaryContainer = Color(0xFFE2F4C8),
    secondary = Color(0xFFAAD395),
    onSecondary = Color(0xFF193612),
    secondaryContainer = Color(0xFF1E3616),
    onSecondaryContainer = Color(0xFFE0F5C4),
    tertiary = Color(0xFFECC24E),
    onTertiary = Color(0xFF362500),
    tertiaryContainer = Color(0xFF3A2E10),
    onTertiaryContainer = Color(0xFFFCEFC2),
    background = Color.Black,
    onBackground = Color(0xFFF2F1EE),
    surface = Color.Black,
    onSurface = Color(0xFFF2F1EE),
    surfaceVariant = Color(0xFF20242B),
    onSurfaceVariant = Color(0xFFA7AEA9),
    surfaceContainerLowest = Color(0xFF0D0F12),
    surfaceContainerLow = Color(0xFF121519),
    surfaceContainer = Color(0xFF181B20),
    surfaceContainerHigh = Color(0xFF20242B),
    surfaceContainerHighest = Color(0xFF292E37),
    outline = Color(0xFF5A615E),
    outlineVariant = Color(0xFF2B303A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val SplashExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

val SplashExpressiveTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun SplashTheme(
    themeMode: ThemeMode,
    forceAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = forceAmoledBlack || when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val scheme = if (useDark) SplashAmoledDarkColorScheme else SplashLightColorScheme

    MaterialExpressiveTheme(
        colorScheme = scheme,
        shapes = SplashExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
        typography = SplashExpressiveTypography,
        content = content
    )
}
