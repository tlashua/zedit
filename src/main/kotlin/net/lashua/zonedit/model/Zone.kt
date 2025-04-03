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
                room.id.removePrefix(name.lowercase()).toIntOrNull()
            }
            .maxOrNull()?.plus(1) ?: 0
    }

    fun snapPosition(pos: Position): Position {
        if (!snapToGrid) return pos

        val snappedX = (pos.x / gridSizeDp).roundToInt() * gridSizeDp
        val snappedY = (pos.y / gridSizeDp).roundToInt() * gridSizeDp

        // Add debug logging
        println("Snapping position: (${pos.x}, ${pos.y}) to (${snappedX}, ${snappedY}) with gridSize=${gridSizeDp}")

        return Position(x = snappedX, y = snappedY)
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
    val exits: Map<ExitDirection, String> = emptyMap(),
    val exitCorners: Map<ExitDirection, String> = emptyMap(), // "LEFT" or "RIGHT" for UP/DOWN exits
    val flags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

data class Position(
    val x: Float,
    val y: Float,
)
