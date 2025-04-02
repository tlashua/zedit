package net.lashua.zonedit.ui.graph

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

/**
 * Represents a visual node in the graph editor.
 * 
 * @param T The type of data associated with this node
 * @property id A unique identifier for this node
 * @property x The x-coordinate of the node in dp
 * @property y The y-coordinate of the node in dp
 * @property width The width of the node in dp
 * @property height The height of the node in dp
 * @property data The data associated with this node
 * @property isSelected Whether the node is selected
 */
data class VisualNode<T>(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val data: T,
    val isSelected: Boolean = false
) {
    /**
     * Gets the bounds of the node in dp.
     */
    val bounds: Rect
        get() = Rect(x, y, x + width, y + height)
    
    /**
     * Gets the size of the node in dp.
     */
    val size: Size
        get() = Size(width, height)
    
    /**
     * Checks if a point is inside the node.
     * 
     * @param pointX The x-coordinate of the point in dp
     * @param pointY The y-coordinate of the point in dp
     * @return True if the point is inside the node, false otherwise
     */
    fun containsPoint(pointX: Float, pointY: Float): Boolean {
        return bounds.contains(androidx.compose.ui.geometry.Offset(pointX, pointY))
    }
    
    /**
     * Creates a copy of this node with updated position.
     * 
     * @param x The new x-coordinate of the node in dp
     * @param y The new y-coordinate of the node in dp
     * @return A new VisualNode with the updated position
     */
    fun moveTo(x: Float, y: Float): VisualNode<T> {
        return copy(x = x, y = y)
    }
    
    /**
     * Creates a copy of this node with updated selection state.
     * 
     * @param isSelected The new selection state
     * @return A new VisualNode with the updated selection state
     */
    fun select(isSelected: Boolean): VisualNode<T> {
        return copy(isSelected = isSelected)
    }
}
