package net.lashua.zonedit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import net.lashua.zonedit.ui.graph.GraphCanvas
import net.lashua.zonedit.viewmodel.MudZoneViewModel
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("net.lashua.zonedit.ui.components.GraphCanvasContainer")

/**
 * A container for the GraphCanvas component.
 * Provides rulers and handles the conversion between Zone and MudZone.
 *
 * @param zone The Zone to display
 * @param zoomLevel The zoom level of the canvas
 * @param canvasWidthDp The width of the canvas in dp
 * @param canvasHeightDp The height of the canvas in dp
 * @param selectedRoom The currently selected room, if any
 * @param onZoneChanged Callback for when the zone changes
 * @param onRoomSelected Callback for when a room is selected
 * @param onConnectionStarted Callback for when a connection is started
 * @param onPointerPositionChanged Callback for when the pointer position changes
 * @param modifier The modifier for the container
 */
@Composable
fun GraphCanvasContainer(
    zone: Zone,
    zoomLevel: Float,
    canvasWidthDp: Float,
    canvasHeightDp: Float,
    selectedRoom: Room?,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit,
    onPointerPositionChanged: (Offset?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    log.debug("GraphCanvasContainer: Converting Zone to MudZone. Zone has {} rooms and {} total exits",
        zone.rooms.size,
        zone.rooms.sumOf { it.exits.size }
    )

    // Convert Zone to MudZone
    val mudZone = remember(zone) {
        val result = MudZone(
            name = zone.name,
            nodeWidthDp = zone.nodeWidthDp,
            nodeHeightDp = zone.nodeHeightDp,
            gridSizeDp = zone.gridSizeDp,
            snapToGrid = zone.snapToGrid,
            rooms = zone.rooms.map { room ->
                MudRoom(
                    id = room.id,
                    data = RoomData(
                        name = room.name,
                        description = room.description,
                        position = room.position,
                        flags = room.flags,
                        metadata = room.metadata
                    )
                )
            },
            exits = zone.rooms.flatMap { room ->
                room.exits.map { exit ->
                    MudExit(
                        sourceId = room.id,
                        targetId = exit.destinationId,
                        data = ExitData(
                            direction = exit.direction,
                            corner = exit.corner,
                            flags = emptyList(),
                            metadata = emptyMap()
                        )
                    )
                }
            }
        )

        log.debug("GraphCanvasContainer: Converted to MudZone with {} rooms and {} exits",
            result.rooms.size,
            result.exits.size
        )

        result
    }

    // Create a view model for the MudZone
    val viewModel = remember {
        log.debug("GraphCanvasContainer: Creating new MudZoneViewModel")
        MudZoneViewModel(mudZone)
    }

    // Set the selected room
    LaunchedEffect(selectedRoom) {
        val roomId = selectedRoom?.id
        if (viewModel.selectedRoomId != roomId) {
            log.debug("GraphCanvasContainer: Selecting room {}", roomId)
            viewModel.selectRoom(roomId)
        }
    }

    // Update the view model when the zone changes
    LaunchedEffect(zone) {
        log.debug("GraphCanvasContainer: Updating viewModel.zone with new Zone data")
        viewModel.zone = mudZone
    }

    // Handle room selection changes
    val onRoomSelectionChanged = { roomId: String? ->
        log.debug("GraphCanvasContainer: Room selection changed to {}", roomId)
        val selectedMudRoom = roomId?.let { viewModel.zone.getRoom(it) }
        val selectedZoneRoom = selectedMudRoom?.let { mudRoom ->
            zone.rooms.find { it.id == mudRoom.id }
        }
        onRoomSelected(selectedZoneRoom)
    }

    // Handle zone changes
    val onZoneUpdated = { updatedMudZone: MudZone ->
        log.debug("GraphCanvasContainer: Zone updated. MudZone has {} rooms and {} exits",
            updatedMudZone.rooms.size,
            updatedMudZone.exits.size
        )

        // Convert MudZone back to Zone
        val updatedZone = Zone(
            id = zone.id,
            name = updatedMudZone.name,
            nodeWidthDp = updatedMudZone.nodeWidthDp,
            nodeHeightDp = updatedMudZone.nodeHeightDp,
            gridSizeDp = zone.gridSizeDp,
            snapToGrid = zone.snapToGrid,
            rooms = updatedMudZone.rooms.map { room ->
                // Group exits by source room
                val roomExits = updatedMudZone.exits
                    .filter { it.sourceId == room.id }
                    .map { exit ->
                        Exit(
                            direction = exit.direction,
                            destinationId = exit.targetId,
                            corner = exit.corner
                        )
                    }

                Room(
                    id = room.id,
                    name = room.name,
                    description = room.description,
                    position = room.position,
                    exits = roomExits,
                    flags = room.flags,
                    metadata = room.metadata
                )
            }
        )

        log.debug("GraphCanvasContainer: Converted back to Zone with {} rooms and {} total exits",
            updatedZone.rooms.size,
            updatedZone.rooms.sumOf { it.exits.size }
        )

        onZoneChanged(updatedZone)
    }

    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
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
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, top = 24.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                // Set up effects to handle zone and selection changes
                DisposableEffect(viewModel) {
                    // Create a callback to handle zone changes
                    val zoneChangeCallback = object : (MudZone) -> Unit {
                        override fun invoke(updatedZone: MudZone) {
                            log.debug("GraphCanvasContainer: Zone change callback triggered")
                            onZoneUpdated(updatedZone)
                        }
                    }

                    // Create a callback to handle room selection changes
                    val roomSelectionCallback = object : (String?) -> Unit {
                        override fun invoke(roomId: String?) {
                            log.debug("GraphCanvasContainer: Room selection callback triggered for {}", roomId)
                            onRoomSelectionChanged(roomId)
                        }
                    }

                    // Register the callbacks with the view model
                    viewModel.onZoneChanged = zoneChangeCallback
                    viewModel.onRoomSelected = roomSelectionCallback

                    // Clean up when the effect is disposed
                    onDispose {
                        viewModel.onZoneChanged = null
                        viewModel.onRoomSelected = null
                    }
                }

                // Set up effect to handle pointer position changes
                LaunchedEffect(Unit) {
                    log.debug("GraphCanvasContainer: LaunchedEffect for pointer position triggered")
                    // This would need to be implemented in the GraphCanvas component
                    // For now, we'll just pass null
                    onPointerPositionChanged(null)
                }

                GraphCanvas(
                    viewModel = viewModel,
                    canvasWidthDp = canvasWidthDp.dp,
                    canvasHeightDp = canvasHeightDp.dp,
                    zoomLevel = zoomLevel
                )
            }
        }
    }
}
