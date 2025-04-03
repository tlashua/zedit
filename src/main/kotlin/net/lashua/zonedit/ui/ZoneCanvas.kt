package net.lashua.zonedit.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import net.lashua.zonedit.ui.components.ConnectionComponent
import net.lashua.zonedit.ui.components.ContextMenuComponent
import net.lashua.zonedit.ui.components.GridComponent
import net.lashua.zonedit.ui.components.RoomComponent
import net.lashua.zonedit.ui.events.CompositeEventHandler
import net.lashua.zonedit.ui.state.rememberZoneCanvasState
import org.slf4j.LoggerFactory

data class ConnectionDragState(
    val sourceRoomId: String,
    val direction: ExitDirection,
    val currentPoint: Offset,
    val sourceCorner: String // "LEFT" or "RIGHT"
)

private val log = LoggerFactory.getLogger("net.lashua.zonedit.ui.ZoneCanvas")

@Composable
fun ZoneCanvas(
    zone: Zone,
    zoomLevel: Float = 1f,
    canvasWidthDp: Dp,
    canvasHeightDp: Dp,
    selectedRoom: Room? = null,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    onPointerPositionChanged: (Offset?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val gridSizeDp = zone.gridSizeDp.dp
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    // Use the state holder to manage all state
    val state = rememberZoneCanvasState(
        zone = zone,
        selectedRoom = selectedRoom,
        onZoneChanged = onZoneChanged,
        onRoomSelected = onRoomSelected,
        onConnectionStarted = onConnectionStarted,
        onPointerPositionChanged = onPointerPositionChanged
    )

    LaunchedEffect(canvasWidthDp, canvasHeightDp, zoomLevel) {
        log.debug("Canvas dimensions updated - width: {}dp, height: {}dp, zoom: {}",
            canvasWidthDp.value, canvasHeightDp.value, zoomLevel)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState)
    ) {
        // Apply all event handlers using the composite handler
        val eventHandler = CompositeEventHandler.default()

        val canvasModifier = Modifier
            .size(canvasWidthDp, canvasHeightDp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

        // Apply all event handlers to the modifier
        val modifierWithEvents = eventHandler.applyTo(
            state = state,
            modifier = canvasModifier,
            density = density,
            zoomLevel = zoomLevel,
            canvasWidthDp = canvasWidthDp.value,
            canvasHeightDp = canvasHeightDp.value
        )

        Canvas(
            modifier = modifierWithEvents
        ) {
            // Draw grid
            GridComponent.draw(this, gridSizeDp, zoomLevel, size)

            // Draw existing connections
            for (room in state.zone.rooms) {
                ConnectionComponent.drawConnections(this, room, state.zone, density, zoomLevel)
            }

            // Draw connection preview if dragging
            state.connectionDragState?.let { dragState ->
                val sourceRoom = state.zone.rooms.find { it.id == dragState.sourceRoomId } ?: return@let

                // Draw connection preview
                ConnectionComponent.drawConnectionPreview(
                    drawScope = this,
                    sourceRoom = sourceRoom,
                    direction = dragState.direction,
                    currentPoint = dragState.currentPoint,
                    sourceCorner = dragState.sourceCorner,
                    zone = state.zone,
                    density = density,
                    zoomLevel = zoomLevel
                )
            }

            // Draw rooms last so they appear on top
            for (room in state.zone.rooms) {
                RoomComponent.draw(
                    drawScope = this,
                    room = room,
                    zone = state.zone,
                    density = density,
                    zoomLevel = zoomLevel,
                    isSelected = room.id == state.selectedRoom?.id,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Context menu for connections
        ContextMenuComponent.ConnectionContextMenu(
            connectionTriple = state.contextMenuConnection,
            position = state.contextMenuPosition,
            onDismiss = { state.hideContextMenu() },
            onDeleteConnection = { sourceRoom, direction ->
                state.removeConnection(sourceRoom, direction)
            }
        )
    }
}


