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

        for (handler in handlers) {
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
                    PointerPositionHandler(),
                    RoomSelectionHandler(),
                    ConnectionContextMenuHandler(),
                    DragHandler()
                )
            )
        }
    }
}
