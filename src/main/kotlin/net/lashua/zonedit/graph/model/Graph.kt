package net.lashua.zonedit.graph.model

/**
 * Represents a directed graph with nodes and edges.
 *
 * @param N The type of data associated with nodes
 * @param E The type of data associated with edges
 * @property nodes The nodes in the graph
 * @property edges The edges in the graph
 */
open class Graph<N, E>(
    val nodes: List<Node<N>> = emptyList(),
    val edges: List<Edge<E>> = emptyList()
) {
    /**
     * Creates a copy of this graph with the specified nodes and edges.
     *
     * @param nodes The nodes for the new graph
     * @param edges The edges for the new graph
     * @return A new Graph with the specified nodes and edges
     */
    fun copy(
        nodes: List<Node<N>> = this.nodes,
        edges: List<Edge<E>> = this.edges
    ): Graph<N, E> {
        return Graph(nodes, edges)
    }
    /**
     * Gets a node by its ID.
     *
     * @param id The ID of the node to get
     * @return The node with the specified ID, or null if not found
     */
    fun getNode(id: String): Node<N>? {
        return nodes.find { it.id == id }
    }

    /**
     * Gets all edges originating from a node.
     *
     * @param nodeId The ID of the node
     * @return A list of edges originating from the node
     */
    fun getOutgoingEdges(nodeId: String): List<Edge<E>> {
        return edges.filter { it.sourceId == nodeId }
    }

    /**
     * Gets all edges targeting a node.
     *
     * @param nodeId The ID of the node
     * @return A list of edges targeting the node
     */
    fun getIncomingEdges(nodeId: String): List<Edge<E>> {
        return edges.filter { it.targetId == nodeId }
    }

    /**
     * Gets all nodes connected to a node by outgoing edges.
     *
     * @param nodeId The ID of the node
     * @return A list of nodes connected to the node by outgoing edges
     */
    fun getOutgoingNodes(nodeId: String): List<Node<N>> {
        val targetIds = getOutgoingEdges(nodeId).map { it.targetId }
        return nodes.filter { it.id in targetIds }
    }

    /**
     * Gets all nodes connected to a node by incoming edges.
     *
     * @param nodeId The ID of the node
     * @return A list of nodes connected to the node by incoming edges
     */
    fun getIncomingNodes(nodeId: String): List<Node<N>> {
        val sourceIds = getIncomingEdges(nodeId).map { it.sourceId }
        return nodes.filter { it.id in sourceIds }
    }

    /**
     * Adds a node to the graph.
     *
     * @param node The node to add
     * @return A new graph with the node added
     */
    fun addNode(node: Node<N>): Graph<N, E> {
        return copy(nodes = nodes + node)
    }

    /**
     * Removes a node from the graph.
     * Also removes all edges connected to the node.
     *
     * @param nodeId The ID of the node to remove
     * @return A new graph with the node and its edges removed
     */
    fun removeNode(nodeId: String): Graph<N, E> {
        val updatedNodes = nodes.filter { it.id != nodeId }
        val updatedEdges = edges.filter { it.sourceId != nodeId && it.targetId != nodeId }
        return copy(nodes = updatedNodes, edges = updatedEdges)
    }

    /**
     * Updates a node in the graph.
     *
     * @param node The updated node
     * @return A new graph with the node updated
     */
    fun updateNode(node: Node<N>): Graph<N, E> {
        val updatedNodes = nodes.map { if (it.id == node.id) node else it }
        return copy(nodes = updatedNodes)
    }

    /**
     * Adds an edge to the graph.
     *
     * @param edge The edge to add
     * @return A new graph with the edge added
     */
    fun addEdge(edge: Edge<E>): Graph<N, E> {
        return copy(edges = edges + edge)
    }

    /**
     * Removes an edge from the graph.
     *
     * @param sourceId The ID of the source node
     * @param targetId The ID of the target node
     * @return A new graph with the edge removed
     */
    fun removeEdge(sourceId: String, targetId: String): Graph<N, E> {
        val updatedEdges = edges.filter { !(it.sourceId == sourceId && it.targetId == targetId) }
        return copy(edges = updatedEdges)
    }

    /**
     * Updates an edge in the graph.
     *
     * @param edge The updated edge
     * @return A new graph with the edge updated
     */
    fun updateEdge(edge: Edge<E>): Graph<N, E> {
        val updatedEdges = edges.map {
            if (it.sourceId == edge.sourceId && it.targetId == edge.targetId) edge else it
        }
        return copy(edges = updatedEdges)
    }
}
