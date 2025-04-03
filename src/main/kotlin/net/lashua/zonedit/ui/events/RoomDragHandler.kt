package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.ui.state.ZoneCanvasState
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory

/**
 * Handler for room dragging
 */
class RoomDragHandler : AbstractDragHandler() {
    companion object {
        private val LOG = LoggerFactory.getLogger(RoomDragHandler::class.java)
    }

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        return modifier.pointerInput(zoomLevel) {
            detectDragGestures(
                onDragStart = { offset ->
                    LOG.debug("ROOM HANDLER: Drag start at: ({}, {}) with zoomLevel={}, density={}",
                        offset.x, offset.y, zoomLevel, density)

                    // Check if a connection drag has already been started
                    if (state.connectionDragState != null) {
                        LOG.debug("Connection drag already started, not starting room drag")
                        return@detectDragGestures
                    }

                    // Log all room positions for debugging
                    logRoomPositions(state, density, zoomLevel)

                    // First check if we're clicking on a connection point
                    if (state.selectedRoom != null) {
                        // Don't try to start a connection drag here, let the ConnectionDragHandler handle it
                        // Just check if we're clicking on a connection point
                        val roomRect = CoordinateConverter.getRoomRect(state.selectedRoom!!, state.zone, density, zoomLevel)
                        val connectionPointSize = CoordinateConverter.scaleWithZoom(12f * density, zoomLevel)
                        val closestPoint = state.connectionManager.findClosestConnectionPoint(
                            state.selectedRoom!!, roomRect, offset, connectionPointSize
                        )

                        if (closestPoint != null) {
                            LOG.debug("Connection point clicked, not dragging room")
                            return@detectDragGestures
                        }

                        LOG.debug("No connection point clicked, trying to drag room")
                        tryStartRoomDrag(state, offset, density, zoomLevel)
                    } else {
                        // No selected room, so just try to drag a room
                        tryStartRoomDrag(state, offset, density, zoomLevel)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    // Check if a connection drag is in progress
                    if (state.connectionDragState != null) {
                        LOG.debug("Connection drag in progress, not updating room position")
                        return@detectDragGestures
                    }

                    // Update room position if we're dragging a room
                    if (state.draggedRoomId != null) {
                        // Convert screen drag amount to model drag amount using CoordinateConverter
                        val modelDrag = CoordinateConverter.screenDragToModelDrag(dragAmount, density, zoomLevel)

                        val room = state.zone.rooms.first { it.id == state.draggedRoomId }
                        val oldPos = room.position

                        // Calculate new position first without snapping
                        // Allow dragging beyond the visible canvas area, only constrain to prevent negative positions
                        val rawX = (oldPos.x + modelDrag.x).coerceAtLeast(0f)
                        val rawY = (oldPos.y + modelDrag.y).coerceAtLeast(0f)

                        LOG.debug(
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
                    LOG.debug("=== ROOM HANDLER: Drag End ===")
                    LOG.debug("Final dragged room: {}", state.draggedRoomId)

                    // Check if a connection drag is in progress
                    if (state.connectionDragState != null) {
                        LOG.debug("Connection drag in progress, not stopping room drag")
                        return@detectDragGestures
                    }

                    // Stop room dragging
                    if (state.draggedRoomId != null) {
                        state.stopDragging()
                    }
                }
            )
        }
    }
}
