package net.lashua.zonedit.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import net.lashua.zonedit.model.ConnectionManager
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import org.slf4j.LoggerFactory

/**
 * Component for drawing rooms on the canvas
 */
object RoomComponent {
    private val log = LoggerFactory.getLogger(RoomComponent::class.java)
    private val connectionManager = ConnectionManager()

    /**
     * Draws a room on the canvas
     *
     * @param drawScope The DrawScope to draw in
     * @param room The room to draw
     * @param zone The zone containing the room
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @param isSelected Whether the room is selected
     * @param textMeasurer The TextMeasurer for drawing text
     */
    fun draw(
        drawScope: DrawScope,
        room: Room,
        zone: Zone,
        density: Float,
        zoomLevel: Float,
        isSelected: Boolean,
        textMeasurer: TextMeasurer
    ) {
        val rect = getRoomRect(room, zone, density, zoomLevel)

        // Only attempt to draw if the room has positive dimensions
        if (rect.width <= 0 || rect.height <= 0) return

        drawScope.apply {
            // Draw room background
            drawRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Fill
            )

            // Draw room border
            drawRect(
                color = if (isSelected) Color.Blue else Color.Black,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = if (isSelected) 2f else 1f)
            )

            // Only attempt to draw text if there's enough space
            if (rect.width >= 10 && rect.height >= 10) {  // Minimum size threshold for text
                try {
                    // Create base font sizes
                    val nameFontSize = (14 * zoomLevel).sp
                    val idFontSize = (10 * zoomLevel).sp

                    // Measure text dimensions
                    val nameStyle = TextStyle(fontSize = nameFontSize, color = Color.Black)
                    val idStyle = TextStyle(fontSize = idFontSize, color = Color.Gray)

                    val nameMeasure = textMeasurer.measure(room.name, nameStyle)
                    // No need to measure ID text separately since we're using the TextMeasurer directly in drawText

                    // Calculate vertical spacing between name and id
                    val verticalSpacing = 4f * zoomLevel

                    // Draw name
                    drawText(
                        textMeasurer = textMeasurer,
                        text = room.name,
                        topLeft = rect.topLeft + Offset(8f * zoomLevel, 8f * zoomLevel),
                        style = nameStyle
                    )

                    // Draw ID below name with proper spacing
                    drawText(
                        textMeasurer = textMeasurer,
                        text = room.id,
                        topLeft = rect.topLeft + Offset(
                            8f * zoomLevel,
                            8f * zoomLevel + nameMeasure.size.height + verticalSpacing
                        ),
                        style = idStyle
                    )

                    // Draw connection points if selected
                    if (isSelected) {
                        drawConnectionPoints(rect, room, density, zoomLevel)
                    }
                } catch (e: IllegalArgumentException) {
                    log.trace("Skipping text draw for room {} due to size constraints", room.id)
                }
            }
        }
    }

    /**
     * Draws connection points for a room
     *
     * @param rect The rectangle of the room
     * @param room The room to draw connection points for
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     */
    private fun DrawScope.drawConnectionPoints(
        rect: Rect,
        room: Room,
        density: Float,
        zoomLevel: Float
    ) {
        val connectionPointSize = 12f * density * zoomLevel

        // Cardinal direction points (N,S,E,W)
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.center.x, rect.top),
            style = Fill
        )

        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.center.x, rect.bottom),
            style = Fill
        )

        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.right, rect.center.y),
            style = Fill
        )

        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.left, rect.center.y),
            style = Fill
        )

        // UP/DOWN connection points are always shown when room is selected
        val upColor = Color.Green
        val downColor = Color.Green

        // Top corners (UP)
        drawCircle(
            color = upColor,
            radius = connectionPointSize / 2,
            center = Offset(rect.right, rect.top),
            style = Fill
        )

        drawCircle(
            color = upColor,
            radius = connectionPointSize / 2,
            center = Offset(rect.left, rect.top),
            style = Fill
        )

        // Bottom corners (DOWN)
        drawCircle(
            color = downColor,
            radius = connectionPointSize / 2,
            center = Offset(rect.right, rect.bottom),
            style = Fill
        )

        drawCircle(
            color = downColor,
            radius = connectionPointSize / 2,
            center = Offset(rect.left, rect.bottom),
            style = Fill
        )
    }

    /**
     * Gets the rectangle for a room
     *
     * @param room The room to get the rectangle for
     * @param zone The zone containing the room
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The rectangle for the room
     */
    fun getRoomRect(room: Room, zone: Zone, density: Float, zoomLevel: Float): Rect {
        return connectionManager.getRoomRect(room, zone, density, zoomLevel)
    }
}
