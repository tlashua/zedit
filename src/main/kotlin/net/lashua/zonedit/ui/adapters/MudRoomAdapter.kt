package net.lashua.zonedit.ui.adapters

import androidx.compose.ui.graphics.Color
import net.lashua.zonedit.model.MudRoom
import net.lashua.zonedit.model.RoomData
import net.lashua.zonedit.ui.graph.VisualNode

/**
 * Adapter for converting between MudRoom and VisualNode.
 */
object MudRoomAdapter {
    /**
     * Converts a MudRoom to a VisualNode.
     * 
     * @param room The MudRoom to convert
     * @param width The width of the node in dp
     * @param height The height of the node in dp
     * @param isSelected Whether the node is selected
     * @return A VisualNode representing the MudRoom
     */
    fun toVisualNode(
        room: MudRoom,
        width: Float,
        height: Float,
        isSelected: Boolean = false
    ): VisualNode<RoomData> {
        return VisualNode(
            id = room.id,
            x = room.position.x,
            y = room.position.y,
            width = width,
            height = height,
            data = room.data,
            isSelected = isSelected
        )
    }
    
    /**
     * Updates a MudRoom with the position from a VisualNode.
     * 
     * @param room The MudRoom to update
     * @param visualNode The VisualNode to get the position from
     * @return A new MudRoom with the updated position
     */
    fun updateRoomFromVisualNode(
        room: MudRoom,
        visualNode: VisualNode<RoomData>
    ): MudRoom {
        return room.updatePosition(
            net.lashua.zonedit.model.Position(visualNode.x, visualNode.y)
        )
    }
}
