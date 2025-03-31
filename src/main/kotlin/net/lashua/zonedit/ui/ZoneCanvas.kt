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
import androidx.compose.ui.graphics.PathEffect
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
import net.lashua.zonedit.model.*

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
    var currentSelectedRoom by remember { mutableStateOf(selectedRoom) }

    LaunchedEffect(selectedRoom) {
        currentSelectedRoom = selectedRoom
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
                        // Only handle taps if we're not dragging
                        if (draggedRoomId == null && connectionDragState == null) {
                            val clickedRoom = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                roomRect.contains(offset)
                            }
                            currentSelectedRoom = clickedRoom
                            onRoomSelected(clickedRoom)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            println("=== Drag Start ===")
                            println("Initial offset: $offset")
                            println("Selected room: ${currentSelectedRoom?.id}")
                            
                            // First check if we're near any connection point of the selected room
                            if (currentSelectedRoom != null) {
                                val roomRect = getRoomRect(currentSelectedRoom!!, currentZone, density, zoomLevel)
                                val connectionPointSize = 12f * density * zoomLevel
                                
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
                                    println("Starting connection drag from ${currentSelectedRoom!!.id} in direction $direction")
                                    connectionDragState = ConnectionDragState(
                                        sourceRoomId = currentSelectedRoom!!.id,
                                        direction = direction,
                                        currentPoint = offset
                                    )
                                    onConnectionStarted(currentSelectedRoom!!, direction)
                                    return@detectDragGestures
                                }
                            }
                            
                            // Only try to drag room if we're not near any connection point
                            val roomToDrag = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                roomRect.contains(offset)
                            }
                            
                            if (roomToDrag != null) {
                                println("Starting room drag: ${roomToDrag.id}")
                                draggedRoomId = roomToDrag.id
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (connectionDragState != null) {
                                connectionDragState = connectionDragState?.copy(
                                    currentPoint = connectionDragState!!.currentPoint + dragAmount
                                )
                            } else if (draggedRoomId != null) {
                                // Handle room dragging
                                val modelDragX = dragAmount.x / (density * zoomLevel)
                                val modelDragY = dragAmount.y / (density * zoomLevel)
                                
                                val room = currentZone.rooms.first { it.id == draggedRoomId }
                                val oldPos = room.position
                                
                                val newX = (oldPos.x + modelDragX).coerceIn(0f, canvasWidth.toFloat() - currentZone.nodeWidth)
                                val newY = (oldPos.y + modelDragY).coerceIn(0f, canvasHeight.toFloat() - currentZone.nodeHeight)
                                
                                val updatedRooms = currentZone.rooms.map { r ->
                                    if (r.id == draggedRoomId) r.copy(position = Position(x = newX, y = newY))
                                    else r
                                }
                                currentZone = currentZone.copy(rooms = updatedRooms)
                                onZoneChanged(currentZone)
                            }
                        },
                        onDragEnd = {
                            println("=== Drag End ===")
                            println("Final connection state: $connectionDragState")
                            println("Final dragged room: $draggedRoomId")
                            
                            connectionDragState?.let { state ->
                                println("Checking for target room at: ${state.currentPoint}")
                                val targetRoom = currentZone.rooms.firstOrNull { room ->
                                    val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                    val contains = roomRect.contains(state.currentPoint)
                                    println("Checking room ${room.id}: contains=${contains}")
                                    contains && room.id != state.sourceRoomId
                                }
                                println("Target room found: ${targetRoom?.id}")
                                
                                if (targetRoom != null) {
                                    val updatedRooms = currentZone.rooms.map { room ->
                                        if (room.id == state.sourceRoomId) {
                                            room.copy(
                                                exits = room.exits + (state.direction to targetRoom.id)
                                            )
                                        } else room
                                    }
                                    currentZone = currentZone.copy(rooms = updatedRooms)
                                    onZoneChanged(currentZone)
                                }
                            }
                            
                            connectionDragState = null
                            draggedRoomId = null
                        }
                    )
                }
        ) {
            drawGrid(baseGridSize.toPx(), zoomLevel, size)
            
            // Draw existing connections
            for (room in currentZone.rooms) {
                drawConnections(room, currentZone, density, zoomLevel)
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
                drawRoom(room, currentZone, density, zoomLevel, room.id == currentSelectedRoom?.id, textMeasurer)
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
    zoomLevel: Float
) {
    val sourceRect = getRoomRect(room, zone, density, zoomLevel)
    val sourceCenter = Offset(
        sourceRect.center.x,
        sourceRect.center.y
    )
    
    for ((_, destId) in room.exits) {
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
            (destCenter.y - sourceCenter.y),
            (destCenter.x - sourceCenter.x)
        )
        val arrowAngle = (kotlin.math.PI / 6).toFloat() // 30 degrees
        
        val arrowPoint1 = Offset(
            destCenter.x - arrowLength * kotlin.math.cos(angle - arrowAngle),
            destCenter.y - arrowLength * kotlin.math.sin(angle - arrowAngle)
        )
        val arrowPoint2 = Offset(
            destCenter.x - arrowLength * kotlin.math.cos(angle + arrowAngle),
            destCenter.y - arrowLength * kotlin.math.sin(angle + arrowAngle)
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

private fun isNearPoint(point: Offset, target: Offset, threshold: Float): Boolean {
    val distance = kotlin.math.sqrt(
        (point.x - target.x) * (point.x - target.x) +
        (point.y - target.y) * (point.y - target.y)
    )
    return distance <= threshold
}
