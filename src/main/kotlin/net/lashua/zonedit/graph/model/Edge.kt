package net.lashua.zonedit.graph.model

/**
 * Represents a directed edge in a graph.
 *
 * @param T The type of data associated with this edge
 * @property sourceId The ID of the source node
 * @property targetId The ID of the target node
 * @property data The data associated with this edge
 */
open class Edge<T>(
    val sourceId: String,
    val targetId: String,
    val data: T
)
