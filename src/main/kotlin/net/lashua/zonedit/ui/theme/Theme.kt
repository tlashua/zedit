package net.lashua.zonedit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

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
    CompositionLocalProvider(
        LocalSplitPaneStyle provides SplitPaneStyle(
            splitter = SplitterStyle(
                width = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                gripColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                gripSize = 20.dp,
                gripGap = 4.dp,
                gripCount = 3
            )
        )
    ) {
        MaterialTheme(
            typography = CustomTypography,
            colorScheme = BlueScheme,
            content = content
        )
    }
}

private data class SplitPaneStyle(
    val splitter: SplitterStyle
)

private data class SplitterStyle(
    val width: Dp,
    val color: Color,
    val gripColor: Color,
    val gripSize: Dp,
    val gripGap: Dp,
    val gripCount: Int
)

private val LocalSplitPaneStyle = compositionLocalOf { 
    SplitPaneStyle(
        splitter = SplitterStyle(
            width = 4.dp,
            color = Color.Transparent,
            gripColor = Color.Gray,
            gripSize = 20.dp,
            gripGap = 4.dp,
            gripCount = 3
        )
    ) 
}

@Composable
fun CustomSplitter() {
    val style = LocalSplitPaneStyle.current
    Box(
        Modifier
            .width(style.splitter.width)
            .fillMaxHeight()
            .background(style.splitter.color)
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(style.splitter.gripGap)
        ) {
            repeat(style.splitter.gripCount) {
                Box(
                    Modifier
                        .width(style.splitter.gripSize)
                        .height(1.dp)
                        .background(
                            style.splitter.gripColor,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
