package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private val log = LoggerFactory.getLogger("net.lashua.zonedit.model.ConnectionManager")

data class ConnectionPoint(
    val position: Offset,
    val room: Room,
    val direction: ExitDirection,
    val corner: String? = null  // Only used for UP/DOWN connections
)

data class Connection(
    val source: ConnectionPoint,
    val destination: ConnectionPoint
)

class ConnectionManager {
    /**
     * Converts model coordinates to screen coordinates and creates a Rect for a room
     *
     * @param room The room to get the rectangle for
     * @param zone The zone containing the room
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return A Rect representing the room's position and size on screen
     */
    fun getRoomRect(room: Room, zone: Zone, density: Float, zoomLevel: Float): Rect {
        // Use the CoordinateConverter to ensure consistent coordinate conversion
        return CoordinateConverter.getRoomRect(room, zone, density, zoomLevel)
    }

    /**
     * Gets all connection points for a room
     *
     * @param room The room to get connection points for
     * @param rect The screen rectangle of the room
     * @return A list of ConnectionPoint objects
     */
    fun getConnectionPoints(room: Room, rect: Rect): List<ConnectionPoint> {
        return buildList {
            // Cardinal directions (N,S,E,W)
            add(ConnectionPoint(
                Offset(rect.center.x, rect.top),
                room,
                ExitDirection.NORTH
            ))
            add(ConnectionPoint(
                Offset(rect.center.x, rect.bottom),
                room,
                ExitDirection.SOUTH
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.center.y),
                room,
                ExitDirection.EAST
            ))
            add(ConnectionPoint(
                Offset(rect.left, rect.center.y),
                room,
                ExitDirection.WEST
            ))

            // UP points (regardless of whether UP exit exists)
            add(ConnectionPoint(
                Offset(rect.left, rect.top),
                room,
                ExitDirection.UP,
                "LEFT"
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.top),
                room,
                ExitDirection.UP,
                "RIGHT"
            ))

            // DOWN points (regardless of whether DOWN exit exists)
            add(ConnectionPoint(
                Offset(rect.left, rect.bottom),
                room,
                ExitDirection.DOWN,
                "LEFT"
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.bottom),
                room,
                ExitDirection.DOWN,
                "RIGHT"
            ))
        }
    }

    /**
     * Checks if a point is near a specific target point
     *
     * @param point The point to check
     * @param target The target point
     * @param threshold The maximum distance to consider "near"
     * @return True if the point is within the threshold distance of the target
     */
    fun isNearPoint(point: Offset, target: Offset, threshold: Float): Boolean {
        val distance = sqrt(
            (point.x - target.x).pow(2) + (point.y - target.y).pow(2)
        )
        val result = distance <= threshold

        log.debug("isNearPoint: point=({}, {}), target=({}, {}), distance={}, threshold={}, result={}",
            point.x, point.y, target.x, target.y, distance, threshold, result)

        return result
    }

    /**
     * Checks if a point is near a line segment
     *
     * @param point The point to check
     * @param start The start point of the line
     * @param end The end point of the line
     * @param threshold The maximum distance to consider "near"
     * @return True if the point is within the threshold distance of the line
     */
    fun isNearLine(point: Offset, start: Offset, end: Offset, threshold: Float): Boolean {
        // If the line is very short, increase the hit area
        val lineLength = sqrt(
            (end.x - start.x).pow(2) + (end.y - start.y).pow(2)
        )

        // For very short lines, use a circular hit area
        if (lineLength < threshold * 2) {
            val centerPoint = Offset(
                (start.x + end.x) / 2,
                (start.y + end.y) / 2
            )
            val distanceToCenter = sqrt(
                (point.x - centerPoint.x).pow(2) + (point.y - centerPoint.y).pow(2)
            )
            return distanceToCenter <= threshold
        }

        // For longer lines, use the point-to-line distance formula
        val numerator = abs(
            (end.y - start.y) * point.x -
            (end.x - start.x) * point.y +
            end.x * start.y -
            end.y * start.x
        )

        val denominator = sqrt(
            (end.y - start.y).pow(2) + (end.x - start.x).pow(2)
        )

        // Calculate the projection point
        val t = ((point.x - start.x) * (end.x - start.x) +
                (point.y - start.y) * (end.y - start.y)) /
                (lineLength * lineLength)

        // Check if the projection point lies outside the line segment
        return if (t < 0.0f || t > 1.0f) {
            // If outside, use distance to nearest endpoint
            val distToStart = sqrt(
                (point.x - start.x).pow(2) + (point.y - start.y).pow(2)
            )
            val distToEnd = sqrt(
                (point.x - end.x).pow(2) + (point.y - end.y).pow(2)
            )
            minOf(distToStart, distToEnd) <= threshold
        } else {
            // If inside, use perpendicular distance
            numerator / denominator <= threshold
        }
    }

    /**
     * Gets a connection between two rooms if it exists
     *
     * @param source The source room
     * @param direction The direction from source to destination
     * @param destination The destination room
     * @return A Connection object if the connection exists, null otherwise
     */
    fun getConnection(source: Room, direction: ExitDirection, destination: Room): Connection? {
        val destId = source.exits[direction] ?: return null
        if (destination.id != destId) return null

        val oppositeDir = getOppositeDirection(direction)

        // Verify bi-directional connection
        if (destination.exits[oppositeDir] != source.id) {
            log.warn("Invalid connection state: {} -> {} is not bi-directional", source.id, destination.id)
            return null
        }

        return Connection(
            source = ConnectionPoint(
                position = Offset.Zero, // Will be set by drawing code
                room = source,
                direction = direction,
                corner = source.exitCorners[direction]
            ),
            destination = ConnectionPoint(
                position = Offset.Zero, // Will be set by drawing code
                room = destination,
                direction = oppositeDir,
                corner = destination.exitCorners[oppositeDir]
            )
        )
    }

    /**
     * Gets the opposite direction
     *
     * @param direction The direction to get the opposite of
     * @return The opposite direction
     */
    fun getOppositeDirection(direction: ExitDirection): ExitDirection {
        return when (direction) {
            ExitDirection.NORTH -> ExitDirection.SOUTH
            ExitDirection.SOUTH -> ExitDirection.NORTH
            ExitDirection.EAST -> ExitDirection.WEST
            ExitDirection.WEST -> ExitDirection.EAST
            ExitDirection.UP -> ExitDirection.DOWN
            ExitDirection.DOWN -> ExitDirection.UP
        }
    }

    /**
     * Determines if a connection should be drawn based on room positions
     * This prevents drawing the same connection twice
     *
     * @param sourceRoom The source room
     * @param direction The direction of the connection
     * @param destRoom The destination room
     * @return True if the connection should be drawn
     */
    fun shouldDrawConnection(sourceRoom: Room, direction: ExitDirection, destRoom: Room): Boolean {
        return when (direction) {
            ExitDirection.NORTH, ExitDirection.SOUTH ->
                sourceRoom.position.y >= destRoom.position.y
            ExitDirection.EAST, ExitDirection.WEST,
            ExitDirection.UP, ExitDirection.DOWN ->
                sourceRoom.id <= destRoom.id
        }
    }

    /**
     * Gets the connection points for a connection between two rooms
     *
     * @param direction The direction of the connection
     * @param sourceRect The screen rectangle of the source room
     * @param destRect The screen rectangle of the destination room
     * @param sourceRoom The source room
     * @param destRoom The destination room
     * @return A pair of Offset objects representing the start and end points of the connection
     */
    fun getConnectionPoints(
        direction: ExitDirection,
        sourceRect: Rect,
        destRect: Rect,
        sourceRoom: Room,
        destRoom: Room
    ): Pair<Offset, Offset> {
        return when (direction) {
            ExitDirection.NORTH -> Pair(
                Offset(sourceRect.center.x, sourceRect.top),
                Offset(destRect.center.x, destRect.bottom)
            )
            ExitDirection.SOUTH -> Pair(
                Offset(sourceRect.center.x, sourceRect.bottom),
                Offset(destRect.center.x, destRect.top)
            )
            ExitDirection.EAST -> Pair(
                Offset(sourceRect.right, sourceRect.center.y),
                Offset(destRect.left, destRect.center.y)
            )
            ExitDirection.WEST -> Pair(
                Offset(sourceRect.left, sourceRect.center.y),
                Offset(destRect.right, destRect.center.y)
            )
            ExitDirection.UP, ExitDirection.DOWN -> {
                val srcCorner = sourceRoom.exitCorners[direction] ?: "RIGHT"
                val oppositeDir = if (direction == ExitDirection.UP) ExitDirection.DOWN else ExitDirection.UP
                val destCorner = destRoom.exitCorners[oppositeDir] ?: "LEFT"

                val srcPoint = when {
                    direction == ExitDirection.DOWN && srcCorner == "LEFT" ->
                        Offset(sourceRect.left, sourceRect.bottom)
                    direction == ExitDirection.DOWN ->
                        Offset(sourceRect.right, sourceRect.bottom)
                    srcCorner == "LEFT" ->
                        Offset(sourceRect.left, sourceRect.top)
                    else ->
                        Offset(sourceRect.right, sourceRect.top)
                }

                val dstPoint = when {
                    oppositeDir == ExitDirection.DOWN && destCorner == "LEFT" ->
                        Offset(destRect.left, destRect.bottom)
                    oppositeDir == ExitDirection.DOWN ->
                        Offset(destRect.right, destRect.bottom)
                    destCorner == "LEFT" ->
                        Offset(destRect.left, destRect.top)
                    else ->
                        Offset(destRect.right, destRect.top)
                }
                Pair(srcPoint, dstPoint)
            }
        }
    }

    /**
     * Gets the connection point for a specific direction on a room
     *
     * @param room The room
     * @param rect The screen rectangle of the room
     * @param direction The direction
     * @param corner The corner (for UP/DOWN directions)
     * @return An Offset representing the connection point
     */
    fun getConnectionPoint(
        room: Room,
        rect: Rect,
        direction: ExitDirection,
        corner: String? = null
    ): Offset {
        return when (direction) {
            ExitDirection.NORTH -> Offset(rect.center.x, rect.top)
            ExitDirection.SOUTH -> Offset(rect.center.x, rect.bottom)
            ExitDirection.EAST -> Offset(rect.right, rect.center.y)
            ExitDirection.WEST -> Offset(rect.left, rect.center.y)
            ExitDirection.UP -> {
                val actualCorner = corner ?: room.exitCorners[direction] ?: "RIGHT"
                if (actualCorner == "LEFT") Offset(rect.left, rect.top) else Offset(rect.right, rect.top)
            }
            ExitDirection.DOWN -> {
                val actualCorner = corner ?: room.exitCorners[direction] ?: "RIGHT"
                if (actualCorner == "LEFT") Offset(rect.left, rect.bottom) else Offset(rect.right, rect.bottom)
            }
        }
    }

    /**
     * Creates a bi-directional connection between two rooms
     *
     * @param source The source room
     * @param direction The direction from source to destination
     * @param destination The destination room
     * @param sourceCorner The corner for UP/DOWN connections
     * @return A pair of updated rooms
     */
    fun createConnection(source: Room, direction: ExitDirection, destination: Room, sourceCorner: String? = null): Pair<Room, Room> {
        val oppositeDir = getOppositeDirection(direction)

        val updatedSource = source.copy(
            exits = source.exits + (direction to destination.id),
            exitCorners = when (direction) {
                ExitDirection.UP, ExitDirection.DOWN -> source.exitCorners + (direction to (sourceCorner ?: "RIGHT"))
                else -> source.exitCorners
            }
        )

        val destCorner = if (sourceCorner == "LEFT") "RIGHT" else "LEFT"
        val updatedDest = destination.copy(
            exits = destination.exits + (oppositeDir to source.id),
            exitCorners = when (oppositeDir) {
                ExitDirection.UP, ExitDirection.DOWN -> destination.exitCorners + (oppositeDir to destCorner)
                else -> destination.exitCorners
            }
        )

        return Pair(updatedSource, updatedDest)
    }

    /**
     * Removes a bi-directional connection between two rooms
     *
     * @param source The source room
     * @param direction The direction from source to destination
     * @param zone The zone containing the rooms
     * @return A pair of updated rooms, or null if the connection doesn't exist
     */
    fun removeConnection(source: Room, direction: ExitDirection, zone: Zone): Pair<Room, Room>? {
        log.debug("removeConnection called - source: {}, direction: {}", source.id, direction)

        val destId = source.exits[direction]
        if (destId == null) {
            log.warn("No destination found for direction {} in room {}", direction, source.id)
            return null
        }

        val destination = zone.rooms.find { it.id == destId }
        if (destination == null) {
            log.warn("Destination room {} not found in zone", destId)
            return null
        }

        val oppositeDir = getOppositeDirection(direction)
        log.debug("Removing bi-directional connection {} <-[{}/{}]-> {}",
            source.id, direction, oppositeDir, destination.id)

        val updatedSource = source.copy(
            exits = source.exits - direction,
            exitCorners = source.exitCorners - direction
        )

        val updatedDest = destination.copy(
            exits = destination.exits - oppositeDir,
            exitCorners = destination.exitCorners - oppositeDir
        )

        log.debug("Connection removal complete")
        return Pair(updatedSource, updatedDest)
    }

    /**
     * Gets the color for a connection based on its direction
     *
     * @param direction The direction of the connection
     * @return The color to use for the connection
     */
    fun getConnectionColor(direction: ExitDirection): Color {
        return when (direction) {
            ExitDirection.UP, ExitDirection.DOWN -> Color.Green
            else -> Color.Gray
        }
    }

    /**
     * Calculates the angle between two points
     *
     * @param start The start point
     * @param end The end point
     * @return The angle in radians
     */
    fun calculateAngle(start: Offset, end: Offset): Float {
        return kotlin.math.atan2(
            (end.y - start.y),
            (end.x - start.x)
        )
    }

    /**
     * Calculates the points for an arrow at the given position and angle
     *
     * @param point The point where the arrow is located
     * @param angle The angle of the arrow in radians
     * @param length The length of the arrow
     * @param arrowAngle The angle between the arrow lines and the main direction
     * @return A pair of points representing the two sides of the arrow
     */
    fun calculateArrowPoints(point: Offset, angle: Float, length: Float, arrowAngle: Float): Pair<Offset, Offset> {
        val arrowPoint1 = Offset(
            point.x - length * kotlin.math.cos(angle - arrowAngle),
            point.y - length * kotlin.math.sin(angle - arrowAngle)
        )
        val arrowPoint2 = Offset(
            point.x - length * kotlin.math.cos(angle + arrowAngle),
            point.y - length * kotlin.math.sin(angle + arrowAngle)
        )

        return Pair(arrowPoint1, arrowPoint2)
    }

    /**
     * Finds the closest connection point to a given offset
     *
     * @param room The room to check connection points for
     * @param rect The screen rectangle of the room
     * @param point The point to check against
     * @param threshold The maximum distance to consider "near"
     * @return The closest ConnectionPoint if within threshold, null otherwise
     */
    fun findClosestConnectionPoint(room: Room, rect: Rect, point: Offset, threshold: Float): ConnectionPoint? {
        val connectionPoints = getConnectionPoints(room, rect)

        // Find the closest connection point to the offset
        val closestPoint = connectionPoints.minByOrNull { connectionPoint ->
            val distance = sqrt(
                (point.x - connectionPoint.position.x).pow(2) +
                (point.y - connectionPoint.position.y).pow(2)
            )
            distance
        }

        // Check if we're close enough to the closest point
        val result = if (closestPoint != null && isNearPoint(point, closestPoint.position, threshold)) {
            closestPoint
        } else {
            null
        }

        log.debug("findClosestConnectionPoint for room {}: point=({}, {}), closest={}, threshold={}, result={}",
            room.id, point.x, point.y, closestPoint?.position, threshold, result?.direction)

        return result
    }

    /**
     * Finds a connection near a given point
     *
     * @param zone The zone containing the rooms
     * @param point The point to check against
     * @param threshold The maximum distance to consider "near"
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return A Triple of (sourceRoom, direction, destRoom) if a connection is found, null otherwise
     */
    fun findConnectionNearPoint(
        zone: Zone,
        point: Offset,
        threshold: Float,
        density: Float,
        zoomLevel: Float
    ): Triple<Room, ExitDirection, Room>? {
        for (room in zone.rooms) {
            val sourceRect = getRoomRect(room, zone, density, zoomLevel)

            for ((exitDir, destId) in room.exits) {
                val destRoom = zone.rooms.find { it.id == destId } ?: continue

                // Only check each connection once
                if (shouldDrawConnection(room, exitDir, destRoom)) {
                    val destRect = getRoomRect(destRoom, zone, density, zoomLevel)

                    // Get source and destination points
                    val (sourcePoint, destPoint) = getConnectionPoints(
                        exitDir, sourceRect, destRect, room, destRoom
                    )

                    // Adjust threshold based on connection type
                    val hitDetectionDistance = when (exitDir) {
                        ExitDirection.UP, ExitDirection.DOWN -> threshold * 1.25f
                        else -> threshold
                    }

                    if (isNearLine(point, sourcePoint, destPoint, hitDetectionDistance)) {
                        return Triple(room, exitDir, destRoom)
                    }
                }
            }
        }

        return null
    }

    /**
     * Draws a connection preview line
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
        val sourceRect = getRoomRect(sourceRoom, zone, density, zoomLevel)
        val sourcePoint = getConnectionPoint(sourceRoom, sourceRect, direction, sourceCorner)

        // Use the connection color from ConnectionManager, but use Blue for cardinal directions
        // instead of Gray to make the preview more visible
        val previewColor = if (direction in listOf(ExitDirection.UP, ExitDirection.DOWN))
            getConnectionColor(direction)
        else
            Color.Blue

        drawScope.drawLine(
            color = previewColor,
            start = sourcePoint,
            end = currentPoint,
            strokeWidth = 2f * zoomLevel,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }

    /**
     * Finds a room at the given point
     *
     * @param zone The zone containing the rooms
     * @param point The point to check
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The room at the point, or null if no room is found
     */
    fun findRoomAtPoint(zone: Zone, point: Offset, density: Float, zoomLevel: Float): Room? {
        log.debug("findRoomAtPoint: point=({}, {}) with density={}, zoom={}",
            point.x, point.y, density, zoomLevel)

        val result = zone.rooms.firstOrNull { room ->
            val roomRect = CoordinateConverter.getRoomRect(room, zone, density, zoomLevel)
            val contains = CoordinateConverter.containsPoint(roomRect, point, 2f)
            log.debug("  checking room {}: rect=({}, {}, {}, {}), contains={}",
                room.id, roomRect.left, roomRect.top, roomRect.right, roomRect.bottom, contains)
            contains
        }

        log.debug("findRoomAtPoint result: {}", result?.id)
        return result
    }
}
