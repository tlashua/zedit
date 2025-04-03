package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.ui.state.ZoneCanvasState
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory

/**
 * Handler for room selection
 */
class RoomSelectionHandler : EventHandler {
    private val log = LoggerFactory.getLogger(RoomSelectionHandler::class.java)

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        log.debug("RoomSelectionHandler.applyTo called with zoomLevel={}, density={}", zoomLevel, density)
        return modifier.pointerInput(zoomLevel) {
            detectTapGestures(
                onTap = { offset ->
                    log.debug("onTap at ({}, {}) with zoomLevel={}, density={}", offset.x, offset.y, zoomLevel, density)
                    // Only handle taps if we're not in the middle of dragging
                    if (state.draggedRoomId == null && state.connectionDragState == null) {
                        // Log all room positions for debugging
                        state.zone.rooms.forEach { room ->
                            val roomRect = CoordinateConverter.getRoomRect(room, state.zone, density, zoomLevel)
                            log.debug("Room {}: position=({}, {}), rect=({}, {}, {}, {})",
                                room.id, room.position.x, room.position.y,
                                roomRect.left, roomRect.top, roomRect.right, roomRect.bottom)
                        }

                        val clickedRoom = state.connectionManager.findRoomAtPoint(
                            state.zone, offset, density, zoomLevel
                        )
                        log.debug("Room clicked: {}", clickedRoom?.id)
                        state.updateSelectedRoom(clickedRoom)
                    }
                }
            )
        }
    }
}
