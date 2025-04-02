package net.lashua.zonedit.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import net.lashua.zonedit.model.ConnectionManager
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.ConnectionDragState
import org.slf4j.LoggerFactory

/**
 * State holder for the ZoneCanvas component.
 * Encapsulates all state variables and provides methods for updating them.
 */
class ZoneCanvasState(
    initialZone: Zone,
    initialSelectedRoom: Room?,
    private val onZoneChanged: (Zone) -> Unit,
    private val onRoomSelected: (Room?) -> Unit,
    private val onConnectionStarted: (Room, ExitDirection) -> Unit,
    private val onPointerPositionChanged: (Offset?) -> Unit
) {
    private val log = LoggerFactory.getLogger(ZoneCanvasState::class.java)
    val connectionManager = ConnectionManager()

    // State variables
    var zone by mutableStateOf(initialZone)
        private set

    var selectedRoom by mutableStateOf(initialSelectedRoom)
        private set

    var draggedRoomId by mutableStateOf<String?>(null)
        private set

    var connectionDragState by mutableStateOf<ConnectionDragState?>(null)
        private set

    var lastPosition by mutableStateOf<Position?>(null)
        private set

    var contextMenuConnection by mutableStateOf<Triple<Room, ExitDirection, Room>?>(null)
        private set

    var contextMenuPosition by mutableStateOf<DpOffset?>(null)
        private set

    // Methods to update state
    fun updateZone(newZone: Zone) {
        if (zone != newZone) {
            log.trace("Zone updated: {}", newZone.rooms.map { it.id })
            zone = newZone
            onZoneChanged(newZone)
        }
    }

    fun updateSelectedRoom(room: Room?) {
        if (selectedRoom != room) {
            log.trace("Selected room updated: {}", room?.id)
            selectedRoom = room
            onRoomSelected(room)
        }
    }

    fun updatePointerPosition(position: Offset?) {
        onPointerPositionChanged(position)
    }

    fun startDraggingRoom(roomId: String) {
        log.debug("Started dragging room: {}", roomId)
        draggedRoomId = roomId
    }

    fun updateRoomPosition(roomId: String, position: Position) {
        val updatedRooms = zone.rooms.map { r ->
            if (r.id == roomId) r.copy(position = position) else r
        }
        updateZone(zone.copy(rooms = updatedRooms))
    }

    fun stopDragging() {
        if (draggedRoomId != null) {
            log.debug("Stopped dragging room: {}", draggedRoomId)
            // Finalize any position updates
            draggedRoomId?.let { id ->
                val room = zone.rooms.first { it.id == id }
                updateZone(zone.updateRoomPosition(id, room.position))
            }
            draggedRoomId = null
        }

        if (connectionDragState != null) {
            log.debug("Stopped dragging connection")
            connectionDragState = null
        }
    }

    fun startConnectionDrag(room: Room, direction: ExitDirection, point: Offset, corner: String) {
        log.debug("Started connection drag from {} in direction {}", room.id, direction)
        connectionDragState = ConnectionDragState(
            sourceRoomId = room.id,
            direction = direction,
            currentPoint = point,
            sourceCorner = corner
        )
        onConnectionStarted(room, direction)
    }

    fun updateConnectionDragPoint(point: Offset) {
        connectionDragState?.let {
            connectionDragState = it.copy(currentPoint = point)
        }
    }

    fun finalizeConnectionDrag(point: Offset, density: Float, zoomLevel: Float, canvasWidthDp: Float, canvasHeightDp: Float) {
        connectionDragState?.let { state ->
            val sourceRoom = zone.rooms.first { it.id == state.sourceRoomId }
            val targetRoom = connectionManager.findRoomAtPoint(
                zone, state.currentPoint, density, zoomLevel
            )?.takeIf { it.id != state.sourceRoomId }

            val updatedZone = if (targetRoom != null) {
                // Connect to existing room
                zone.connectRooms(
                    sourceRoom,
                    targetRoom,
                    state.direction,
                    state.sourceCorner
                )
            } else {
                // Create new room with connection
                val modelX = (state.currentPoint.x / (density * zoomLevel))
                    .coerceIn(0f, canvasWidthDp - zone.nodeWidthDp)

                val modelY = (state.currentPoint.y / (density * zoomLevel))
                    .coerceIn(0f, canvasHeightDp - zone.nodeHeightDp)

                zone.createRoomWithConnection(
                    sourceRoom,
                    state.direction,
                    Position(modelX, modelY),
                    state.sourceCorner
                )
            }
            updateZone(updatedZone)
        }
    }

    fun showContextMenu(connection: Triple<Room, ExitDirection, Room>?, position: DpOffset?) {
        contextMenuConnection = connection
        contextMenuPosition = position
    }

    fun hideContextMenu() {
        contextMenuConnection = null
        contextMenuPosition = null
    }

    fun removeConnection(room: Room, direction: ExitDirection) {
        val updatedZone = zone.removeRoomConnection(room, direction)
        updateZone(updatedZone)
        hideContextMenu()
    }

    fun updateLastPosition(position: Position) {
        lastPosition = position
    }

    // Synchronize with external state
    fun syncWithExternalState(newZone: Zone, newSelectedRoom: Room?) {
        if (zone != newZone) {
            zone = newZone
        }
        
        if (selectedRoom != newSelectedRoom) {
            selectedRoom = newSelectedRoom
        }
    }
}

/**
 * Creates and remembers a ZoneCanvasState.
 */
@Composable
fun rememberZoneCanvasState(
    zone: Zone,
    selectedRoom: Room?,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    onPointerPositionChanged: (Offset?) -> Unit = {}
): ZoneCanvasState {
    val state = remember {
        ZoneCanvasState(
            initialZone = zone,
            initialSelectedRoom = selectedRoom,
            onZoneChanged = onZoneChanged,
            onRoomSelected = onRoomSelected,
            onConnectionStarted = onConnectionStarted,
            onPointerPositionChanged = onPointerPositionChanged
        )
    }

    // Synchronize with external state
    LaunchedEffect(zone, selectedRoom) {
        state.syncWithExternalState(zone, selectedRoom)
    }

    return state
}
