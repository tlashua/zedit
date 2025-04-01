package net.lashua.zonedit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val CustomTypography = Typography(
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 12.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp),
    // You can customize other text styles as needed
    bodyLarge = Typography().bodyLarge.copy(fontSize = 14.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.sp),
)

@Composable
fun ZoneEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = CustomTypography,
        colorScheme = lightColorScheme(), // You can customize colors too
        content = content
    )
}