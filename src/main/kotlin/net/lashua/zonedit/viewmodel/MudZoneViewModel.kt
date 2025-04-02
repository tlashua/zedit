package net.lashua.zonedit.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.MudExit
import net.lashua.zonedit.model.MudRoom
import net.lashua.zonedit.model.MudZone
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.RoomData
import org.slf4j.LoggerFactory

/**
 * ViewModel for the MudZone editor.
 * Handles user interactions and state management.
 */
class MudZoneViewModel(initialZone: MudZone) {
    private val log = LoggerFactory.getLogger("net.lashua.zonedit.viewmodel.MudZoneViewModel")

    // State
    private val _zone = mutableStateOf(initialZone)
    var zone: MudZone
        get() = _zone.value
        set(value) {
            if (_zone.value != value) {
                _zone.value = value
                onZoneChanged?.invoke(value)
            }
        }

    private val _selectedRoomId = mutableStateOf<String?>(null)
    var selectedRoomId: String?
        get() = _selectedRoomId.value
        private set(value) {
            if (_selectedRoomId.value != value) {
                _selectedRoomId.value = value
                onRoomSelected?.invoke(value)
            }
        }

    private val _draggedRoomId = mutableStateOf<String?>(null)
    var draggedRoomId: String?
        get() = _draggedRoomId.value
        private set(value) {
            _draggedRoomId.value = value
        }

    private val _connectionDragState = mutableStateOf<ConnectionDragState?>(null)
    var connectionDragState: ConnectionDragState?
        get() = _connectionDragState.value
        private set(value) {
            _connectionDragState.value = value
        }

    private val _contextMenuInfo = mutableStateOf<ContextMenuInfo?>(null)
    var contextMenuInfo: ContextMenuInfo?
        get() = _contextMenuInfo.value
        private set(value) {
            _contextMenuInfo.value = value
        }

    // Callbacks
    var onZoneChanged: ((MudZone) -> Unit)? = null
    var onRoomSelected: ((String?) -> Unit)? = null

    // Actions

    /**
     * Selects a room.
     *
     * @param roomId The ID of the room to select, or null to deselect
     */
    fun selectRoom(roomId: String?) {
        log.debug("MudZoneViewModel: Selecting room: {}", roomId)
        selectedRoomId = roomId
    }

    /**
     * Starts dragging a room.
     *
     * @param roomId The ID of the room to drag
     */
    fun startDraggingRoom(roomId: String) {
        log.debug("MudZoneViewModel: Starting room drag: {}", roomId)
        draggedRoomId = roomId
    }

    /**
     * Updates the position of a room during dragging.
     *
     * @param roomId The ID of the room to update
     * @param position The new position of the room
     */
    fun updateRoomPosition(roomId: String, position: Position, snap: Boolean = false) {
        log.debug("MudZoneViewModel: Updating room position: {} to {}, snap: {}", roomId, position, snap)
        // Use the MudZone's updateRoomPosition method, which handles snapping to grid
        zone = zone.updateRoomPosition(roomId, position, snap)
    }

    /**
     * Stops dragging a room.
     */
    fun stopDraggingRoom() {
        log.debug("MudZoneViewModel: Stopping room drag: {}", draggedRoomId)
        draggedRoomId = null
    }

    /**
     * Starts dragging a connection.
     *
     * @param roomId The ID of the source room
     * @param direction The direction of the connection
     * @param corner The corner for UP/DOWN connections
     */
    fun startConnectionDrag(roomId: String, direction: ExitDirection, corner: String? = null) {
        log.debug("MudZoneViewModel: Starting connection drag from {} in direction {}, corner: {}", roomId, direction, corner)
        connectionDragState = ConnectionDragState(roomId, direction, corner)
    }

    /**
     * Finalizes a connection drag by connecting to a target room or creating a new room.
     *
     * @param targetRoomId The ID of the target room, or null to create a new room
     * @param position The position for the new room, if creating one
     */
    fun finalizeConnectionDrag(targetRoomId: String?, position: Position? = null) {
        val state = connectionDragState ?: return
        log.debug("MudZoneViewModel: Finalizing connection drag from {} to {}, position: {}",
            state.sourceRoomId, targetRoomId, position)

        val sourceRoom = zone.getRoom(state.sourceRoomId) ?: return

        zone = if (targetRoomId != null) {
            // Connect to existing room
            val targetRoom = zone.getRoom(targetRoomId) ?: return
            log.debug("MudZoneViewModel: Connecting rooms {} to {} in direction {}, corner: {}",
                sourceRoom.id, targetRoom.id, state.direction, state.corner)
            zone.connectRooms(sourceRoom, targetRoom, state.direction, state.corner)
        } else if (position != null) {
            // Create new room with connection
            log.debug("MudZoneViewModel: Creating new room with connection from {} in direction {}, corner: {}",
                sourceRoom.id, state.direction, state.corner)
            zone.createRoomWithConnection(sourceRoom, state.direction, position, state.corner)
        } else {
            // No target room or position, do nothing
            log.debug("MudZoneViewModel: No target room or position, doing nothing")
            zone
        }

        connectionDragState = null
    }

    /**
     * Cancels a connection drag.
     */
    fun cancelConnectionDrag() {
        log.debug("Canceling connection drag")
        connectionDragState = null
    }

    /**
     * Shows a context menu for a connection.
     *
     * @param sourceRoomId The ID of the source room
     * @param direction The direction of the connection
     * @param targetRoomId The ID of the target room
     * @param position The position of the context menu
     */
    fun showConnectionContextMenu(
        sourceRoomId: String,
        direction: ExitDirection,
        targetRoomId: String,
        position: androidx.compose.ui.unit.DpOffset
    ) {
        log.debug("Showing context menu for connection: {} --[{}]--> {}", sourceRoomId, direction, targetRoomId)
        contextMenuInfo = ContextMenuInfo.ConnectionMenu(sourceRoomId, direction, targetRoomId, position)
    }

    /**
     * Shows a context menu for a room.
     *
     * @param roomId The ID of the room
     * @param position The position of the context menu
     */
    fun showRoomContextMenu(roomId: String, position: androidx.compose.ui.unit.DpOffset) {
        log.debug("Showing context menu for room: {}", roomId)
        contextMenuInfo = ContextMenuInfo.RoomMenu(roomId, position)
    }

    /**
     * Hides the context menu.
     */
    fun hideContextMenu() {
        log.debug("Hiding context menu")
        contextMenuInfo = null
    }

    /**
     * Removes a connection.
     *
     * @param sourceRoomId The ID of the source room
     * @param direction The direction of the connection
     * @param targetRoomId The ID of the target room
     */
    fun removeConnection(sourceRoomId: String, direction: ExitDirection, targetRoomId: String) {
        log.debug("Removing connection: {} --[{}]--> {}", sourceRoomId, direction, targetRoomId)

        val sourceRoom = zone.getRoom(sourceRoomId) ?: return
        val targetRoom = zone.getRoom(targetRoomId) ?: return

        zone = zone.removeExit(sourceRoomId, targetRoomId, direction)
        hideContextMenu()
    }

    /**
     * Creates a new room.
     *
     * @param position The position of the new room
     * @return The ID of the new room
     */
    fun createRoom(position: Position): String {
        log.debug("Creating new room at position: {}", position)

        val roomId = "room${zone.rooms.size + 1}"
        val room = MudRoom(
            id = roomId,
            data = RoomData(
                name = "New Room",
                description = "A new room",
                position = position
            )
        )

        zone = zone.addRoom(room)
        return roomId
    }

    /**
     * Removes a room.
     *
     * @param roomId The ID of the room to remove
     */
    fun removeRoom(roomId: String) {
        log.debug("Removing room: {}", roomId)

        zone = zone.removeRoom(roomId)

        if (selectedRoomId == roomId) {
            selectedRoomId = null
        }

        hideContextMenu()
    }

    /**
     * Updates a room's name.
     *
     * @param roomId The ID of the room to update
     * @param name The new name of the room
     */
    fun updateRoomName(roomId: String, name: String) {
        log.debug("Updating room name: {} -> {}", roomId, name)

        val room = zone.getRoom(roomId) ?: return
        val updatedRoom = room.copy(name = name)
        zone = zone.updateRoom(updatedRoom)
    }

    /**
     * Updates a room's description.
     *
     * @param roomId The ID of the room to update
     * @param description The new description of the room
     */
    fun updateRoomDescription(roomId: String, description: String) {
        log.debug("Updating room description: {}", roomId)

        val room = zone.getRoom(roomId) ?: return
        val updatedRoom = room.copy(description = description)
        zone = zone.updateRoom(updatedRoom)
    }
}

/**
 * State for dragging a connection.
 *
 * @property sourceRoomId The ID of the source room
 * @property direction The direction of the connection
 * @property corner The corner for UP/DOWN connections
 */
data class ConnectionDragState(
    val sourceRoomId: String,
    val direction: ExitDirection,
    val corner: String? = null
) {
    override fun toString(): String {
        return "ConnectionDragState(sourceRoomId=$sourceRoomId, direction=$direction, corner=$corner)"
    }
}

/**
 * Information for a context menu.
 */
sealed class ContextMenuInfo {
    /**
     * Information for a connection context menu.
     *
     * @property sourceRoomId The ID of the source room
     * @property direction The direction of the connection
     * @property targetRoomId The ID of the target room
     * @property position The position of the context menu
     */
    data class ConnectionMenu(
        val sourceRoomId: String,
        val direction: ExitDirection,
        val targetRoomId: String,
        val position: androidx.compose.ui.unit.DpOffset
    ) : ContextMenuInfo()

    /**
     * Information for a room context menu.
     *
     * @property roomId The ID of the room
     * @property position The position of the context menu
     */
    data class RoomMenu(
        val roomId: String,
        val position: androidx.compose.ui.unit.DpOffset
    ) : ContextMenuInfo()
}
