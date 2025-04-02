package net.lashua.zonedit.ui.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import net.lashua.zonedit.model.ExitData

/**
 * Renderer for drawing edges in the graph editor.
 */
object EdgeRenderer {
    /**
     * Draws an edge in the graph editor.
     * 
     * @param drawScope The DrawScope to draw in
     * @param edge The edge to draw
     * @param zoomLevel The current zoom level
     * @param isDashed Whether to draw the edge as dashed
     */
    fun draw(
        drawScope: DrawScope,
        edge: VisualEdge<ExitData>,
        zoomLevel: Float,
        isDashed: Boolean = false
    ) {
        drawScope.apply {
            // Draw the line
            drawLine(
                color = edge.color,
                start = edge.sourcePoint,
                end = edge.targetPoint,
                strokeWidth = edge.strokeWidth * zoomLevel,
                pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
            )
            
            // Draw arrows
            drawArrows(edge.sourcePoint, edge.targetPoint, edge.color, zoomLevel)
        }
    }
    
    /**
     * Draws arrows for an edge.
     * 
     * @param sourcePoint The source point of the edge
     * @param targetPoint The target point of the edge
     * @param color The color of the arrows
     * @param zoomLevel The current zoom level
     */
    private fun DrawScope.drawArrows(
        sourcePoint: Offset,
        targetPoint: Offset,
        color: Color,
        zoomLevel: Float
    ) {
        val arrowLength = 20f * zoomLevel
        val arrowAngle = (kotlin.math.PI / 6).toFloat()
        
        // Calculate angles
        val angleToDestination = calculateAngle(sourcePoint, targetPoint)
        val angleToSource = calculateAngle(targetPoint, sourcePoint)
        
        // Draw destination arrow
        val (destArrowPoint1, destArrowPoint2) = calculateArrowPoints(
            targetPoint, angleToDestination, arrowLength, arrowAngle
        )
        drawLine(color = color, start = targetPoint, end = destArrowPoint1, strokeWidth = 2f * zoomLevel)
        drawLine(color = color, start = targetPoint, end = destArrowPoint2, strokeWidth = 2f * zoomLevel)
        
        // Draw source arrow
        val (sourceArrowPoint1, sourceArrowPoint2) = calculateArrowPoints(
            sourcePoint, angleToSource, arrowLength, arrowAngle
        )
        drawLine(color = color, start = sourcePoint, end = sourceArrowPoint1, strokeWidth = 2f * zoomLevel)
        drawLine(color = color, start = sourcePoint, end = sourceArrowPoint2, strokeWidth = 2f * zoomLevel)
    }
    
    /**
     * Calculates the angle between two points.
     * 
     * @param from The starting point
     * @param to The ending point
     * @return The angle in radians
     */
    private fun calculateAngle(from: Offset, to: Offset): Float {
        return kotlin.math.atan2(to.y - from.y, to.x - from.x)
    }
    
    /**
     * Calculates the points for an arrow.
     * 
     * @param point The point of the arrow
     * @param angle The angle of the arrow
     * @param length The length of the arrow
     * @param arrowAngle The angle of the arrow head
     * @return A pair of points for the arrow head
     */
    private fun calculateArrowPoints(
        point: Offset,
        angle: Float,
        length: Float,
        arrowAngle: Float
    ): Pair<Offset, Offset> {
        val point1 = Offset(
            point.x + length * kotlin.math.cos(angle + arrowAngle).toFloat(),
            point.y + length * kotlin.math.sin(angle + arrowAngle).toFloat()
        )
        
        val point2 = Offset(
            point.x + length * kotlin.math.cos(angle - arrowAngle).toFloat(),
            point.y + length * kotlin.math.sin(angle - arrowAngle).toFloat()
        )
        
        return Pair(point1, point2)
    }
}
