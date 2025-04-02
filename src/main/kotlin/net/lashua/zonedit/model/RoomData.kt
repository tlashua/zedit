package net.lashua.zonedit.model

/**
 * Represents the data associated with a room in a MUD zone.
 * 
 * @property name The name of the room
 * @property description The description of the room
 * @property position The position of the room in the editor
 * @property flags The flags associated with the room
 * @property metadata Additional metadata associated with the room
 */
data class RoomData(
    val name: String,
    val description: String,
    val position: Position,
    val flags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
