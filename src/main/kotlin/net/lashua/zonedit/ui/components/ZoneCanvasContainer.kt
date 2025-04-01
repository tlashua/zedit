package net.lashua.zonedit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.ZoneCanvas

@Composable
fun ZoneCanvasContainer(
    zone: Zone,
    zoomLevel: Float,
    canvasWidth: Int,
    canvasHeight: Int,
    selectedRoom: Room?,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(start = 24.dp, top = 24.dp) // Space for rulers
    ) {
        // Horizontal ruler
        Canvas(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            // Draw ruler markings
            // Implementation details for ruler...
        }

        // Vertical ruler
        Canvas(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .align(Alignment.TopStart)
        ) {
            // Draw ruler markings
            // Implementation details for ruler...
        }

        // Main canvas
        ZoneCanvas(
            zone = zone,
            zoomLevel = zoomLevel,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            selectedRoom = selectedRoom,
            onZoneChanged = onZoneChanged,
            onRoomSelected = onRoomSelected,
            onConnectionStarted = onConnectionStarted,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp)
        )
    }
}