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
import kotlin.math.roundToInt

@Composable
fun ZoneCanvas(
    zone: Zone,
    onZoneChanged: (Zone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    var scale by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    
    val gridSize = 20.dp
    val roomSize = 200f
    val canvasMinSize = 10000f
    
    // Calculate canvas bounds based on room positions
    val bounds = remember(zone.rooms) {
        if (zone.rooms.isEmpty()) {
            Pair(Offset.Zero, Offset(canvasMinSize, canvasMinSize))
        } else {
            zone.rooms.fold(Pair(Offset.Zero, Offset.Zero)) { acc, room ->
                Pair(
                    Offset(
                        minOf(acc.first.x, room.position.x),
                        minOf(acc.first.y, room.position.y)
                    ),
                    Offset(
                        maxOf(acc.second.x, room.position.x),
                        maxOf(acc.second.y, room.position.y)
                    )
                )
            }
        }
    }
    
    // Add padding to bounds and ensure minimum size
    val canvasSize = remember(bounds) {
        Offset(
            maxOf(bounds.second.x + 2000f, canvasMinSize),
            maxOf(bounds.second.y + 2000f, canvasMinSize)
        )
    }

    // Convert canvas size to dp units
    val canvasSizeDp = remember(canvasSize) {
        Offset(
            canvasSize.x / density.density,
            canvasSize.y / density.density
        )
    }

    val gridSizeInPx = with(density) { gridSize.toPx() }
    
    // Calculate actual number of grid lines based on canvas size
    val gridLinesHorizontal = remember(canvasSize) { (canvasSize.x / gridSizeInPx).toInt() }
    val gridLinesVertical = remember(canvasSize) { (canvasSize.y / gridSizeInPx).toInt() }

    // Calculate drag bounds based on actual canvas size, leaving one grid square margin
    val dragBounds = remember(canvasSize, gridSizeInPx) {
        Offset(
            canvasSize.x - gridSizeInPx,
            canvasSize.y - gridSizeInPx
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .size(canvasSizeDp.x.dp, canvasSizeDp.y.dp)
                .border(1.dp, Color.Red)
                .drawBehind {
                    // Draw vertical grid lines
                    for (x in 0..gridLinesHorizontal) {
                        val isMajorLine = x % 10 == 0
                        drawLine(
                            color = if (isMajorLine) Color.Gray else Color.LightGray,
                            start = Offset(x * gridSize.toPx(), 0f),
                            end = Offset(x * gridSize.toPx(), size.height),
                            strokeWidth = if (isMajorLine) 1f else 0.5f
                        )
                    }
                    // Draw horizontal grid lines
                    for (y in 0..gridLinesVertical) {
                        val isMajorLine = y % 10 == 0
                        drawLine(
                            color = if (isMajorLine) Color.Gray else Color.LightGray,
                            start = Offset(0f, y * gridSize.toPx()),
                            end = Offset(size.width, y * gridSize.toPx()),
                            strokeWidth = if (isMajorLine) 1f else 0.5f
                        )
                    }
                }
        ) {
            // Add debug text at the top of the canvas
            SelectionContainer {
                Column {
                    Text(
                        "Grid lines: $gridLinesHorizontal x $gridLinesVertical",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        "Grid size: ${gridSize.value}dp",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            
            for (room in zone.rooms) {
                key(room.id) {
                    var position by remember(room.id) { 
                        mutableStateOf(Offset(room.position.x, room.position.y)) 
                    }
                    
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                            .border(1.dp, Color.Black)
                            .padding(8.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    
                                    val newX = (position.x + dragAmount.x).coerceIn(0f, dragBounds.x)
                                    val newY = (position.y + dragAmount.y).coerceIn(0f, dragBounds.y)
                                    
                                    position = Offset(newX, newY)
                                    
                                    val updatedRooms = zone.rooms.map { r ->
                                        if (r.id == room.id) {
                                            r.copy(position = Position(newX, newY))
                                        } else r
                                    }
                                    onZoneChanged(zone.copy(rooms = updatedRooms))
                                }
                            }
                    ) {
                        Column {
                            Text(room.name)
                            Text(
                                room.id,
                                color = Color.Gray
                            )
                            SelectionContainer {
                                Column {
                                    Text(
                                        "x: ${position.x.roundToInt()}, y: ${position.y.roundToInt()}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        "max: ${dragBounds.x.roundToInt()} x ${dragBounds.y.roundToInt()}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        "canvas: ${canvasSize.x.roundToInt()} x ${canvasSize.y.roundToInt()}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
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