package net.lashua.zonedit.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Position
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ZoneCanvas(
    zone: Zone,
    zoomLevel: Float = 1f,
    onZoneChanged: (Zone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current.density
    
    val gridSize = (20 * zoomLevel).dp
    val canvasMinSize = (1000 * zoomLevel).dp
    
    // Calculate canvas bounds based on room positions (in dp)
    val bounds = remember(zone.rooms) {
        if (zone.rooms.isEmpty()) {
            Pair(Offset.Zero, Offset(canvasMinSize.value, canvasMinSize.value))
        } else {
            zone.rooms.fold(Pair(Offset.Zero, Offset.Zero)) { acc, room ->
                Pair(
                    Offset(
                        minOf(acc.first.x, room.position.x / density),
                        minOf(acc.first.y, room.position.y / density)
                    ),
                    Offset(
                        maxOf(acc.second.x, room.position.x / density),
                        maxOf(acc.second.y, room.position.y / density)
                    )
                )
            }
        }
    }
    
    // Add padding and ensure minimum size (in dp)
    // Round up to nearest grid size multiple to ensure grid lines align perfectly
    val canvasSize = remember(bounds) {
        Offset(
            ceil(maxOf(bounds.second.x + 100f, canvasMinSize.value) / gridSize.value) * gridSize.value,
            ceil(maxOf(bounds.second.y + 100f, canvasMinSize.value) / gridSize.value) * gridSize.value
        )
    }

    // Calculate number of grid lines - now exactly matching canvas size
    val gridLinesHorizontal = remember(canvasSize) { (canvasSize.x / gridSize.value).toInt() }
    val gridLinesVertical = remember(canvasSize) { (canvasSize.y / gridSize.value).toInt() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .size(
                    (canvasSize.x * zoomLevel).dp,
                    (canvasSize.y * zoomLevel).dp
                )
                .border(1.dp, Color.Red)
                .drawBehind {
                    val gridSizePx = gridSize.toPx()
                    
                    // Draw vertical grid lines
                    repeat(gridLinesHorizontal + 1) { x ->
                        val isMajorLine = x % 10 == 0
                        drawLine(
                            color = if (isMajorLine) Color.Gray else Color.LightGray,
                            start = Offset(x * gridSizePx, 0f),
                            end = Offset(x * gridSizePx, size.height),
                            strokeWidth = if (isMajorLine) 1f else 0.5f
                        )
                    }
                    // Draw horizontal grid lines
                    repeat(gridLinesVertical + 1) { y ->
                        val isMajorLine = y % 10 == 0
                        drawLine(
                            color = if (isMajorLine) Color.Gray else Color.LightGray,
                            start = Offset(0f, y * gridSizePx),
                            end = Offset(size.width, y * gridSizePx),
                            strokeWidth = if (isMajorLine) 1f else 0.5f
                        )
                    }
                }
        ) {
            // Debug information at origin
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    "(0,0)",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    "Grid: ${gridSize.value}dp (${gridLinesHorizontal}x${gridLinesVertical} lines)",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    "Canvas: ${canvasSize.x.roundToInt()}dp x ${canvasSize.y.roundToInt()}dp",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            
            for (room in zone.rooms) {
                key(room.id) {
                    // Store position in dp
                    var position by remember(room.id) { 
                        mutableStateOf(Offset(room.position.x / density, room.position.y / density)) 
                    }
                    
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(
                                ((position.x * density * zoomLevel).roundToInt()),
                                ((position.y * density * zoomLevel).roundToInt())
                            )}
                            .size(
                                (100 * zoomLevel).dp,
                                (100 * zoomLevel).dp
                            )
                            .border(1.dp, Color.Black)
                            .padding((8 * zoomLevel).dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    
                                    val newX = (position.x + dragAmount.x / (density * zoomLevel))
                                        .coerceIn(0f, canvasSize.x - 100f)
                                    val newY = (position.y + dragAmount.y / (density * zoomLevel))
                                        .coerceIn(0f, canvasSize.y - 100f)
                                    
                                    position = Offset(newX, newY)
                                    
                                    val updatedRooms = zone.rooms.map { r ->
                                        if (r.id == room.id) {
                                            r.copy(position = Position(
                                                newX * density,
                                                newY * density
                                            ))
                                        } else r
                                    }
                                    onZoneChanged(zone.copy(rooms = updatedRooms))
                                }
                            }
                    ) {
                        Column {
                            Text(
                                room.name,
                                fontSize = (14 * zoomLevel).sp
                            )
                            Text(
                                room.id,
                                color = Color.Gray,
                                fontSize = (10 * zoomLevel).sp
                            )
                        }
                    }
                }
            }
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(verticalScrollState)
        )
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart),
            adapter = rememberScrollbarAdapter(horizontalScrollState)
        )
    }
}