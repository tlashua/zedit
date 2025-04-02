package net.lashua.zonedit.ui.adapters

import net.lashua.zonedit.model.ExitData
import net.lashua.zonedit.model.MudZone
import net.lashua.zonedit.model.RoomData
import net.lashua.zonedit.ui.graph.VisualGraph
import net.lashua.zonedit.ui.graph.VisualNode

/**
 * Adapter for converting between MudZone and VisualGraph.
 */
object MudZoneAdapter {
    /**
     * Converts a MudZone to a VisualGraph.
     * 
     * @param zone The MudZone to convert
     * @param selectedRoomId The ID of the selected room, if any
     * @return A VisualGraph representing the MudZone
     */
    fun toVisualGraph(
        zone: MudZone,
        selectedRoomId: String? = null
    ): VisualGraph<RoomData, ExitData> {
        // Convert rooms to visual nodes
        val visualNodes = zone.rooms.map { room ->
            MudRoomAdapter.toVisualNode(
                room = room,
                width = zone.nodeWidthDp,
                height = zone.nodeHeightDp,
                isSelected = room.id == selectedRoomId
            )
        }
        
        // Create a map of node IDs to visual nodes for quick lookup
        val nodeMap = visualNodes.associateBy { it.id }
        
        // Convert exits to visual edges
        val visualEdges = zone.exits.mapNotNull { exit ->
            val sourceNode = nodeMap[exit.sourceId] ?: return@mapNotNull null
            val targetNode = nodeMap[exit.targetId] ?: return@mapNotNull null
            
            MudExitAdapter.toVisualEdge(
                exit = exit,
                sourceNode = sourceNode,
                targetNode = targetNode
            )
        }
        
        return VisualGraph(
            nodes = visualNodes,
            edges = visualEdges
        )
    }
    
    /**
     * Updates a MudZone with the positions from a VisualGraph.
     * 
     * @param zone The MudZone to update
     * @param visualGraph The VisualGraph to get the positions from
     * @return A new MudZone with the updated positions
     */
    fun updateZoneFromVisualGraph(
        zone: MudZone,
        visualGraph: VisualGraph<RoomData, ExitData>
    ): MudZone {
        var updatedZone = zone
        
        // Update room positions
        for (visualNode in visualGraph.nodes) {
            val room = zone.getRoom(visualNode.id) ?: continue
            val updatedRoom = MudRoomAdapter.updateRoomFromVisualNode(room, visualNode)
            updatedZone = updatedZone.updateRoom(updatedRoom)
        }
        
        return updatedZone
    }
}
