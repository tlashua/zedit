package net.lashua.zonedit.ui.graph

/**
 * Represents a visual graph in the graph editor.
 * 
 * @param N The type of data associated with nodes
 * @param E The type of data associated with edges
 * @property nodes The visual nodes in the graph
 * @property edges The visual edges in the graph
 */
data class VisualGraph<N, E>(
    val nodes: List<VisualNode<N>> = emptyList(),
    val edges: List<VisualEdge<E>> = emptyList()
) {
    /**
     * Gets a node by its ID.
     * 
     * @param id The ID of the node to get
     * @return The node with the specified ID, or null if not found
     */
    fun getNode(id: String): VisualNode<N>? {
        return nodes.find { it.id == id }
    }
    
    /**
     * Gets all edges originating from a node.
     * 
     * @param nodeId The ID of the node
     * @return A list of edges originating from the node
     */
    fun getOutgoingEdges(nodeId: String): List<VisualEdge<E>> {
        return edges.filter { it.sourceId == nodeId }
    }
    
    /**
     * Gets all edges targeting a node.
     * 
     * @param nodeId The ID of the node
     * @return A list of edges targeting the node
     */
    fun getIncomingEdges(nodeId: String): List<VisualEdge<E>> {
        return edges.filter { it.targetId == nodeId }
    }
    
    /**
     * Gets the selected node, if any.
     * 
     * @return The selected node, or null if no node is selected
     */
    fun getSelectedNode(): VisualNode<N>? {
        return nodes.find { it.isSelected }
    }
    
    /**
     * Gets the selected edge, if any.
     * 
     * @return The selected edge, or null if no edge is selected
     */
    fun getSelectedEdge(): VisualEdge<E>? {
        return edges.find { it.isSelected }
    }
    
    /**
     * Finds a node at a specific point.
     * 
     * @param x The x-coordinate of the point in dp
     * @param y The y-coordinate of the point in dp
     * @return The node at the specified point, or null if no node is found
     */
    fun findNodeAt(x: Float, y: Float): VisualNode<N>? {
        // Check nodes in reverse order to prioritize nodes drawn on top
        return nodes.reversed().find { it.containsPoint(x, y) }
    }
    
    /**
     * Finds an edge near a specific point.
     * 
     * @param x The x-coordinate of the point in dp
     * @param y The y-coordinate of the point in dp
     * @param threshold The distance threshold in dp
     * @return The edge near the specified point, or null if no edge is found
     */
    fun findEdgeNear(x: Float, y: Float, threshold: Float): VisualEdge<E>? {
        val point = androidx.compose.ui.geometry.Offset(x, y)
        return edges.find { it.isNearPoint(point, threshold) }
    }
    
    /**
     * Selects a node and deselects all others.
     * 
     * @param nodeId The ID of the node to select, or null to deselect all nodes
     * @return A new VisualGraph with the updated selection state
     */
    fun selectNode(nodeId: String?): VisualGraph<N, E> {
        val updatedNodes = nodes.map { it.select(it.id == nodeId) }
        return copy(nodes = updatedNodes)
    }
    
    /**
     * Selects an edge and deselects all others.
     * 
     * @param sourceId The ID of the source node of the edge to select
     * @param targetId The ID of the target node of the edge to select
     * @return A new VisualGraph with the updated selection state
     */
    fun selectEdge(sourceId: String, targetId: String): VisualGraph<N, E> {
        val updatedEdges = edges.map { 
            it.select(it.sourceId == sourceId && it.targetId == targetId) 
        }
        return copy(edges = updatedEdges)
    }
    
    /**
     * Deselects all nodes and edges.
     * 
     * @return A new VisualGraph with all nodes and edges deselected
     */
    fun deselectAll(): VisualGraph<N, E> {
        val updatedNodes = nodes.map { it.select(false) }
        val updatedEdges = edges.map { it.select(false) }
        return copy(nodes = updatedNodes, edges = updatedEdges)
    }
    
    /**
     * Updates the position of a node.
     * 
     * @param nodeId The ID of the node to update
     * @param x The new x-coordinate of the node in dp
     * @param y The new y-coordinate of the node in dp
     * @return A new VisualGraph with the updated node position
     */
    fun updateNodePosition(nodeId: String, x: Float, y: Float): VisualGraph<N, E> {
        val updatedNodes = nodes.map { 
            if (it.id == nodeId) it.moveTo(x, y) else it 
        }
        return copy(nodes = updatedNodes)
    }
}
