package net.lashua.zonedit.ui.events

import androidx.compose.ui.Modifier
import net.lashua.zonedit.ui.state.ZoneCanvasState

/**
 * Base interface for all event handlers
 */
interface EventHandler {
    /**
     * Apply this event handler to a modifier
     *
     * @param state The ZoneCanvasState to use
     * @param modifier The modifier to apply the event handler to
     * @param density The screen density factor
     * @param zoomLevel The current zoom level
     * @return The modified modifier
     */
    fun applyTo(
        state: ZoneCanvasState,
        modifier: Modifier,
        density: Float,
        zoomLevel: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): Modifier
}
