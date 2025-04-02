package net.lashua.zonedit.model

import net.lashua.zonedit.graph.model.Edge

/**
 * Represents an exit in a MUD zone.
 * Extends the generic Edge class with MUD-specific functionality.
 */
class MudExit(sourceId: String, targetId: String, data: ExitData) : Edge<ExitData>(sourceId, targetId, data) {
    /**
     * Gets the direction of the exit.
     */
    val direction: ExitDirection
        get() = data.direction
    
    /**
     * Gets the corner for UP/DOWN exits (LEFT or RIGHT).
     */
    val corner: String?
        get() = data.corner
    
    /**
     * Gets the flags associated with the exit.
     */
    val flags: List<String>
        get() = data.flags
    
    /**
     * Gets the metadata associated with the exit.
     */
    val metadata: Map<String, String>
        get() = data.metadata
    
    /**
     * Creates a copy of this exit with updated data.
     * 
     * @param direction The new direction of the exit
     * @param corner The new corner for UP/DOWN exits
     * @param flags The new flags associated with the exit
     * @param metadata The new metadata associated with the exit
     * @return A new MudExit with the updated data
     */
    fun copy(
        direction: ExitDirection = this.direction,
        corner: String? = this.corner,
        flags: List<String> = this.flags,
        metadata: Map<String, String> = this.metadata
    ): MudExit {
        val newData = ExitData(direction, corner, flags, metadata)
        return MudExit(sourceId, targetId, newData)
    }
}
