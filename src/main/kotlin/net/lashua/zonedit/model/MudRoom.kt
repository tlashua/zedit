package net.lashua.zonedit.model

import net.lashua.zonedit.graph.model.Node

/**
 * Represents a room in a MUD zone.
 * Extends the generic Node class with MUD-specific functionality.
 */
class MudRoom(id: String, data: RoomData) : Node<RoomData>(id, data) {
    /**
     * Gets the name of the room.
     */
    val name: String
        get() = data.name
    
    /**
     * Gets the description of the room.
     */
    val description: String
        get() = data.description
    
    /**
     * Gets the position of the room in the editor.
     */
    val position: Position
        get() = data.position
    
    /**
     * Gets the flags associated with the room.
     */
    val flags: List<String>
        get() = data.flags
    
    /**
     * Gets the metadata associated with the room.
     */
    val metadata: Map<String, String>
        get() = data.metadata
    
    /**
     * Creates a copy of this room with updated data.
     * 
     * @param name The new name of the room
     * @param description The new description of the room
     * @param position The new position of the room
     * @param flags The new flags associated with the room
     * @param metadata The new metadata associated with the room
     * @return A new MudRoom with the updated data
     */
    fun copy(
        name: String = this.name,
        description: String = this.description,
        position: Position = this.position,
        flags: List<String> = this.flags,
        metadata: Map<String, String> = this.metadata
    ): MudRoom {
        val newData = RoomData(name, description, position, flags, metadata)
        return MudRoom(id, newData)
    }
    
    /**
     * Updates the position of the room.
     * 
     * @param position The new position of the room
     * @return A new MudRoom with the updated position
     */
    fun updatePosition(position: Position): MudRoom {
        return copy(position = position)
    }
}
