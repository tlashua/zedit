package net.lashua.zonedit.model

data class Zone(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList(),
)

data class Room(
    val id: String,
    val name: String,
    val description: String,
    val position: Position,
    val exits: Map<Direction, String> = emptyMap(), // Maps direction to destination room ID
)

data class Position(
    val x: Float,
    val y: Float,
)

enum class Direction {
    NORTH, SOUTH, EAST, WEST, UP, DOWN
}