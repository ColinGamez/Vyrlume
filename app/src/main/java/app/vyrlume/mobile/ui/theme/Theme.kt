package app.vyrlume.mobile.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AeroMidnight = Color(0xFF061B2D)
val AeroDeep = Color(0xFF0A2A40)
val AeroSurface = Color(0xFF10344B)
val AeroSky = Color(0xFF58D8FF)
val AeroBlue = Color(0xFF159DD8)
val AeroMint = Color(0xFFA5E65A)
val AeroText = Color(0xFFF2FAFF)
val AeroMuted = Color(0xFF9EB8C8)
val AeroError = Color(0xFFFF8D96)

private val AeroColors = darkColorScheme(
    primary = AeroSky,
    onPrimary = AeroMidnight,
    primaryContainer = Color(0xFF164E68),
    onPrimaryContainer = AeroText,
    secondary = AeroMint,
    onSecondary = AeroMidnight,
    secondaryContainer = Color(0xFF344F28),
    onSecondaryContainer = AeroText,
    background = AeroMidnight,
    onBackground = AeroText,
    surface = AeroDeep,
    onSurface = AeroText,
    surfaceVariant = AeroSurface,
    onSurfaceVariant = AeroMuted,
    outline = Color(0xFF416176),
    error = AeroError,
)

private val AeroTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun VyrlumeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AeroColors,
        typography = AeroTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides AeroText) {
            content()
        }
    }
}
