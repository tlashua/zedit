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
                            val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                            roomRect.contains(offset)
                        }
                        onRoomSelected(clickedRoom)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            draggedRoomId = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
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
                                
                                // Calculate new position with constraints
                                val newX = (oldPos.x + modelDragX).coerceIn(
                                    0f,  // Minimum X position
                                    canvasWidth.toFloat() - currentZone.nodeWidth  // Maximum X position
                                )
                                val newY = (oldPos.y + modelDragY).coerceIn(
                                    0f,  // Minimum Y position
                                    canvasHeight.toFloat() - currentZone.nodeHeight  // Maximum Y position
                                )
                                
                                val newPos = Position(x = newX, y = newY)
                                
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
            
            // Draw connections first so they appear behind rooms
            for (room in currentZone.rooms) {
                drawConnections(room, currentZone, density, zoomLevel, selectedRoom)
            }
            
            // Draw rooms on top
            for (room in currentZone.rooms) {
                drawRoom(room, currentZone, density, zoomLevel, room.id == selectedRoom?.id, textMeasurer)
            }
        }
    }
}

private fun getRoomRect(room: Room, zone: Zone, density: Float, zoomLevel: Float): Rect {
    // Convert model coordinates to screen coordinates
    val screenX = room.position.x * density * zoomLevel
    val screenY = room.position.y * density * zoomLevel
    val width = zone.nodeWidth * density * zoomLevel
    val height = zone.nodeHeight * density * zoomLevel
    
    return Rect(
        offset = Offset(screenX, screenY),
        size = Size(width, height)
    )
}

private fun DrawScope.drawRoom(
    room: Room,
    zone: Zone,
    density: Float,
    zoomLevel: Float,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val rect = getRoomRect(room, zone, density, zoomLevel)
    
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

private fun DrawScope.drawConnections(
    room: Room,
    zone: Zone,
    density: Float,
    zoomLevel: Float,
    selectedRoom: Room?
) {
    val sourceRect = getRoomRect(room, zone, density, zoomLevel)
    val sourceCenter = Offset(
        sourceRect.center.x,
        sourceRect.center.y
    )
    
    for ((direction, destId) in room.exits) {
        val destRoom = zone.rooms.find { it.id == destId } ?: continue
        val destRect = getRoomRect(destRoom, zone, density, zoomLevel)
        val destCenter = Offset(
            destRect.center.x,
            destRect.center.y
        )
        
        drawLine(
            color = Color.Gray,
            start = sourceCenter,
            end = destCenter,
            strokeWidth = 2f * zoomLevel
        )
        
        // Convert angles to Float
        val arrowLength = 20f * zoomLevel
        val angle = kotlin.math.atan2(
            (destCenter.y - sourceCenter.y).toFloat(),
            (destCenter.x - sourceCenter.x).toFloat()
        )
        val arrowAngle = (kotlin.math.PI / 6).toFloat() // 30 degrees
        
        val arrowPoint1 = Offset(
            destCenter.x - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat(),
            destCenter.y - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
        )
        val arrowPoint2 = Offset(
            destCenter.x - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat(),
            destCenter.y - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()
        )
        
        drawLine(
            color = Color.Gray,
            start = destCenter,
            end = arrowPoint1,
            strokeWidth = 2f * zoomLevel
        )
        drawLine(
            color = Color.Gray,
            start = destCenter,
            end = arrowPoint2,
            strokeWidth = 2f * zoomLevel
        )
    }
}