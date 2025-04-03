package net.lashua.zonedit.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import net.lashua.zonedit.model.ConnectionManager
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.util.CoordinateConverter

/**
 * Component for drawing connections between rooms
 */
object ConnectionComponent {
    private val connectionManager = ConnectionManager()

    /**
     * Draws all connections for a room
     *
     * @param drawScope The DrawScope to draw in
     * @param room The room to draw connections for
     * @param zone The zone containing the room
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     */
    fun drawConnections(
        drawScope: DrawScope,
        room: Room,
        zone: Zone,
        density: Float,
        zoomLevel: Float
    ) {
        val sourceRect = RoomComponent.getRoomRect(room, zone, density, zoomLevel)

        drawScope.apply {
            for ((exitDir, destId) in room.exits) {
                val destRoom = zone.rooms.find { it.id == destId } ?: continue

                // For all directions, only draw the connection once
                if (connectionManager.shouldDrawConnection(room, exitDir, destRoom)) {
                    // Verify connection exists before drawing
                    if (connectionManager.getConnection(room, exitDir, destRoom) == null) continue

                    val destRect = RoomComponent.getRoomRect(destRoom, zone, density, zoomLevel)

                    // Get source and destination points
                    val (sourcePoint, destPoint) = connectionManager.getConnectionPoints(
                        exitDir, sourceRect, destRect, room, destRoom
                    )

                    val connectionColor = connectionManager.getConnectionColor(exitDir)

                    // Draw the line
                    drawLine(
                        color = connectionColor,
                        start = sourcePoint,
                        end = destPoint,
                        strokeWidth = CoordinateConverter.scaleWithZoom(2f, zoomLevel)
                    )

                    // Draw arrows
                    drawArrows(sourcePoint, destPoint, connectionColor, zoomLevel)
                }
            }
        }
    }

    /**
     * Draws arrows for a connection
     *
     * @param sourcePoint The source point of the connection
     * @param destPoint The destination point of the connection
     * @param color The color of the arrows
     * @param zoomLevel The current zoom level
     */
    private fun DrawScope.drawArrows(
        sourcePoint: Offset,
        destPoint: Offset,
        color: Color,
        zoomLevel: Float
    ) {
        val arrowLength = CoordinateConverter.scaleWithZoom(20f, zoomLevel)
        val arrowAngle = (kotlin.math.PI / 6).toFloat()

        // Calculate angles
        val angleToDestination = connectionManager.calculateAngle(sourcePoint, destPoint)
        val angleToSource = connectionManager.calculateAngle(destPoint, sourcePoint)

        // Draw destination arrow
        val (destArrowPoint1, destArrowPoint2) = connectionManager.calculateArrowPoints(
            destPoint, angleToDestination, arrowLength, arrowAngle
        )
        val strokeWidth = CoordinateConverter.scaleWithZoom(2f, zoomLevel)
        drawLine(color = color, start = destPoint, end = destArrowPoint1, strokeWidth = strokeWidth)
        drawLine(color = color, start = destPoint, end = destArrowPoint2, strokeWidth = strokeWidth)

        // Draw source arrow
        val (sourceArrowPoint1, sourceArrowPoint2) = connectionManager.calculateArrowPoints(
            sourcePoint, angleToSource, arrowLength, arrowAngle
        )
        drawLine(color = color, start = sourcePoint, end = sourceArrowPoint1, strokeWidth = strokeWidth)
        drawLine(color = color, start = sourcePoint, end = sourceArrowPoint2, strokeWidth = strokeWidth)
    }

    /**
     * Draws a connection preview during dragging
     *
     * @param drawScope The DrawScope to draw in
     * @param sourceRoom The source room
     * @param direction The direction of the connection
     * @param currentPoint The current point of the drag
     * @param sourceCorner The corner for UP/DOWN connections
     * @param zone The zone containing the rooms
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     */
    fun drawConnectionPreview(
        drawScope: DrawScope,
        sourceRoom: Room,
        direction: ExitDirection,
        currentPoint: Offset,
        sourceCorner: String,
        zone: Zone,
        density: Float,
        zoomLevel: Float
    ) {
        val sourceRect = RoomComponent.getRoomRect(sourceRoom, zone, density, zoomLevel)
        val sourcePoint = connectionManager.getConnectionPoint(sourceRoom, sourceRect, direction, sourceCorner)

        // Use the connection color from ConnectionManager, but use Blue for cardinal directions
        // instead of Gray to make the preview more visible
        val previewColor = if (direction in listOf(ExitDirection.UP, ExitDirection.DOWN))
            connectionManager.getConnectionColor(direction)
        else
            Color.Blue

        drawScope.drawLine(
            color = previewColor,
            start = sourcePoint,
            end = currentPoint,
            strokeWidth = CoordinateConverter.scaleWithZoom(2f, zoomLevel),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }
}
