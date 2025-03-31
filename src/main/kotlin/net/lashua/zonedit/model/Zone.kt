package net.lashua.zonedit.model

import kotlin.math.roundToInt

enum class ExitDirection {
    NORTH, EAST, SOUTH, WEST, UP, DOWN
}

data class Zone(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList(),
    val nodeWidth: Float = 100f,  // Default node width in dp
    val nodeHeight: Float = 60f,  // Default node height in dp
    val gridSize: Float = 20f,  // Default grid size
    val snapToGrid: Boolean = true  // Default to enabled
) {
    // Helper function to get next available room number
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
            x = (pos.x / gridSize).roundToInt() * gridSize,
            y = (pos.y / gridSize).roundToInt() * gridSize
        )
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
    val flags: List<String> = emptyList()
)

data class Position(
    val x: Float,
    val y: Float,
)
