package net.lashua.zonedit.ui.adapters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import net.lashua.zonedit.model.ExitData
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.MudExit
import net.lashua.zonedit.model.MudRoom
import net.lashua.zonedit.ui.graph.VisualEdge
import net.lashua.zonedit.ui.graph.VisualNode

/**
 * Adapter for converting between MudExit and VisualEdge.
 */
object MudExitAdapter {
    /**
     * Converts a MudExit to a VisualEdge.
     * 
     * @param exit The MudExit to convert
     * @param sourceNode The source VisualNode
     * @param targetNode The target VisualNode
     * @param isSelected Whether the edge is selected
     * @return A VisualEdge representing the MudExit
     */
    fun toVisualEdge(
        exit: MudExit,
        sourceNode: VisualNode<*>,
        targetNode: VisualNode<*>,
        isSelected: Boolean = false
    ): VisualEdge<ExitData> {
        // Calculate source and target points based on exit direction
        val (sourcePoint, targetPoint) = calculateConnectionPoints(
            exit.direction,
            exit.corner,
            sourceNode,
            targetNode
        )
        
        // Determine color based on exit direction
        val color = getDirectionColor(exit.direction)
        
        return VisualEdge(
            sourceId = exit.sourceId,
            targetId = exit.targetId,
            sourcePoint = sourcePoint,
            targetPoint = targetPoint,
            color = color,
            data = exit.data,
            isSelected = isSelected
        )
    }
    
    /**
     * Calculates the connection points for an exit.
     * 
     * @param direction The direction of the exit
     * @param corner The corner for UP/DOWN exits
     * @param sourceNode The source node
     * @param targetNode The target node
     * @return A pair of source and target points
     */
    private fun calculateConnectionPoints(
        direction: ExitDirection,
        corner: String?,
        sourceNode: VisualNode<*>,
        targetNode: VisualNode<*>
    ): Pair<Offset, Offset> {
        val sourceBounds = sourceNode.bounds
        val targetBounds = targetNode.bounds
        
        val sourcePoint = when (direction) {
            ExitDirection.NORTH -> Offset(sourceBounds.center.x, sourceBounds.top)
            ExitDirection.SOUTH -> Offset(sourceBounds.center.x, sourceBounds.bottom)
            ExitDirection.EAST -> Offset(sourceBounds.right, sourceBounds.center.y)
            ExitDirection.WEST -> Offset(sourceBounds.left, sourceBounds.center.y)
            ExitDirection.UP -> {
                if (corner == "LEFT") {
                    Offset(sourceBounds.left, sourceBounds.top)
                } else {
                    Offset(sourceBounds.right, sourceBounds.top)
                }
            }
            ExitDirection.DOWN -> {
                if (corner == "LEFT") {
                    Offset(sourceBounds.left, sourceBounds.bottom)
                } else {
                    Offset(sourceBounds.right, sourceBounds.bottom)
                }
            }
        }
        
        // For the target point, use the opposite direction
        val targetDirection = getOppositeDirection(direction)
        val targetPoint = when (targetDirection) {
            ExitDirection.NORTH -> Offset(targetBounds.center.x, targetBounds.top)
            ExitDirection.SOUTH -> Offset(targetBounds.center.x, targetBounds.bottom)
            ExitDirection.EAST -> Offset(targetBounds.right, targetBounds.center.y)
            ExitDirection.WEST -> Offset(targetBounds.left, targetBounds.center.y)
            ExitDirection.UP -> {
                val targetCorner = if (corner == "LEFT") "RIGHT" else "LEFT"
                if (targetCorner == "LEFT") {
                    Offset(targetBounds.left, targetBounds.top)
                } else {
                    Offset(targetBounds.right, targetBounds.top)
                }
            }
            ExitDirection.DOWN -> {
                val targetCorner = if (corner == "LEFT") "RIGHT" else "LEFT"
                if (targetCorner == "LEFT") {
                    Offset(targetBounds.left, targetBounds.bottom)
                } else {
                    Offset(targetBounds.right, targetBounds.bottom)
                }
            }
        }
        
        return Pair(sourcePoint, targetPoint)
    }
    
    /**
     * Gets the opposite direction of a given direction.
     * 
     * @param direction The direction to get the opposite of
     * @return The opposite direction
     */
    private fun getOppositeDirection(direction: ExitDirection): ExitDirection {
        return when (direction) {
            ExitDirection.NORTH -> ExitDirection.SOUTH
            ExitDirection.SOUTH -> ExitDirection.NORTH
            ExitDirection.EAST -> ExitDirection.WEST
            ExitDirection.WEST -> ExitDirection.EAST
            ExitDirection.UP -> ExitDirection.DOWN
            ExitDirection.DOWN -> ExitDirection.UP
        }
    }
    
    /**
     * Gets the color for a direction.
     * 
     * @param direction The direction to get the color for
     * @return The color for the direction
     */
    private fun getDirectionColor(direction: ExitDirection): Color {
        return when (direction) {
            ExitDirection.NORTH, ExitDirection.SOUTH, ExitDirection.EAST, ExitDirection.WEST -> Color.Blue
            ExitDirection.UP -> Color.Green
            ExitDirection.DOWN -> Color.Green
        }
    }
}
