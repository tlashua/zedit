package net.lashua.zonedit.ui.events

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * Handler for connection context menus
 */
class ConnectionContextMenuHandler : EventHandler {
    private val log = LoggerFactory.getLogger(ConnectionContextMenuHandler::class.java)

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        return modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (!event.buttons.isSecondaryPressed) {
                        continue
                    }

                    val offset = event.changes.first().position
                    log.debug("Right click detected at coordinates: ({}, {})", offset.x, offset.y)

                    // Use ConnectionManager to find a connection near the click point
                    val hitDetectionThreshold = 80f * density * zoomLevel
                    val connectionTriple = state.connectionManager.findConnectionNearPoint(
                        state.zone, offset, hitDetectionThreshold, density, zoomLevel
                    )

                    if (connectionTriple != null) {
                        val (sourceRoom, direction, destRoom) = connectionTriple
                        log.debug("Hit detected on connection: {} --[{}]--> {}",
                            sourceRoom.id, direction, destRoom.id)

                        state.showContextMenu(
                            connectionTriple,
                            DpOffset(
                                x = (offset.x / density).dp,
                                y = (offset.y / density).dp
                            )
                        )
                    }
                }
            }
        }
    }
}
