package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.ui.components.RoomComponent
import net.lashua.zonedit.ui.state.ZoneCanvasState
import net.lashua.zonedit.util.CoordinateConverter
import org.slf4j.LoggerFactory

/**
 * Handler for connection creation via dragging
 */
class ConnectionDragHandler : AbstractDragHandler() {
    companion object {
        private val LOG = LoggerFactory.getLogger(ConnectionDragHandler::class.java)
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
                    LOG.debug("CONNECTION HANDLER: Drag start at: ({}, {}) with zoomLevel={}, density={}",
                        offset.x, offset.y, zoomLevel, density)

                    // Log all room positions for debugging
                    logRoomPositions(state, density, zoomLevel)

                    // Check if we're starting a connection drag
                    if (state.selectedRoom != null) {
                        val result = tryStartConnectionDrag(state, offset, density, zoomLevel)
                        LOG.debug("tryStartConnectionDrag result: {}", result)

                        // Only try to drag room if no connection point was clicked
                        if (result == -1) {
                            LOG.debug("No connection point clicked, trying to drag room")
                            tryStartRoomDrag(state, offset, density, zoomLevel)
                        } else {
                            LOG.debug("Connection point clicked, not dragging room")
                        }
                        // If result is not -1, a connection point was clicked and a connection drag was started
                        // In this case, we don't want to drag the room
                    } else {
                        // No selected room, so just try to drag a room
                        tryStartRoomDrag(state, offset, density, zoomLevel)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    LOG.debug("CONNECTION HANDLER: onDrag called with dragAmount=({}, {})", dragAmount.x, dragAmount.y)

                    // Update connection drag point if we're dragging a connection
                    if (state.connectionDragState != null) {
                        LOG.debug("Updating connection drag point from {} to {}",
                            state.connectionDragState!!.currentPoint,
                            change.position)
                        // Use the current position instead of adding the drag amount
                        state.updateConnectionDragPoint(change.position)
                    } else {
                        LOG.debug("No connection drag state, not updating connection drag point")
                    }
                },
                onDragCancel = {
                    LOG.debug("=== CONNECTION HANDLER: Drag Cancel ===")
                    if (state.connectionDragState != null) {
                        LOG.debug("Canceling connection drag")
                        state.stopDragging()
                    }
                },
                onDragEnd = {
                    LOG.debug("=== CONNECTION HANDLER: Drag End ===")
                    LOG.debug("Final connection state: sourceRoom={}, direction={}, corner={}, currentPoint={}",
                        state.connectionDragState?.sourceRoomId,
                        state.connectionDragState?.direction,
                        state.connectionDragState?.sourceCorner,
                        state.connectionDragState?.currentPoint)

                    // Finalize any connection drag
                    if (state.connectionDragState != null) {
                        LOG.debug("Finalizing connection drag with point={}, density={}, zoomLevel={}",
                            state.connectionDragState!!.currentPoint, density, zoomLevel)
                        state.finalizeConnectionDrag(
                            state.connectionDragState!!.currentPoint,
                            density,
                            zoomLevel,
                            canvasWidthDp,
                            canvasHeightDp
                        )

                        // Stop connection dragging
                        LOG.debug("Stopping connection drag")
                        state.stopDragging()
                    } else {
                        LOG.debug("No connection drag state to finalize")
                    }
                }
            )
        }
    }


}
