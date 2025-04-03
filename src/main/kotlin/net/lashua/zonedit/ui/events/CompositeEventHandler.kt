package net.lashua.zonedit.ui.events

import androidx.compose.ui.Modifier
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.slf4j.LoggerFactory

/**
 * A composite event handler that combines multiple event handlers
 */
class CompositeEventHandler(
    private val handlers: List<EventHandler>
) : EventHandler {
    private val log = LoggerFactory.getLogger(CompositeEventHandler::class.java)

    override fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier {
        log.debug("Applying {} event handlers", handlers.size)
        var result = modifier

        // Apply handlers in reverse order so that the last handler in the list gets first chance to handle events
        for (handler in handlers.reversed()) {
            result = handler.applyTo(
                state,
                result,
                density,
                zoomLevel,
                canvasWidthDp,
                canvasHeightDp
            )
        }

        return result
    }

    companion object {
        /**
         * Create a default composite event handler with all the standard handlers
         */
        fun default(): CompositeEventHandler {
            return CompositeEventHandler(
                listOf(
                    // Handlers are applied in reverse order, so the last handler in this list
                    // gets first chance to handle events
                    PointerPositionHandler(),
                    RoomSelectionHandler(),
                    ConnectionContextMenuHandler(),
                    // Use the combined drag handler instead of separate handlers
                    CombinedDragHandler()
                )
            )
        }
    }
}
