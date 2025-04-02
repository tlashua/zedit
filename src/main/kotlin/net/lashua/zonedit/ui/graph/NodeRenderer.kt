package net.lashua.zonedit.ui.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import net.lashua.zonedit.model.RoomData

/**
 * Renderer for drawing nodes in the graph editor.
 */
object NodeRenderer {
    /**
     * Draws a node in the graph editor.
     * 
     * @param drawScope The DrawScope to draw in
     * @param node The node to draw
     * @param textMeasurer The TextMeasurer for drawing text
     * @param zoomLevel The current zoom level
     * @param showConnectionPoints Whether to show connection points
     */
    fun draw(
        drawScope: DrawScope,
        node: VisualNode<RoomData>,
        textMeasurer: TextMeasurer,
        zoomLevel: Float,
        showConnectionPoints: Boolean = false
    ) {
        drawScope.apply {
            // Draw node background
            drawRect(
                color = Color.White,
                topLeft = Offset(node.x, node.y),
                size = node.size,
                style = Fill
            )
            
            // Draw node border
            drawRect(
                color = if (node.isSelected) Color.Blue else Color.Black,
                topLeft = Offset(node.x, node.y),
                size = node.size,
                style = Stroke(width = if (node.isSelected) 2f else 1f)
            )
            
            // Only attempt to draw text if there's enough space
            if (node.width >= 10 && node.height >= 10) {
                try {
                    // Create base font sizes
                    val nameFontSize = (14 * zoomLevel).sp
                    val idFontSize = (10 * zoomLevel).sp
                    
                    // Measure text dimensions
                    val nameStyle = TextStyle(fontSize = nameFontSize, color = Color.Black)
                    val idStyle = TextStyle(fontSize = idFontSize, color = Color.Gray)
                    
                    val nameMeasure = textMeasurer.measure(node.data.name, nameStyle)
                    
                    // Calculate vertical spacing between name and id
                    val verticalSpacing = 4f * zoomLevel
                    
                    // Draw name
                    drawText(
                        textMeasurer = textMeasurer,
                        text = node.data.name,
                        topLeft = Offset(node.x + 8f * zoomLevel, node.y + 8f * zoomLevel),
                        style = nameStyle
                    )
                    
                    // Draw ID below name with proper spacing
                    drawText(
                        textMeasurer = textMeasurer,
                        text = node.id,
                        topLeft = Offset(
                            node.x + 8f * zoomLevel,
                            node.y + 8f * zoomLevel + nameMeasure.size.height + verticalSpacing
                        ),
                        style = idStyle
                    )
                    
                    // Draw connection points if selected
                    if (node.isSelected && showConnectionPoints) {
                        drawConnectionPoints(node)
                    }
                } catch (e: IllegalArgumentException) {
                    // Skip text drawing if there's an error
                }
            }
        }
    }
    
    /**
     * Draws connection points for a node.
     * 
     * @param node The node to draw connection points for
     */
    private fun DrawScope.drawConnectionPoints(node: VisualNode<RoomData>) {
        val connectionPointSize = 6f
        val bounds = node.bounds
        
        // Cardinal direction points (N,S,E,W)
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize,
            center = Offset(bounds.center.x, bounds.top),
            style = Fill
        )
        
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize,
            center = Offset(bounds.center.x, bounds.bottom),
            style = Fill
        )
        
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize,
            center = Offset(bounds.right, bounds.center.y),
            style = Fill
        )
        
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize,
            center = Offset(bounds.left, bounds.center.y),
            style = Fill
        )
        
        // UP/DOWN connection points
        val upColor = Color.Green
        val downColor = Color.Green
        
        // Top corners (UP)
        drawCircle(
            color = upColor,
            radius = connectionPointSize,
            center = Offset(bounds.right, bounds.top),
            style = Fill
        )
        
        drawCircle(
            color = upColor,
            radius = connectionPointSize,
            center = Offset(bounds.left, bounds.top),
            style = Fill
        )
        
        // Bottom corners (DOWN)
        drawCircle(
            color = downColor,
            radius = connectionPointSize,
            center = Offset(bounds.right, bounds.bottom),
            style = Fill
        )
        
        drawCircle(
            color = downColor,
            radius = connectionPointSize,
            center = Offset(bounds.left, bounds.bottom),
            style = Fill
        )
    }
}
