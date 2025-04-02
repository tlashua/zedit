package net.lashua.zonedit.model

import kotlin.math.roundToInt

enum class ExitDirection {
    NORTH, EAST, SOUTH, WEST, UP, DOWN
}

data class Zone(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList(),
    val nodeWidthDp: Float = 100f,  // Changed from nodeWidth
    val nodeHeightDp: Float = 60f,  // Changed from nodeHeight
    val gridSizeDp: Float = 20f,    // Changed from gridSize
    val snapToGrid: Boolean = true
) {
    // Update helper functions to work with dp
    fun getNextRoomNumber(): Int {
        if (rooms.isEmpty()) return 0

        return rooms
            .mapNotNull { room ->
                room.id.removePrefix("${name.lowercase()}").toIntOrNull()
            }
            .maxOrNull()?.plus(1) ?: 0
    }

    fun snapPosition(pos: Position): Position {
        if (!snapToGrid) return pos
        return Position(
            x = (pos.x / gridSizeDp).roundToInt() * gridSizeDp,
            y = (pos.y / gridSizeDp).roundToInt() * gridSizeDp
        )
    }

    fun createRoomWithConnection(
        sourceRoom: Room,
        direction: ExitDirection,
        position: Position,
        sourceCorner: String? = null
    ): Zone {
        val newRoom = Room(
            id = "${name.lowercase()}${getNextRoomNumber()}",
            name = "New Room",
            description = "Description",
            position = if (snapToGrid) snapPosition(position) else position
        )

        val (updatedSource, updatedNewRoom) = ConnectionManager().createConnection(
            sourceRoom,
            direction,
            newRoom,
            sourceCorner
        )

        return copy(rooms = rooms.map { room ->
            when (room.id) {
                updatedSource.id -> updatedSource
                else -> room
            }
        } + updatedNewRoom)
    }

    fun connectRooms(
        sourceRoom: Room,
        targetRoom: Room,
        direction: ExitDirection,
        sourceCorner: String? = null
    ): Zone {
        val (updatedSource, updatedDest) = ConnectionManager().createConnection(
            sourceRoom,
            direction,
            targetRoom,
            sourceCorner
        )

        return copy(rooms = rooms.map { room ->
            when (room.id) {
                updatedSource.id -> updatedSource
                updatedDest.id -> updatedDest
                else -> room
            }
        })
    }

    fun updateRoomPosition(roomId: String, position: Position): Zone {
        val finalPos = if (snapToGrid) snapPosition(position) else position
        return copy(rooms = rooms.map { room ->
            if (room.id == roomId) room.copy(position = finalPos) else room
        })
    }

    fun removeRoomConnection(sourceRoom: Room, direction: ExitDirection): Zone {
        val (updatedSource, updatedDest) = ConnectionManager().removeConnection(sourceRoom, direction, this)
            ?: return this // Return unchanged zone if connection doesn't exist

        return copy(rooms = rooms.map { room ->
            when (room.id) {
                updatedSource.id -> updatedSource
                updatedDest.id -> updatedDest
                else -> room
            }
        })
    }
}

// I don't know that strings are a good idea here.  At one point we may
// need an identifier object.
data class Exits(
    var northDest: String?,
    var eastDest: String?,
    var southDest: String?,
    var westDest: String?,
    var upDest: String?,
    var downDest: String?,
)

data class Room(
    val id: String,
    val name: String,
    val description: String,
    val position: Position,
    val exits: List<Exit> = emptyList(),
    val flags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Gets all exits in a specific direction
     *
     * @param direction The direction to get exits for
     * @return A list of exits in the specified direction
     */
    fun getExitsInDirection(direction: ExitDirection): List<Exit> {
        return exits.filter { it.direction == direction }
    }

    /**
     * Gets the first exit in a specific direction
     *
     * @param direction The direction to get the exit for
     * @return The first exit in the specified direction, or null if none exists
     */
    fun getFirstExitInDirection(direction: ExitDirection): Exit? {
        return exits.firstOrNull { it.direction == direction }
    }

    /**
     * Checks if this room has an exit to a specific room
     *
     * @param direction The direction to check
     * @param destinationId The ID of the destination room
     * @return True if this room has an exit to the specified room in the specified direction
     */
    fun hasExitTo(direction: ExitDirection, destinationId: String): Boolean {
        return exits.any { it.direction == direction && it.destinationId == destinationId }
    }
}

data class Position(
    val x: Float,
    val y: Float,
)
