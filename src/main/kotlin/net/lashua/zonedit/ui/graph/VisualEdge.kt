package net.lashua.zonedit.ui.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Represents a visual edge in the graph editor.
 * 
 * @param T The type of data associated with this edge
 * @property sourceId The ID of the source node
 * @property targetId The ID of the target node
 * @property sourcePoint The source point of the edge in dp
 * @property targetPoint The target point of the edge in dp
 * @property color The color of the edge
 * @property strokeWidth The stroke width of the edge
 * @property data The data associated with this edge
 * @property isSelected Whether the edge is selected
 */
data class VisualEdge<T>(
    val sourceId: String,
    val targetId: String,
    val sourcePoint: Offset,
    val targetPoint: Offset,
    val color: Color,
    val strokeWidth: Float = 2f,
    val data: T,
    val isSelected: Boolean = false
) {
    /**
     * Checks if a point is near the edge.
     * 
     * @param point The point to check in dp
     * @param threshold The distance threshold in dp
     * @return True if the point is near the edge, false otherwise
     */
    fun isNearPoint(point: Offset, threshold: Float): Boolean {
        // Calculate the distance from the point to the line segment
        val distance = distanceToLineSegment(sourcePoint, targetPoint, point)
        return distance <= threshold
    }
    
    /**
     * Calculates the distance from a point to a line segment.
     * 
     * @param start The start point of the line segment
     * @param end The end point of the line segment
     * @param point The point to calculate the distance to
     * @return The distance from the point to the line segment
     */
    private fun distanceToLineSegment(start: Offset, end: Offset, point: Offset): Float {
        val lengthSquared = (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        if (lengthSquared == 0f) return (point - start).getDistance()
        
        // Calculate the projection of the point onto the line
        val t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / lengthSquared
        val projectionT = t.coerceIn(0f, 1f)
        
        // Calculate the closest point on the line segment
        val projection = Offset(
            start.x + projectionT * (end.x - start.x),
            start.y + projectionT * (end.y - start.y)
        )
        
        // Calculate the distance from the point to the projection
        return (point - projection).getDistance()
    }
    
    /**
     * Creates a copy of this edge with updated selection state.
     * 
     * @param isSelected The new selection state
     * @return A new VisualEdge with the updated selection state
     */
    fun select(isSelected: Boolean): VisualEdge<T> {
        return copy(isSelected = isSelected)
    }
}
