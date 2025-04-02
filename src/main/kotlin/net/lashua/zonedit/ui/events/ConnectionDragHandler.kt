package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.ui.components.RoomComponent
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * Handler for connection creation via dragging
 */
class ConnectionDragHandler : EventHandler {
    private val log = LoggerFactory.getLogger(ConnectionDragHandler::class.java)

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
                    
                    // Check if we're starting a connection drag
                    if (state.selectedRoom != null) {
                        tryStartConnectionDrag(state, offset, density, zoomLevel)
                    }
                    
                    // If we're not starting a connection drag, check if we're dragging a room
                    if (state.connectionDragState == null) {
                        tryStartRoomDrag(state, offset, density, zoomLevel)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    
                    // Update connection drag point if we're dragging a connection
                    if (state.connectionDragState != null) {
                        state.updateConnectionDragPoint(state.connectionDragState!!.currentPoint + dragAmount)
                    }
                },
                onDragEnd = {
                    log.debug("=== Connection Drag End ===")
                    log.debug("Final connection state: sourceRoom={}, direction={}, corner={}",
                        state.connectionDragState?.sourceRoomId,
                        state.connectionDragState?.direction,
                        state.connectionDragState?.sourceCorner)
                    
                    // Finalize any connection drag
                    state.connectionDragState?.let {
                        state.finalizeConnectionDrag(
                            it.currentPoint,
                            density,
                            zoomLevel,
                            canvasWidthDp,
                            canvasHeightDp
                        )
                    }
                    
                    // Stop connection dragging
                    if (state.connectionDragState != null) {
                        state.stopDragging()
                    }
                }
            )
        }
    }
    
    private fun tryStartConnectionDrag(
        state: ZoneCanvasState,
        offset: Offset,
        density: Float,
        zoomLevel: Float
    ) {
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
        }
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
