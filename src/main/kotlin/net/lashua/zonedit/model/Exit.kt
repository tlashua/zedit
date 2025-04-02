package net.lashua.zonedit.model

/**
 * Represents an exit from a room to another room.
 * 
 * @property direction The direction of the exit
 * @property destinationId The ID of the destination room
 * @property corner The corner for UP/DOWN exits (LEFT or RIGHT)
 */
data class Exit(
    val direction: ExitDirection,
    val destinationId: String,
    val corner: String? = null
)
