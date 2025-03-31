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
import net.lashua.zonedit.model.ExitDirection
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerInputChange
import net.lashua.zonedit.model.ConnectionDragState

@Composable
fun ZoneCanvas(
    zone: Zone,
    zoomLevel: Float = 1f,
    canvasWidth: Int = 1000,
    canvasHeight: Int = 1000,
    selectedRoom: Room? = null,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val baseGridSize = 20.dp
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    
    var currentZone by remember { mutableStateOf(zone) }
    var draggedRoomId by remember { mutableStateOf<String?>(null) }
    var connectionDragState by remember { mutableStateOf<ConnectionDragState?>(null) }
    
    LaunchedEffect(selectedRoom) {
        println("ZoneCanvas - Selected room updated: ${selectedRoom?.id}")
    }

    LaunchedEffect(zone) {
        println("ZoneCanvas - Zone updated: ${zone.rooms.map { it.id }}")
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
                            // First check if we're starting from a connection point
                            if (selectedRoom != null) {
                                val roomRect = getRoomRect(selectedRoom, currentZone, density, zoomLevel)
                                val connectionPointSize = 12f * density * zoomLevel
                                
                                // Calculate connection points
                                val northPoint = Offset(roomRect.center.x, roomRect.top)
                                val southPoint = Offset(roomRect.center.x, roomRect.bottom)
                                val eastPoint = Offset(roomRect.right, roomRect.center.y)
                                val westPoint = Offset(roomRect.left, roomRect.center.y)
                                
                                val direction = when {
                                    isNearPoint(offset, northPoint, connectionPointSize) -> ExitDirection.NORTH
                                    isNearPoint(offset, southPoint, connectionPointSize) -> ExitDirection.SOUTH
                                    isNearPoint(offset, eastPoint, connectionPointSize) -> ExitDirection.EAST
                                    isNearPoint(offset, westPoint, connectionPointSize) -> ExitDirection.WEST
                                    else -> null
                                }
                                
                                if (direction != null) {
                                    println("Starting connection from ${selectedRoom.id} in direction $direction")
                                    onConnectionStarted(selectedRoom, direction)
                                    connectionDragState = ConnectionDragState(
                                        sourceRoomId = selectedRoom.id,
                                        direction = direction,
                                        currentPoint = offset
                                    )
                                    return@detectDragGestures
                                }
                            }
                            
                            // Only handle room dragging if we're not on a connection point
                            val roomToDrag = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                // Don't start room drag if we're near any connection points
                                if (room.id == selectedRoom?.id) {
                                    val connectionPointSize = 12f * density * zoomLevel
                                    val northPoint = Offset(roomRect.center.x, roomRect.top)
                                    val southPoint = Offset(roomRect.center.x, roomRect.bottom)
                                    val eastPoint = Offset(roomRect.right, roomRect.center.y)
                                    val westPoint = Offset(roomRect.left, roomRect.center.y)
                                    
                                    if (isNearPoint(offset, northPoint, connectionPointSize) ||
                                        isNearPoint(offset, southPoint, connectionPointSize) ||
                                        isNearPoint(offset, eastPoint, connectionPointSize) ||
                                        isNearPoint(offset, westPoint, connectionPointSize)) {
                                        return@firstOrNull false  // Return false instead of null
                                    }
                                }
                                roomRect.contains(offset)  // Return the result of contains()
                            }
                            if (roomToDrag != null) {
                                draggedRoomId = roomToDrag.id
                                onRoomSelected(roomToDrag)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            connectionDragState?.let { state ->
                                connectionDragState = state.copy(
                                    currentPoint = state.currentPoint + dragAmount
                                )
                            } ?: run {
                                draggedRoomId?.let { id ->
                                    // Only handle room dragging if we're not in connection mode
                                    val modelDragX = dragAmount.x / (density * zoomLevel)
                                    val modelDragY = dragAmount.y / (density * zoomLevel)
                                    
                                    val room = currentZone.rooms.first { it.id == id }
                                    val oldPos = room.position
                                    
                                    val newX = (oldPos.x + modelDragX).coerceIn(
                                        0f,
                                        canvasWidth.toFloat() - currentZone.nodeWidth
                                    )
                                    val newY = (oldPos.y + modelDragY).coerceIn(
                                        0f,
                                        canvasHeight.toFloat() - currentZone.nodeHeight
                                    )
                                    
                                    val updatedRooms = currentZone.rooms.map { r ->
                                        if (r.id == id) r.copy(position = Position(x = newX, y = newY))
                                        else r
                                    }
                                    currentZone = currentZone.copy(rooms = updatedRooms)
                                    onZoneChanged(currentZone)
                                }
                            }
                        },
                        onDragEnd = {
                            println("Drag ended")
                            println("Connection state: $connectionDragState")
                            println("Dragged room id: $draggedRoomId")
                            draggedRoomId = null
                            connectionDragState = null
                        }
                    )
                }
        ) {
            drawGrid(baseGridSize.toPx(), zoomLevel, size)
            
            // Draw existing connections
            for (room in currentZone.rooms) {
                drawConnections(room, currentZone, density, zoomLevel, selectedRoom)
            }
            
            // Draw connection preview if dragging
            connectionDragState?.let { state ->
                val sourceRoom = currentZone.rooms.find { it.id == state.sourceRoomId } ?: return@let
                val sourceRect = getRoomRect(sourceRoom, currentZone, density, zoomLevel)
                val sourcePoint = when (state.direction) {
                    ExitDirection.NORTH -> Offset(sourceRect.center.x, sourceRect.top)
                    ExitDirection.SOUTH -> Offset(sourceRect.center.x, sourceRect.bottom)
                    ExitDirection.EAST -> Offset(sourceRect.right, sourceRect.center.y)
                    ExitDirection.WEST -> Offset(sourceRect.left, sourceRect.center.y)
                    ExitDirection.UP -> Offset(sourceRect.center.x, sourceRect.top)
                    ExitDirection.DOWN -> Offset(sourceRect.center.x, sourceRect.bottom)
                }
                
                drawLine(
                    color = Color.Blue,
                    start = sourcePoint,
                    end = state.currentPoint,
                    strokeWidth = 2f * zoomLevel,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
            
            // Draw rooms last so they appear on top
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
    
    // Draw room background
    drawRect(
        color = Color.White,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Fill
    )
    
    // Draw room border
    drawRect(
        color = if (isSelected) Color.Blue else Color.Black,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = if (isSelected) 2f else 1f)
    )
    
    // Draw room name
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
    
    // Draw connection points if selected
    if (isSelected) {
        val connectionPointSize = 12f * density * zoomLevel
        
        // North connection point
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.center.x, rect.top),
            style = Fill
        )
        
        // South connection point
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.center.x, rect.bottom),
            style = Fill
        )
        
        // East connection point
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.right, rect.center.y),
            style = Fill
        )
        
        // West connection point
        drawCircle(
            color = Color.Blue,
            radius = connectionPointSize / 2,
            center = Offset(rect.left, rect.center.y),
            style = Fill
        )
    }
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

private data class ConnectionDragState(
    val sourceRoomId: String,
    val direction: ExitDirection,
    val currentPoint: Offset
)

private fun isNearPoint(point: Offset, target: Offset, threshold: Float): Boolean {
    val distance = kotlin.math.sqrt(
        (point.x - target.x) * (point.x - target.x) +
        (point.y - target.y) * (point.y - target.y)
    )
    return distance <= threshold
}

private fun calculateDistance(point1: Offset, point2: Offset): Float {
    return kotlin.math.sqrt(
        (point1.x - point2.x) * (point1.x - point2.x) +
        (point1.y - point2.y) * (point1.y - point2.y)
    )
}
