package net.lashua.zonedit.graph.model

/**
 * Represents a generic node in a graph.
 *
 * @param T The type of data associated with this node
 * @property id A unique identifier for this node
 * @property data The data associated with this node
 */
open class Node<T>(
    val id: String,
    val data: T
)
