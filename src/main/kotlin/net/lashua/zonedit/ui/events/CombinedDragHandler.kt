package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.ui.state.ZoneCanvasState
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory

/**
 * Combined handler for both connection and room dragging
 */
class CombinedDragHandler : EventHandler {
    companion object {
        private val LOG = LoggerFactory.getLogger(CombinedDragHandler::class.java)
    }

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        LOG.debug("CombinedDragHandler.applyTo called with zoomLevel={}, density={}", zoomLevel, density)
        
        return modifier.pointerInput(zoomLevel) {
            detectDragGestures(
                onDragStart = { offset ->
                    LOG.debug("COMBINED HANDLER: Drag start at: ({}, {}) with zoomLevel={}, density={}",
                        offset.x, offset.y, zoomLevel, density)

                    // Log all room positions for debugging
                    state.zone.rooms.forEach { room ->
                        val roomRect = CoordinateConverter.getRoomRect(room, state.zone, density, zoomLevel)
                        LOG.debug("Room {}: position=({}, {}), rect=({}, {}, {}, {})",
                            room.id, room.position.x, room.position.y,
                            roomRect.left, roomRect.top, roomRect.right, roomRect.bottom)
                    }

                    // First, check if we're starting a connection drag (higher priority)
                    if (state.selectedRoom != null) {
                        val roomRect = CoordinateConverter.getRoomRect(state.selectedRoom!!, state.zone, density, zoomLevel)
                        val connectionPointSize = CoordinateConverter.scaleWithZoom(12f * density, zoomLevel)

                        // Find the closest connection point
                        val closestPoint = state.connectionManager.findClosestConnectionPoint(
                            state.selectedRoom!!, roomRect, offset, connectionPointSize
                        )

                        if (closestPoint != null) {
                            LOG.debug("Connection point detected: direction={}", closestPoint.direction)
                            
                            // Start a connection drag
                            val corner = if (closestPoint.direction in listOf(ExitDirection.UP, ExitDirection.DOWN)) {
                                closestPoint.corner
                            } else {
                                null
                            }
                            
                            LOG.debug("Starting connection drag from {} in direction {} with corner {}",
                                state.selectedRoom?.id, closestPoint.direction, corner)
                                
                            state.startConnectionDrag(
                                state.selectedRoom!!,
                                closestPoint.direction,
                                offset,
                                corner ?: "RIGHT"
                            )
                            return@detectDragGestures
                        }
                    }

                    // If we're not starting a connection drag, check if we're dragging a room
                    val roomToDrag = state.connectionManager.findRoomAtPoint(
                        state.zone, offset, density, zoomLevel
                    )

                    if (roomToDrag != null) {
                        LOG.debug("Starting room drag: {}", roomToDrag.id)
                        state.startDraggingRoom(roomToDrag.id)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    
                    // Handle connection drag
                    if (state.connectionDragState != null) {
                        LOG.debug("Updating connection drag point to {}", change.position)
                        state.updateConnectionDragPoint(change.position)
                        return@detectDragGestures
                    }
                    
                    // Handle room drag
                    if (state.draggedRoomId != null) {
                        // Convert screen drag amount to model drag amount
                        val modelDrag = CoordinateConverter.screenDragToModelDrag(dragAmount, density, zoomLevel)

                        val room = state.zone.rooms.first { it.id == state.draggedRoomId }
                        val oldPos = room.position

                        // Calculate new position without snapping
                        val rawX = (oldPos.x + modelDrag.x).coerceAtLeast(0f)
                        val rawY = (oldPos.y + modelDrag.y).coerceAtLeast(0f)

                        LOG.debug(
                            "Drag position - Old: ({}, {}), New: ({}, {}), Node: {}x{}, Zoom: {}",
                            oldPos.x, oldPos.y,
                            rawX, rawY,
                            state.zone.nodeWidthDp, state.zone.nodeHeightDp,
                            zoomLevel
                        )

                        // During drag - no snapping while actively dragging
                        val newPos = Position(rawX, rawY)
                        state.updateLastPosition(newPos)
                        state.updateRoomPosition(state.draggedRoomId!!, newPos)
                    }
                },
                onDragEnd = {
                    LOG.debug("=== COMBINED HANDLER: Drag End ===")
                    
                    // Handle connection drag end
                    if (state.connectionDragState != null) {
                        LOG.debug("Final connection state: sourceRoom={}, direction={}, corner={}, currentPoint={}",
                            state.connectionDragState?.sourceRoomId,
                            state.connectionDragState?.direction,
                            state.connectionDragState?.sourceCorner,
                            state.connectionDragState?.currentPoint)
                            
                        LOG.debug("Finalizing connection drag with point={}, density={}, zoomLevel={}",
                            state.connectionDragState!!.currentPoint, density, zoomLevel)
                            
                        state.finalizeConnectionDrag(
                            state.connectionDragState!!.currentPoint,
                            density,
                            zoomLevel,
                            canvasWidthDp,
                            canvasHeightDp
                        )
                        
                        LOG.debug("Stopping connection drag")
                        state.stopDragging()
                        return@detectDragGestures
                    }
                    
                    // Handle room drag end
                    LOG.debug("Final dragged room: {}", state.draggedRoomId)
                    
                    if (state.draggedRoomId != null) {
                        LOG.debug("Finalizing room drag: {}", state.draggedRoomId)
                        state.stopDragging()
                    }
                },
                onDragCancel = {
                    LOG.debug("=== COMBINED HANDLER: Drag Cancel ===")
                    
                    if (state.connectionDragState != null || state.draggedRoomId != null) {
                        LOG.debug("Canceling drag")
                        state.stopDragging()
                    }
                }
            )
        }
    }
}
