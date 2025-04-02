package net.lashua.zonedit.ui.events

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.ui.state.ZoneCanvasState
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
        return modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    // Only handle taps if we're not in the middle of dragging
                    if (state.draggedRoomId == null && state.connectionDragState == null) {
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
