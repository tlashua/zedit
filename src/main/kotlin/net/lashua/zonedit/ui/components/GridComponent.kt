package net.lashua.zonedit.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember

/**
 * Component for drawing a grid on the canvas
 */
object GridComponent {
    /**
     * Draws a grid on the canvas
     *
     * @param drawScope The DrawScope to draw in
     * @param gridSizeDp The size of each grid cell in dp
     * @param zoomLevel The current zoom level
     * @param size The size of the canvas
     */
    fun draw(
        drawScope: DrawScope,
        gridSizeDp: Dp = 20.dp,
        zoomLevel: Float,
        size: Size
    ) {
        val gridSizePx = with(drawScope) { gridSizeDp.toPx() }
        drawScope.apply {
            val horizontalLines = (size.height / (gridSizePx * zoomLevel)).toInt()
            val verticalLines = (size.width / (gridSizePx * zoomLevel)).toInt()

            // Draw horizontal lines
            repeat(horizontalLines + 1) { i ->
                val y = i * gridSizePx * zoomLevel
                val isMajor = i % 10 == 0
                drawLine(
                    color = if (isMajor) Color.Gray else Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (isMajor) 1f else 0.5f
                )
            }

            // Draw vertical lines
            repeat(verticalLines + 1) { i ->
                val x = i * gridSizePx * zoomLevel
                val isMajor = i % 10 == 0
                drawLine(
                    color = if (isMajor) Color.Gray else Color.LightGray,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (isMajor) 1f else 0.5f
                )
            }
        }
    }
}
