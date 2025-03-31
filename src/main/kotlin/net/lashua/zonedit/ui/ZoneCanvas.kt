package net.lashua.zonedit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone

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
    val density = LocalDensity.current.density
    val baseGridSize = 20.dp
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    
    var currentZone by remember { mutableStateOf(zone) }
    var draggedRoomId by remember { mutableStateOf<String?>(null) }
    
    // Update currentZone when zone changes from outside
    LaunchedEffect(zone) {
        currentZone = zone
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState)
    ) {
        Canvas(
            modifier = Modifier
                .size(
                    (canvasWidth * zoomLevel).dp,
                    (canvasHeight * zoomLevel).dp
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val clickedRoom = currentZone.rooms.firstOrNull { room ->
                            val roomRect = getRoomRect(room, density, zoomLevel)
                            roomRect.contains(offset)
                        }
                        onRoomSelected(clickedRoom)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            draggedRoomId = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, density, zoomLevel)
                                roomRect.contains(offset)
                            }?.id
                            println("Started dragging room: $draggedRoomId")
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (draggedRoomId != null) {
                                // Convert screen coordinates to model coordinates
                                val modelDragX = dragAmount.x / (zoomLevel * density)
                                val modelDragY = dragAmount.y / (zoomLevel * density)
                                
                                val room = currentZone.rooms.first { it.id == draggedRoomId }
                                val oldPos = room.position
                                val newPos = Position(
                                    x = oldPos.x + modelDragX,
                                    y = oldPos.y + modelDragY
                                )
                                
                                println("Room ${room.id} position update:")
                                println("  Old position: (${oldPos.x}, ${oldPos.y})")
                                println("  Drag amount: ($modelDragX, $modelDragY)")
                                println("  New position: (${newPos.x}, ${newPos.y})")
                                
                                val updatedRooms = currentZone.rooms.map { r ->
                                    if (r.id == draggedRoomId) {
                                        r.copy(position = newPos)
                                    } else r
                                }
                                currentZone = currentZone.copy(rooms = updatedRooms)
                                onZoneChanged(currentZone)
                            }
                        },
                        onDragEnd = {
                            println("Finished dragging room: $draggedRoomId")
                            draggedRoomId = null
                        }
                    )
                }
        ) {
            drawGrid(baseGridSize.toPx(), zoomLevel, size)
            
            for (room in currentZone.rooms) {
                drawRoom(room, density, zoomLevel, room.id == selectedRoom?.id, textMeasurer)
            }
        }
    }
}

private fun getRoomRect(room: Room, density: Float, zoomLevel: Float): Rect {
    // Convert model coordinates to screen coordinates
    val screenX = room.position.x * density * zoomLevel
    val screenY = room.position.y * density * zoomLevel
    val size = 100f * density * zoomLevel
    
    return Rect(
        offset = Offset(screenX, screenY),
        size = Size(size, size)
    )
}

private fun DrawScope.drawRoom(
    room: Room,
    density: Float,
    zoomLevel: Float,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val rect = getRoomRect(room, density, zoomLevel)
    
    drawRect(
        color = Color.White,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Fill
    )
    
    drawRect(
        color = if (isSelected) Color.Blue else Color.Black,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = if (isSelected) 2f else 1f)
    )
    
    val textStyle = TextStyle(
        fontSize = (14 * zoomLevel).sp,
        color = Color.Black
    )
    
     drawText(
        textMeasurer = textMeasurer,
        text = room.name,
        topLeft = rect.topLeft + Offset(8f * zoomLevel, 8f * zoomLevel),
        style = textStyle
    )
}

private fun DrawScope.drawGrid(gridSizePx: Float, zoomLevel: Float, size: Size) {
    val gridSize = gridSizePx * zoomLevel
    val horizontalLines = (size.height / gridSize).toInt()
    val verticalLines = (size.width / gridSize).toInt()
    
    repeat(horizontalLines + 1) { i ->
        val y = i * gridSize
        val isMajor = i % 10 == 0
        drawLine(
            color = if (isMajor) Color.Gray else Color.LightGray,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
    }
    
    repeat(verticalLines + 1) { i ->
        val x = i * gridSize
        val isMajor = i % 10 == 0
        drawLine(
            color = if (isMajor) Color.Gray else Color.LightGray,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = if (isMajor) 1f else 0.5f
        )
    }
}