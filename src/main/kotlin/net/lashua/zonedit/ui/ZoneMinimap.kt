package net.lashua.zonedit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.Zone

@Composable
fun ZoneMinimap(
    zone: Zone,
    canvasWidth: Int,
    canvasHeight: Int,
    minimapSize: Int = 200,
    modifier: Modifier = Modifier
) {
    val baseGridSize = 20.dp
    
    // Calculate scale factor to fit entire canvas in minimap
    val scaleFactor = minimapSize.toFloat() / maxOf(canvasWidth, canvasHeight)
    
    Box(
        modifier = modifier
            .size(
                (canvasWidth * scaleFactor).dp,
                (canvasHeight * scaleFactor).dp
            )
            .border(1.dp, Color.Gray)
            .drawBehind {
                val gridSizePx = baseGridSize.toPx() * scaleFactor
                val gridLinesHorizontal = (canvasWidth / baseGridSize.value).toInt()
                val gridLinesVertical = (canvasHeight / baseGridSize.value).toInt()
                
                // Draw grid
                repeat(gridLinesHorizontal + 1) { i ->
                    val isMajorLine = i % 10 == 0
                    drawLine(
                        color = if (isMajorLine) Color.Gray else Color.LightGray,
                        start = Offset(i * gridSizePx, 0f),
                        end = Offset(i * gridSizePx, size.height),
                        strokeWidth = if (isMajorLine) 0.75f else 0.25f
                    )
                }
                repeat(gridLinesVertical + 1) { i ->
                    val isMajorLine = i % 10 == 0
                    drawLine(
                        color = if (isMajorLine) Color.Gray else Color.LightGray,
                        start = Offset(0f, i * gridSizePx),
                        end = Offset(size.width, i * gridSizePx),
                        strokeWidth = if (isMajorLine) 0.75f else 0.25f
                    )
                }
                
                // Draw rooms
                for (room in zone.rooms) {
                    drawRect(
                        color = Color.Blue.copy(alpha = 0.3f),
                        topLeft = Offset(
                            room.position.x * scaleFactor,
                            room.position.y * scaleFactor
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            width = 100f * scaleFactor,
                            height = 100f * scaleFactor
                        )
                    )
                }
            }
    )
}