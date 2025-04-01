package net.lashua.zonedit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Typography definition
private val CustomTypography = Typography(
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 12.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 14.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.sp),
)

// Material 3 default schemes
private val md_theme_light_primary = Color(0xFF0061A4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
private val md_theme_light_secondary = Color(0xFF535F70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD7E3F7)
private val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)

// You can choose one of these predefined schemes
private val DefaultLightScheme = lightColorScheme() // Material's default purple theme
private val BlueScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer
)

@Composable
fun ZoneEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = CustomTypography,
        colorScheme = BlueScheme, // Try different schemes here
        content = content
    )
}