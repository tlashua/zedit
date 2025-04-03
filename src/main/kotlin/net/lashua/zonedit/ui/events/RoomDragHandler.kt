package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * Handler for room dragging
 */
class RoomDragHandler : EventHandler {
    private val log = LoggerFactory.getLogger(RoomDragHandler::class.java)

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        return modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    log.debug("ROOM HANDLER: Drag start at: ({}, {})", offset.x, offset.y)

                    // Check if we're dragging a room
                    val roomToDrag = state.connectionManager.findRoomAtPoint(
                        state.zone, offset, density, zoomLevel
                    )

                    if (roomToDrag != null) {
                        log.debug("Starting room drag: {}", roomToDrag.id)
                        state.startDraggingRoom(roomToDrag.id)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    // Update room position if we're dragging a room
                    if (state.draggedRoomId != null) {
                        // Accumulate the drag amount before snapping
                        val modelDragX = dragAmount.x / zoomLevel
                        val modelDragY = dragAmount.y / zoomLevel

                        val room = state.zone.rooms.first { it.id == state.draggedRoomId }
                        val oldPos = room.position

                        // Calculate new position first without snapping
                        // Allow dragging beyond the visible canvas area, only constrain to prevent negative positions
                        val rawX = (oldPos.x + modelDragX).coerceAtLeast(0f)
                        val rawY = (oldPos.y + modelDragY).coerceAtLeast(0f)

                        log.debug(
                            "Drag position - Old: ({}, {}), New: ({}, {}), Node: {}x{}, Zoom: {}",
                            oldPos.x, oldPos.y,
                            rawX, rawY,
                            state.zone.nodeWidthDp, state.zone.nodeHeightDp,
                            zoomLevel
                        )

                        // During drag - no snapping at all while actively dragging
                        val newPos = Position(rawX, rawY)

                        state.updateLastPosition(newPos)
                        state.updateRoomPosition(state.draggedRoomId!!, newPos)
                    }
                },
                onDragEnd = {
                    log.debug("=== ROOM HANDLER: Drag End ===")
                    log.debug("Final dragged room: {}", state.draggedRoomId)

                    // Stop room dragging
                    if (state.draggedRoomId != null) {
                        state.stopDragging()
                    }
                }
            )
        }
    }
}
