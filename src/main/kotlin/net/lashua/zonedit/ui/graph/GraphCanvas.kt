package net.lashua.zonedit.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.ExitData
import net.lashua.zonedit.model.MudZone
import net.lashua.zonedit.model.RoomData
import net.lashua.zonedit.ui.adapters.MudZoneAdapter

/**
 * A canvas for displaying and editing a graph.
 * 
 * @param zone The MudZone to display
 * @param selectedRoomId The ID of the selected room, if any
 * @param canvasWidthDp The width of the canvas in dp
 * @param canvasHeightDp The height of the canvas in dp
 * @param zoomLevel The zoom level of the canvas
 * @param onZoneChanged Callback when the zone changes
 * @param onRoomSelected Callback when a room is selected
 */
@Composable
fun GraphCanvas(
    zone: MudZone,
    selectedRoomId: String? = null,
    canvasWidthDp: Dp = 1000.dp,
    canvasHeightDp: Dp = 800.dp,
    zoomLevel: Float = 1f,
    onZoneChanged: (MudZone) -> Unit = {},
    onRoomSelected: (String?) -> Unit = {}
) {
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()
    
    // Convert the MudZone to a VisualGraph
    val visualGraph = remember(zone, selectedRoomId) {
        MudZoneAdapter.toVisualGraph(zone, selectedRoomId)
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .size(canvasWidthDp, canvasHeightDp)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
        ) {
            // Draw the graph
            GraphRenderer.draw(
                drawScope = this,
                graph = visualGraph,
                textMeasurer = textMeasurer,
                zoomLevel = zoomLevel,
                showConnectionPoints = selectedRoomId != null
            )
        }
    }
}
