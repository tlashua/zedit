package net.lashua.zonedit.ui.events

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * Handler for tracking pointer position
 */
class PointerPositionHandler : EventHandler {
    private val log = LoggerFactory.getLogger(PointerPositionHandler::class.java)

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        return modifier.pointerInput(zoomLevel) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val position = event.changes.firstOrNull()?.position

                    // Only update if we have a position and it's within bounds
                    position?.let {
                        log.trace("Pointer position: ({}, {})", it.x, it.y)
                        state.updatePointerPosition(it)
                    }
                }
            }
        }
    }
}
