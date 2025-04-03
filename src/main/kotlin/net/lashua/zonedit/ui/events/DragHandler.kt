package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.ui.components.RoomComponent
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * Combined handler for both connection and room dragging
 */
class DragHandler : EventHandler {
    private val log = LoggerFactory.getLogger(DragHandler::class.java)

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
                    log.debug("Drag start at: ({}, {})", offset.x, offset.y)

                    // First, check if we're starting a connection drag (higher priority)
                    if (state.selectedRoom != null) {
                        val connectionStarted = tryStartConnectionDrag(state, offset, density, zoomLevel)

                        // Only try to drag room if we're not starting a connection drag
                        if (!connectionStarted) {
                            tryStartRoomDrag(state, offset, density, zoomLevel)
                        }
                    } else {
                        // No selected room, so just try to drag a room
                        tryStartRoomDrag(state, offset, density, zoomLevel)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    // Handle connection dragging
                    if (state.connectionDragState != null) {
                        state.updateConnectionDragPoint(state.connectionDragState!!.currentPoint + dragAmount)
                    }
                    // Handle room dragging
                    else if (state.draggedRoomId != null) {
                        // Accumulate the drag amount before snapping
                        // Convert screen drag amount (px) to model coordinates (dp)
                        val modelDragX = dragAmount.x / (density * zoomLevel)
                        val modelDragY = dragAmount.y / (density * zoomLevel)

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
                    log.debug("=== Drag End ===")

                    // Finalize connection drag
                    if (state.connectionDragState != null) {
                        log.debug("Finalizing connection drag: sourceRoom={}, direction={}, corner={}",
                            state.connectionDragState?.sourceRoomId,
                            state.connectionDragState?.direction,
                            state.connectionDragState?.sourceCorner)

                        state.finalizeConnectionDrag(
                            state.connectionDragState!!.currentPoint,
                            density,
                            zoomLevel,
                            canvasWidthDp,
                            canvasHeightDp
                        )
                    }

                    // Log room drag end
                    if (state.draggedRoomId != null) {
                        log.debug("Finalizing room drag: {}", state.draggedRoomId)
                    }

                    // Stop all dragging operations
                    state.stopDragging()
                }
            )
        }
    }

    /**
     * Try to start a connection drag
     *
     * @return true if a connection drag was started, false otherwise
     */
    private fun tryStartConnectionDrag(
        state: ZoneCanvasState,
        offset: Offset,
        density: Float,
        zoomLevel: Float
    ): Boolean {
        val roomRect = RoomComponent.getRoomRect(state.selectedRoom!!, state.zone, density, zoomLevel)
        val connectionPointSize = 12f * density * zoomLevel

        // Let's add more debug logging to see what's happening
        log.debug("Checking connection points for room: {}", state.selectedRoom?.id)
        log.debug("Current exits: {}", state.selectedRoom?.exits)

        // Find the closest connection point using ConnectionManager
        val closestPoint = state.connectionManager.findClosestConnectionPoint(
            state.selectedRoom!!, roomRect, offset, connectionPointSize
        )

        // Determine direction and corner based on the closest point
        val direction = if (closestPoint != null) {
            if (closestPoint.direction in listOf(ExitDirection.UP, ExitDirection.DOWN)) {
                log.debug(
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
            log.debug(
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
            return true
        }

        return false
    }

    private fun tryStartRoomDrag(
        state: ZoneCanvasState,
        offset: Offset,
        density: Float,
        zoomLevel: Float
    ) {
        // We're not starting a connection drag, so check if we're dragging a room
        val roomToDrag = state.connectionManager.findRoomAtPoint(
            state.zone, offset, density, zoomLevel
        )

        if (roomToDrag != null) {
            log.debug("Starting room drag: {}", roomToDrag.id)
            state.startDraggingRoom(roomToDrag.id)
        }
    }
}
