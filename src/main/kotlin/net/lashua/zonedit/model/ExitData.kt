package net.lashua.zonedit.model

/**
 * Represents the data associated with an exit in a MUD zone.
 * 
 * @property direction The direction of the exit
 * @property corner The corner for UP/DOWN exits (LEFT or RIGHT)
 * @property flags The flags associated with the exit (e.g., DOOR, CLOSED, LOCKED)
 * @property metadata Additional metadata associated with the exit
 */
data class ExitData(
    val direction: ExitDirection,
    val corner: String? = null,
    val flags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
