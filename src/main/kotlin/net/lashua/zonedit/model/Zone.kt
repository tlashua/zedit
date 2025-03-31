package net.lashua.zonedit.model

enum class ExitDirection {
    NORTH, SOUTH, EAST, WEST, UP, DOWN
}

data class Zone(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList(),
    val nodeWidth: Float = 100f,  // Default node width in dp
    val nodeHeight: Float = 60f,  // Default node height in dp
)

data class Room(
    val id: String,
    val name: String,
    val description: String,
    val position: Position,
    val exits: Map<ExitDirection, String> = emptyMap(), // Maps direction to destination room ID
)

data class Position(
    val x: Float,
    val y: Float,
)
