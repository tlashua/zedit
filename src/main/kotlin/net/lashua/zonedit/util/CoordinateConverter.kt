package net.lashua.zonedit.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import org.slf4j.LoggerFactory

/**
 * Utility class for converting between different coordinate systems.
 *
 * The application uses two primary coordinate systems:
 * 1. Model coordinates (dp): Used for storing positions in the data model
 * 2. Screen coordinates (px): Used for rendering and input handling
 *
 * This class provides methods to convert between these coordinate systems,
 * ensuring consistent application of density and zoom level.
 */
object CoordinateConverter {
    private val log = LoggerFactory.getLogger(CoordinateConverter::class.java)
    /**
     * Converts model coordinates (dp) to screen coordinates (px)
     *
     * @param position The position in model coordinates (dp)
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The position in screen coordinates (px)
     */
    fun modelToScreen(position: Position, density: Float, zoomLevel: Float): Offset {
        val screenX = position.x * density * zoomLevel
        val screenY = position.y * density * zoomLevel

        log.debug("modelToScreen: ({}, {}) -> ({}, {}) with density={}, zoom={}",
            position.x, position.y, screenX, screenY, density, zoomLevel)

        return Offset(x = screenX, y = screenY)
    }

    /**
     * Converts screen coordinates (px) to model coordinates (dp)
     *
     * @param offset The position in screen coordinates (px)
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The position in model coordinates (dp)
     */
    fun screenToModel(offset: Offset, density: Float, zoomLevel: Float): Position {
        val modelX = offset.x / (density * zoomLevel)
        val modelY = offset.y / (density * zoomLevel)

        log.debug("screenToModel: ({}, {}) -> ({}, {}) with density={}, zoom={}",
            offset.x, offset.y, modelX, modelY, density, zoomLevel)

        return Position(x = modelX, y = modelY)
    }

    /**
     * Converts a screen drag amount (px) to a model drag amount (dp)
     *
     * @param dragAmount The drag amount in screen coordinates (px)
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The drag amount in model coordinates (dp)
     */
    fun screenDragToModelDrag(dragAmount: Offset, density: Float, zoomLevel: Float): Offset {
        val modelDragX = dragAmount.x / (density * zoomLevel)
        val modelDragY = dragAmount.y / (density * zoomLevel)

        log.debug("screenDragToModelDrag: ({}, {}) -> ({}, {}) with density={}, zoom={}",
            dragAmount.x, dragAmount.y, modelDragX, modelDragY, density, zoomLevel)

        return Offset(x = modelDragX, y = modelDragY)
    }

    /**
     * Creates a screen rectangle (px) for a room based on its model position (dp)
     *
     * @param room The room to get the rectangle for
     * @param zone The zone containing the room
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return A Rect representing the room's position and size on screen (px)
     */
    fun getRoomRect(room: Room, zone: Zone, density: Float, zoomLevel: Float): Rect {
        // Convert model coordinates (dp) to screen coordinates (px)
        val screenPos = modelToScreen(room.position, density, zoomLevel)

        // Convert node dimensions from dp to px
        val width = zone.nodeWidthDp * density * zoomLevel
        val height = zone.nodeHeightDp * density * zoomLevel

        val rect = Rect(
            offset = screenPos,
            size = Size(width, height)
        )

        log.debug("getRoomRect for room {}: position=({}, {}), rect=({}, {}, {}, {}) with density={}, zoom={}",
            room.id, room.position.x, room.position.y,
            rect.left, rect.top, rect.right, rect.bottom,
            density, zoomLevel)

        return rect
    }

    /**
     * Scales a size value based on zoom level
     *
     * @param size The size to scale
     * @param zoomLevel The current zoom level
     * @return The scaled size
     */
    fun scaleWithZoom(size: Float, zoomLevel: Float): Float {
        val scaledSize = size * zoomLevel
        log.trace("scaleWithZoom: {} -> {} with zoom={}", size, scaledSize, zoomLevel)
        return scaledSize
    }

    /**
     * Checks if a point is contained within a rectangle with a small tolerance
     * to account for floating point precision issues
     *
     * @param rect The rectangle to check
     * @param point The point to check
     * @param tolerance Optional tolerance value (default: 0.5f)
     * @return True if the point is contained within the rectangle
     */
    fun containsPoint(rect: Rect, point: Offset, tolerance: Float = 0.5f): Boolean {
        val result = point.x >= (rect.left - tolerance) &&
                point.x <= (rect.right + tolerance) &&
                point.y >= (rect.top - tolerance) &&
                point.y <= (rect.bottom + tolerance)

        log.debug("containsPoint: point=({}, {}), rect=({}, {}, {}, {}), tolerance={}, result={}",
            point.x, point.y, rect.left, rect.top, rect.right, rect.bottom, tolerance, result)

        return result
    }
}
