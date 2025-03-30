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

    // Calculate drag bounds in pixels
    val density = LocalDensity.current.density
    val dragBounds = remember(canvasSize) {
        Offset(
            canvasSize.x - (roomSize * density),  // Account for room size in screen pixels
            canvasSize.y - (roomSize * density)
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
                .size(canvasSize.x.dp, canvasSize.y.dp)
                .border(1.dp, Color.Red)
                .drawBehind {
                    // Draw vertical grid lines
                    for (x in 0..(size.width / gridSize.toPx()).toInt()) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(x * gridSize.toPx(), 0f),
                            end = Offset(x * gridSize.toPx(), size.height),
                            strokeWidth = 0.5f
                        )
                    }
                    // Draw horizontal grid lines
                    for (y in 0..(size.height / gridSize.toPx()).toInt()) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, y * gridSize.toPx()),
                            end = Offset(size.width, y * gridSize.toPx()),
                            strokeWidth = 0.5f
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 2f)
                    }
                }
        ) {
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