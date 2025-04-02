package net.lashua.zonedit.ui.graph

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import net.lashua.zonedit.model.ExitData
import net.lashua.zonedit.model.RoomData

/**
 * Renderer for drawing a graph in the graph editor.
 */
object GraphRenderer {
    /**
     * Draws a graph in the graph editor.
     * 
     * @param drawScope The DrawScope to draw in
     * @param graph The graph to draw
     * @param textMeasurer The TextMeasurer for drawing text
     * @param zoomLevel The current zoom level
     * @param showConnectionPoints Whether to show connection points
     */
    fun draw(
        drawScope: DrawScope,
        graph: VisualGraph<RoomData, ExitData>,
        textMeasurer: TextMeasurer,
        zoomLevel: Float,
        showConnectionPoints: Boolean = false
    ) {
        drawScope.apply {
            // Draw grid
            drawGrid(size, zoomLevel)
            
            // Draw edges first so they appear behind nodes
            for (edge in graph.edges) {
                EdgeRenderer.draw(this, edge, zoomLevel)
            }
            
            // Draw nodes on top
            for (node in graph.nodes) {
                NodeRenderer.draw(this, node, textMeasurer, zoomLevel, showConnectionPoints)
            }
        }
    }
    
    /**
     * Draws a grid in the graph editor.
     * 
     * @param size The size of the canvas
     * @param zoomLevel The current zoom level
     */
    private fun DrawScope.drawGrid(size: Size, zoomLevel: Float) {
        val gridSize = 20f * zoomLevel
        val horizontalLines = (size.height / gridSize).toInt()
        val verticalLines = (size.width / gridSize).toInt()
        
        // Draw horizontal lines
        repeat(horizontalLines + 1) { i ->
            val y = i * gridSize
            val isMajor = i % 10 == 0
            drawLine(
                color = if (isMajor) Color.Gray else Color.LightGray,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = if (isMajor) 1f else 0.5f
            )
        }
        
        // Draw vertical lines
        repeat(verticalLines + 1) { i ->
            val x = i * gridSize
            val isMajor = i % 10 == 0
            drawLine(
                color = if (isMajor) Color.Gray else Color.LightGray,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = if (isMajor) 1f else 0.5f
            )
        }
    }
}
