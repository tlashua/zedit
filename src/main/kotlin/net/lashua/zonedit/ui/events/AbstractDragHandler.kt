package net.lashua.zonedit.ui.events

import androidx.compose.ui.geometry.Offset
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.ui.state.ZoneCanvasState
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory

/**
 * Abstract base class for drag handlers with common functionality
 */
abstract class AbstractDragHandler : EventHandler {
    companion object {
        private val LOG = LoggerFactory.getLogger(AbstractDragHandler::class.java)
    }

    // Each subclass should define its own logger

    /**
     * Try to start a connection drag
     *
     * @return 1 if a connection drag was started, 0 if a connection point was clicked but no drag started, -1 if no connection point was clicked
     */
    protected fun tryStartConnectionDrag(
        state: ZoneCanvasState,
        offset: Offset,
        density: Float,
        zoomLevel: Float
    ): Int {
        if (state.selectedRoom == null) return -1

        val roomRect = CoordinateConverter.getRoomRect(state.selectedRoom!!, state.zone, density, zoomLevel)
        val connectionPointSize = CoordinateConverter.scaleWithZoom(12f * density, zoomLevel)

        // Let's add more debug logging to see what's happening
        LOG.debug("Checking connection points for room: {}", state.selectedRoom?.id)
        LOG.debug("Current exits: {}", state.selectedRoom?.exits)

        // Find the closest connection point using ConnectionManager
        val closestPoint = state.connectionManager.findClosestConnectionPoint(
            state.selectedRoom!!, roomRect, offset, connectionPointSize
        )

        // Determine direction and corner based on the closest point
        val direction = if (closestPoint != null) {
            if (closestPoint.direction in listOf(ExitDirection.UP, ExitDirection.DOWN)) {
                LOG.debug(
                    "{} point detected with corner {}",
                    closestPoint.direction,
                    closestPoint.corner
                )
                closestPoint.direction to closestPoint.corner
            } else {
                closestPoint.direction to null
            }
        } else {
            null to null
        }

        // If we found a direction, start a connection drag
        if (direction?.first != null) {
            LOG.debug(
                "Connection point detected: direction={}, corner={}",
                direction.first,
                direction.second
            )

            LOG.debug(
                "Starting connection drag from {} in direction {} with corner {}",
                state.selectedRoom?.id,
                direction.first,
                direction.second
            )
            state.startConnectionDrag(
                state.selectedRoom!!,
                direction.first!!,
                offset,
                direction.second ?: "RIGHT"
            )
            return 1 // Connection drag started
        }

        return -1 // No connection point clicked
    }

    /**
     * Try to start a room drag
     */
    protected fun tryStartRoomDrag(
        state: ZoneCanvasState,
        offset: Offset,
        density: Float,
        zoomLevel: Float
    ) {
        LOG.debug("tryStartRoomDrag called with offset=({}, {}), zoomLevel={}, density={}",
            offset.x, offset.y, zoomLevel, density)

        // Log all room positions for debugging
        state.zone.rooms.forEach { room ->
            val roomRect = CoordinateConverter.getRoomRect(room, state.zone, density, zoomLevel)
            LOG.debug("Room {}: position=({}, {}), rect=({}, {}, {}, {})",
                room.id, room.position.x, room.position.y,
                roomRect.left, roomRect.top, roomRect.right, roomRect.bottom)
        }

        // We're not starting a connection drag, so check if we're dragging a room
        val roomToDrag = state.connectionManager.findRoomAtPoint(
            state.zone, offset, density, zoomLevel
        )

        if (roomToDrag != null) {
            LOG.debug("Starting room drag: {}", roomToDrag.id)
            state.startDraggingRoom(roomToDrag.id)
        }
    }

    /**
     * Log room positions for debugging
     */
    protected fun logRoomPositions(
        state: ZoneCanvasState,
        density: Float,
        zoomLevel: Float
    ) {
        state.zone.rooms.forEach { room ->
            val roomRect = CoordinateConverter.getRoomRect(room, state.zone, density, zoomLevel)
            LOG.debug("Room {}: position=({}, {}), rect=({}, {}, {}, {})",
                room.id, room.position.x, room.position.y,
                roomRect.left, roomRect.top, roomRect.right, roomRect.bottom)
        }
    }
}
