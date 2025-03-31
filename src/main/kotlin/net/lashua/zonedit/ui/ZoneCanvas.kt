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
    canvasWidth: Int = 1000,
    canvasHeight: Int = 1000,
    selectedRoom: Room? = null,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val baseGridSize = 20.dp
    val canvasMinSize = Offset(canvasWidth.toFloat(), canvasHeight.toFloat())
    
    val bounds = remember(zone.rooms) {
        if (zone.rooms.isEmpty()) {
            Pair(Offset.Zero, Offset(canvasMinSize.x, canvasMinSize.y))
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
    
    val canvasSize = remember(bounds, canvasWidth, canvasHeight) {
        Offset(
            maxOf(bounds.second.x + 100f, canvasMinSize.x),
            maxOf(bounds.second.y + 100f, canvasMinSize.y)
        )
    }

    val gridLinesHorizontal = remember(canvasSize) { 
        (canvasSize.x / baseGridSize.value).toInt() 
    }
    val gridLinesVertical = remember(canvasSize) { 
        (canvasSize.y / baseGridSize.value).toInt() 
    }

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
                    val gridSizePx = (baseGridSize * zoomLevel).toPx()
                    
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
            // Single debug info display
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(4.dp)
            ) {
                Column {
                    Text(
                        "(0,0)",
                        color = Color.Gray,
                        fontSize = (10 * zoomLevel).sp
                    )
                    Text(
                        "Rooms: ${zone.rooms.size}",
                        color = Color.Gray,
                        fontSize = (10 * zoomLevel).sp
                    )
                    Text(
                        "Grid: ${(baseGridSize.value * zoomLevel).roundToInt()}dp",
                        color = Color.Gray,
                        fontSize = (10 * zoomLevel).sp
                    )
                }
            }

            // Room rendering
            for (room in zone.rooms) {
                key(room.id) {
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
                            .clickable { onRoomSelected(room) }
                            .border(
                                width = if (selectedRoom?.id == room.id) 2.dp else 1.dp,
                                color = if (selectedRoom?.id == room.id) Color.Blue else Color.Black
                            )
                            .padding((8 * zoomLevel).dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    
                                    // Calculate new position
                                    val newX = (position.x + dragAmount.x / (density * zoomLevel))
                                        .coerceIn(0f, canvasSize.x / density - 100f)
                                    val newY = (position.y + dragAmount.y / (density * zoomLevel))
                                        .coerceIn(0f, canvasSize.y / density - 100f)
                                    
                                    position = Offset(newX, newY)
                                    
                                    // Create a new list with the updated room
                                    val updatedRooms = zone.rooms.map { r ->
                                        if (r.id == room.id) {
                                            r.copy(position = Position(
                                                x = newX * density,
                                                y = newY * density
                                            ))
                                        } else {
                                            r
                                        }
                                    }
                                    
                                    println("Updating room ${room.id}. Total rooms: ${updatedRooms.size}")
                                    println("Room positions: ${updatedRooms.map { "${it.id}: (${it.position.x}, ${it.position.y})" }}")
                                    
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
                            Spacer(Modifier.weight(1f))
                            Text(
                                "(${(position.x * density).roundToInt()}, ${(position.y * density).roundToInt()})",
                                color = Color.Gray,
                                fontSize = (10 * zoomLevel).sp,
                                modifier = Modifier.align(Alignment.End)
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