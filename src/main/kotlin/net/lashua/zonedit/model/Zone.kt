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
