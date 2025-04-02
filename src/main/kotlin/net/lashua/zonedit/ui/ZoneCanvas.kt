package net.lashua.zonedit.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lashua.zonedit.model.*
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class ConnectionDragState(
    val sourceRoomId: String,
    val direction: ExitDirection,
    val currentPoint: Offset,
    val sourceCorner: String // "LEFT" or "RIGHT"
)

private val log = LoggerFactory.getLogger("net.lashua.zonedit.ui.ZoneCanvas")

@Composable
fun ZoneCanvas(
    zone: Zone,
    zoomLevel: Float = 1f,
    canvasWidthDp: Dp,
    canvasHeightDp: Dp,
    selectedRoom: Room? = null,
    onZoneChanged: (Zone) -> Unit,
    onRoomSelected: (Room?) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    onPointerPositionChanged: (Offset?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val baseGridSize = 20.dp
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val connectionManager = remember { ConnectionManager() }

    var currentZone by remember { mutableStateOf(zone) }
    var draggedRoomId by remember { mutableStateOf<String?>(null) }
    var connectionDragState by remember { mutableStateOf<ConnectionDragState?>(null) }
    var currentSelectedRoom by remember { mutableStateOf(selectedRoom) }
    var lastPosition = remember { mutableStateOf<Position?>(null) }
    var contextMenuConnection by remember { mutableStateOf<Triple<Room, ExitDirection, Room>?>(null) }
    var contextMenuPosition by remember { mutableStateOf<DpOffset?>(null) }

    LaunchedEffect(canvasWidthDp, canvasHeightDp) {
        log.debug("Canvas dimensions updated - width: {}dp, height: {}dp", 
            canvasWidthDp.value, canvasHeightDp.value)
    }

    LaunchedEffect(selectedRoom) {
        currentSelectedRoom = selectedRoom
        log.trace("ZoneCanvas - Selected room updated: ${selectedRoom?.id}")
    }

    LaunchedEffect(zone) {
        log.trace("ZoneCanvas - Zone updated: {}", zone.rooms.map { it.id })
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
                .size(canvasWidthDp, canvasHeightDp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            onPointerPositionChanged(position)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Keep existing left-click logic
                            if (draggedRoomId == null && connectionDragState == null) {
                                val clickedRoom = currentZone.rooms.firstOrNull { room ->
                                    val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                    roomRect.contains(offset)
                                }
                                log.debug("Room clicked: ${clickedRoom?.id}")
                                currentSelectedRoom = clickedRoom
                                onRoomSelected(clickedRoom)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (!event.buttons.isSecondaryPressed) {
                                continue
                            }
                            
                            val offset = event.changes.first().position
                            log.debug("Right click detected at coordinates: ({}, {})", offset.x, offset.y)
                            
                            var connectionFound = false
                            // Check for connections near the click
                            for (room in currentZone.rooms) {
                                if (connectionFound) break
                                
                                log.debug("Processing room {} with exits: {}", room.id, room.exits)
                                
                                for ((exitDir, destId) in room.exits) {
                                    val destRoom = currentZone.rooms.find { it.id == destId }
                                    if (destRoom == null) {
                                        log.warn("Invalid connection state - destination room {} not found for exit {} from room {}", 
                                            destId, exitDir, room.id)
                                        continue
                                    }
                                    
                                    log.debug("Checking connection: {} --[{}]--> {}", 
                                        room.id, exitDir, destRoom.id)
                                    
                                    // Modified connection processing logic
                                    val shouldProcess = when (exitDir) {
                                        ExitDirection.NORTH, ExitDirection.SOUTH -> 
                                            room.position.y >= destRoom.position.y
                                        ExitDirection.EAST, ExitDirection.WEST -> 
                                            room.id <= destRoom.id
                                        ExitDirection.UP, ExitDirection.DOWN -> 
                                            true  // Always process UP/DOWN connections
                                    }
                                    
                                    if (shouldProcess) {
                                        val sourceRect = getRoomRect(room, currentZone, density, zoomLevel)
                                        val destRect = getRoomRect(destRoom, currentZone, density, zoomLevel)
                                        
                                        val (sourcePoint, destPoint) = getConnectionPoints(
                                            exitDir, sourceRect, destRect, room, destRoom
                                        )
                                        
                                        log.debug("Connection points - source: ({}, {}), dest: ({}, {})", 
                                            sourcePoint.x, sourcePoint.y, destPoint.x, destPoint.y)
                                        
                                        val hitDetectionDistance = when (exitDir) {
                                            ExitDirection.UP, ExitDirection.DOWN -> 100f * density * zoomLevel
                                            else -> 80f * density * zoomLevel
                                        }
                                        
                                        if (isNearLine(offset, sourcePoint, destPoint, hitDetectionDistance)) {
                                            log.debug("Hit detected on connection: {} --[{}]--> {}", 
                                                room.id, exitDir, destRoom.id)
                                            connectionFound = true
                                            contextMenuConnection = Triple(room, exitDir, destRoom)
                                            contextMenuPosition = DpOffset(
                                                x = (offset.x / density).dp,
                                                y = (offset.y / density).dp
                                            )
                                            // Only consume the event if we found a connection
                                            event.changes.first().consume()
                                            break
                                        }
                                    }
                                }
                            }
                            
                            if (!connectionFound) {
                                log.debug("No connections found near click position: ({}, {})", offset.x, offset.y)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            log.debug("=== Drag Start ===")
                            log.debug("Initial offset: $offset")
                            log.debug("Selected room: ${currentSelectedRoom?.id}")

                            // First check if we're near any connection point of the selected room
                            if (currentSelectedRoom != null) {
                                val roomRect = getRoomRect(currentSelectedRoom!!, currentZone, density, zoomLevel)
                                val connectionPointSize = 12f * density * zoomLevel

                                // Let's add more debug logging to see what's happening
                                log.debug("Checking connection points for room: ${currentSelectedRoom?.id}")
                                log.debug("Current exits: {}", currentSelectedRoom?.exits)
                                
                                val direction = when {
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.center.x, roomRect.top),
                                        connectionPointSize
                                    ) -> ExitDirection.NORTH to null
                                    
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.center.x, roomRect.bottom),
                                        connectionPointSize
                                    ) -> ExitDirection.SOUTH to null
                                    
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.right, roomRect.center.y),
                                        connectionPointSize
                                    ) -> ExitDirection.EAST to null
                                    
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.left, roomRect.center.y),
                                        connectionPointSize
                                    ) -> ExitDirection.WEST to null
                                    
                                    // UP points - both left and right corners
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.right, roomRect.top),
                                        connectionPointSize
                                    ) ||
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.left, roomRect.top),
                                        connectionPointSize
                                    ) -> {
                                        log.debug("UP point detected")
                                        ExitDirection.UP to (
                                            if (isNearPoint(
                                                offset,
                                                Offset(roomRect.left, roomRect.top),
                                                connectionPointSize
                                            ))
                                            "LEFT" else "RIGHT"
                                        )
                                    }
                                    
                                    // DOWN points - both left and right corners
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.right, roomRect.bottom),
                                        connectionPointSize
                                    ) ||
                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.left, roomRect.bottom),
                                        connectionPointSize
                                    ) -> {
                                        log.debug("DOWN point detected")
                                        ExitDirection.DOWN to (
                                            if (isNearPoint(
                                                offset,
                                                Offset(roomRect.left, roomRect.bottom),
                                                connectionPointSize
                                            ))
                                            "LEFT" else "RIGHT"
                                        )
                                    }

                                    else -> null to null
                                }

                                log.debug("Direction detected: {}", direction)

                                if (direction.first != null) {
                                    log.debug(
                                        "Starting connection drag from {} in direction {}",
                                        currentSelectedRoom?.id,
                                        direction.first
                                    )
                                    connectionDragState = ConnectionDragState(
                                        sourceRoomId = currentSelectedRoom!!.id,
                                        direction = direction.first!!,
                                        currentPoint = offset,
                                        sourceCorner = direction.second ?: "RIGHT"
                                    )
                                    onConnectionStarted(currentSelectedRoom!!, direction.first!!)
                                    return@detectDragGestures
                                }
                            }

                            // Only try to drag room if we're not near any connection point
                            val roomToDrag = currentZone.rooms.firstOrNull { room ->
                                val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                roomRect.contains(offset)
                            }

                            if (roomToDrag != null) {
                                log.debug("Starting room drag: ${roomToDrag.id}")
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
                                // Accumulate the drag amount before snapping
                                val modelDragX = dragAmount.x / zoomLevel
                                val modelDragY = dragAmount.y / zoomLevel

                                val room = currentZone.rooms.first { it.id == draggedRoomId }
                                val oldPos = room.position

                                // Calculate new position first without snapping
                                val rawX =
                                    (oldPos.x + modelDragX).coerceIn(0f, canvasWidthDp.value - zone.nodeWidthDp)
                                val rawY = (oldPos.y + modelDragY).coerceIn(
                                    0f,
                                    canvasHeightDp.value - zone.nodeHeightDp
                                )

                                log.debug(
                                    "Drag position - Old: ({}, {}), Raw new: ({}, {}), Current Canvas: {}x{}, Original Canvas: {}x{}, Node: {}x{}, Zoom: {}",
                                    oldPos.x, oldPos.y,
                                    rawX, rawY,
                                    canvasWidthDp.value, canvasHeightDp.value,
                                    canvasWidthDp, canvasHeightDp,
                                    zone.nodeWidthDp, zone.nodeHeightDp,
                                    zoomLevel
                                )

                                // During drag - no snapping at all while actively dragging
                                val newPos = Position(rawX, rawY)

                                lastPosition.value = Position(rawX, rawY)

                                val updatedRooms = currentZone.rooms.map { r ->
                                    if (r.id == draggedRoomId) r.copy(position = newPos) else r
                                }
                                currentZone = currentZone.copy(rooms = updatedRooms)
                                onZoneChanged(currentZone)
                            }
                        },
                        onDragEnd = {
                            log.debug("=== Drag End ===")
                            log.debug("Final connection state: {}", connectionDragState)
                            log.debug("Final dragged room: {}", draggedRoomId)

                            connectionDragState?.let { state ->
                                val sourceRoom = currentZone.rooms.first { it.id == state.sourceRoomId }
                                val targetRoom = currentZone.rooms.firstOrNull { room ->
                                    val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                    roomRect.contains(state.currentPoint) && room.id != state.sourceRoomId
                                }

                                currentZone = if (targetRoom != null) {
                                    currentZone.connectRooms(
                                        sourceRoom,
                                        targetRoom,
                                        state.direction,
                                        state.sourceCorner
                                    )
                                } else {
                                    val modelX = (state.currentPoint.x / (density * zoomLevel))
                                        .coerceIn(0f, canvasWidthDp.value - zone.nodeWidthDp)
                                    
                                    val modelY = (state.currentPoint.y / (density * zoomLevel))
                                        .coerceIn(0f, canvasWidthDp.value - zone.nodeHeightDp)

                                    currentZone.createRoomWithConnection(
                                        sourceRoom,
                                        state.direction,
                                        Position(modelX, modelY),
                                        state.sourceCorner
                                    )
                                }
                                onZoneChanged(currentZone)
                            }

                            draggedRoomId?.let { id ->
                                val room = currentZone.rooms.first { it.id == id }
                                currentZone = currentZone.updateRoomPosition(id, room.position)
                                onZoneChanged(currentZone)
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
                drawConnections(room, currentZone, density, zoomLevel, connectionManager)
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
                    ExitDirection.UP -> if (state.sourceCorner == "LEFT")
                        Offset(sourceRect.left, sourceRect.top)
                    else Offset(sourceRect.right, sourceRect.top)

                    ExitDirection.DOWN -> if (state.sourceCorner == "LEFT")
                        Offset(sourceRect.left, sourceRect.bottom)
                    else Offset(sourceRect.right, sourceRect.bottom)
                }

                drawLine(
                    color = if (state.direction in listOf(
                            ExitDirection.UP,
                            ExitDirection.DOWN
                        )
                    ) Color.Green else Color.Blue,
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

        // Context menu for connections
        contextMenuPosition?.let { position ->
            log.debug("Attempting to show context menu at position: $position")
            contextMenuConnection?.let { (sourceRoom, direction, destRoom) ->
                log.debug("Context menu connection: ${sourceRoom.id} -> ${destRoom.id} ($direction)")
                DropdownMenu(
                    expanded = contextMenuPosition != null,
                    onDismissRequest = {
                        log.debug("Context menu dismissed")
                        contextMenuConnection = null
                        contextMenuPosition = null
                    },
                    offset = position
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Connection") },
                        onClick = {
                            log.debug("Delete connection menu item clicked")
                            val (sourceRoom, direction, targetRoom) = contextMenuConnection ?: run {
                                log.error("Context menu connection is null when delete clicked")
                                return@DropdownMenuItem
                            }
                            log.debug("Attempting to remove connection: {} --[{}]--> {}", 
                                sourceRoom.id, direction, targetRoom.id)
                            
                            val updatedPair = connectionManager.removeConnection(sourceRoom, direction, currentZone)
                            if (updatedPair != null) {
                                log.debug("Connection removed successfully")
                                val (updatedSource, updatedDest) = updatedPair
                                currentZone = currentZone.copy(
                                    rooms = currentZone.rooms.map { r ->
                                        when (r.id) {
                                            updatedSource.id -> updatedSource
                                            updatedDest.id -> updatedDest
                                            else -> r
                                        }
                                    }
                                )
                                onZoneChanged(currentZone)
                            } else {
                                log.warn("Failed to remove connection")
                            }
                            contextMenuConnection = null
                            contextMenuPosition = null
                        }
                    )
                }
            }
        }
    }
}

private fun getRoomRect(room: Room, zone: Zone, density: Float, zoomLevel: Float): Rect {
    // Convert model coordinates (px) to screen coordinates
    val screenX = room.position.x * zoomLevel
    val screenY = room.position.y * zoomLevel
    val width = zone.nodeWidthDp * density * zoomLevel
    val height = zone.nodeHeightDp * density * zoomLevel

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

    // Only attempt to draw if the room has positive dimensions
    if (rect.width <= 0 || rect.height <= 0) return

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

    // Only attempt to draw text if there's enough space
    if (rect.width >= 10 && rect.height >= 10) {  // Minimum size threshold for text
        try {
            // Create base font sizes
            val nameFontSize = (14 * zoomLevel).sp
            val idFontSize = (10 * zoomLevel).sp

            // Measure text dimensions
            val nameStyle = TextStyle(fontSize = nameFontSize, color = Color.Black)
            val idStyle = TextStyle(fontSize = idFontSize, color = Color.Gray)

            val nameMeasure = textMeasurer.measure(room.name, nameStyle)
            val idMeasure = textMeasurer.measure(room.id, idStyle)

            // Calculate vertical spacing between name and id
            val verticalSpacing = 4f * zoomLevel

            // Draw name
            drawText(
                textMeasurer = textMeasurer,
                text = room.name,
                topLeft = rect.topLeft + Offset(8f * zoomLevel, 8f * zoomLevel),
                style = nameStyle
            )

            // Draw ID below name with proper spacing
            drawText(
                textMeasurer = textMeasurer,
                text = room.id,
                topLeft = rect.topLeft + Offset(
                    8f * zoomLevel,
                    8f * zoomLevel + nameMeasure.size.height + verticalSpacing
                ),
                style = idStyle
            )

            // Draw connection points if selected
            if (isSelected) {
                val connectionPointSize = 12f * density * zoomLevel

                // Cardinal direction points (N,S,E,W)
                drawCircle(
                    color = Color.Blue,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.center.x, rect.top),
                    style = Fill
                )

                drawCircle(
                    color = Color.Blue,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.center.x, rect.bottom),
                    style = Fill
                )

                drawCircle(
                    color = Color.Blue,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.right, rect.center.y),
                    style = Fill
                )

                drawCircle(
                    color = Color.Blue,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.left, rect.center.y),
                    style = Fill
                )

                // Check if UP/DOWN exits exist
                val hasUpExit = room.exits.containsKey(ExitDirection.UP)
                val hasDownExit = room.exits.containsKey(ExitDirection.DOWN)

                // Corner points for UP/DOWN (all four corners)
                // Change from Gray to Green even when exit exists
                val upColor = Color.Green    // Remove the conditional color
                val downColor = Color.Green  // Remove the conditional color

                // Top corners (UP)
                drawCircle(
                    color = upColor,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.right, rect.top),
                    style = Fill
                )

                drawCircle(
                    color = upColor,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.left, rect.top),
                    style = Fill
                )

                // Bottom corners (DOWN)
                drawCircle(
                    color = downColor,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.right, rect.bottom),
                    style = Fill
                )

                drawCircle(
                    color = downColor,
                    radius = connectionPointSize / 2,
                    center = Offset(rect.left, rect.bottom),
                    style = Fill
                )
            }
        } catch (e: IllegalArgumentException) {
            log.trace("Skipping text draw for room ${room.id} due to size constraints")
        }
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
    connectionManager: ConnectionManager
) {
    val sourceRect = getRoomRect(room, zone, density, zoomLevel)
    val points = connectionManager.getConnectionPoints(room, sourceRect)

    for ((exitDir, destId) in room.exits) {
        val destRoom = zone.rooms.find { it.id == destId } ?: continue

        // For all directions, only draw the connection once
        if (when (exitDir) {
            ExitDirection.NORTH, ExitDirection.SOUTH -> room.position.y >= destRoom.position.y
            ExitDirection.EAST, ExitDirection.WEST,
            ExitDirection.UP, ExitDirection.DOWN -> room.id <= destRoom.id
        }) {
            val connection = connectionManager.getConnection(room, exitDir, destRoom) ?: continue

            val destRect = getRoomRect(destRoom, zone, density, zoomLevel)
            
            // Get source and destination points
            val (sourcePoint, destPoint) = when (exitDir) {
                ExitDirection.UP, ExitDirection.DOWN -> {
                    val srcCorner = connection.source.corner ?: "RIGHT"
                    val destCorner = connection.destination.corner ?: "LEFT"
                    
                    val srcPoint = when {
                        exitDir == ExitDirection.DOWN && srcCorner == "LEFT" -> 
                            Offset(sourceRect.left, sourceRect.bottom)
                        exitDir == ExitDirection.DOWN -> 
                            Offset(sourceRect.right, sourceRect.bottom)
                        srcCorner == "LEFT" -> 
                            Offset(sourceRect.left, sourceRect.top)
                        else -> 
                            Offset(sourceRect.right, sourceRect.top)
                    }
                    
                    val dstPoint = when {
                        exitDir == ExitDirection.UP && destCorner == "LEFT" -> 
                            Offset(destRect.left, destRect.bottom)
                        exitDir == ExitDirection.UP -> 
                            Offset(destRect.right, destRect.bottom)
                        destCorner == "LEFT" -> 
                            Offset(destRect.left, destRect.top)
                        else -> 
                            Offset(destRect.right, destRect.top)
                    }
                    
                    Pair(srcPoint, dstPoint)
                }
                ExitDirection.NORTH -> Pair(
                    Offset(sourceRect.center.x, sourceRect.top),
                    Offset(destRect.center.x, destRect.bottom)
                )
                ExitDirection.SOUTH -> Pair(
                    Offset(sourceRect.center.x, sourceRect.bottom),
                    Offset(destRect.center.x, destRect.top)
                )
                ExitDirection.EAST -> Pair(
                    Offset(sourceRect.right, sourceRect.center.y),
                    Offset(destRect.left, destRect.center.y)
                )
                ExitDirection.WEST -> Pair(
                    Offset(sourceRect.left, sourceRect.center.y),
                    Offset(destRect.right, destRect.center.y)
                )
            }

            val connectionColor = when (exitDir) {
                ExitDirection.UP, ExitDirection.DOWN -> Color.Green
                else -> Color.Gray
            }

            // Draw the line
            drawLine(
                color = connectionColor,
                start = sourcePoint,
                end = destPoint,
                strokeWidth = 2f * zoomLevel
            )

            // Draw arrows
            val arrowLength = 20f * zoomLevel
            val arrowAngle = (kotlin.math.PI / 6).toFloat()

            val angleToDestination = kotlin.math.atan2(
                (destPoint.y - sourcePoint.y),
                (destPoint.x - sourcePoint.x)
            )
            drawArrow(destPoint, angleToDestination, arrowLength, arrowAngle, zoomLevel, connectionColor)

            val angleToSource = kotlin.math.atan2(
                (sourcePoint.y - destPoint.y),
                (sourcePoint.x - destPoint.x)
            )
            drawArrow(sourcePoint, angleToSource, arrowLength, arrowAngle, zoomLevel, connectionColor)
        }
    }
}

private fun DrawScope.drawArrow(
    point: Offset,
    angle: Float,
    length: Float,
    arrowAngle: Float,
    zoomLevel: Float,
    color: Color
) {
    val arrowPoint1 = Offset(
        point.x - length * kotlin.math.cos(angle - arrowAngle),
        point.y - length * kotlin.math.sin(angle - arrowAngle)
    )
    val arrowPoint2 = Offset(
        point.x - length * kotlin.math.cos(angle + arrowAngle),
        point.y - length * kotlin.math.sin(angle + arrowAngle)
    )

    drawLine(
        color = color,
        start = point,
        end = arrowPoint1,
        strokeWidth = 2f * zoomLevel
    )
    drawLine(
        color = color,
        start = point,
        end = arrowPoint2,
        strokeWidth = 2f * zoomLevel
    )
}

private fun isNearPoint(point: Offset, target: Offset, threshold: Float): Boolean {
    val distance = kotlin.math.sqrt(
        (point.x - target.x) * (point.x - target.x) +
                (point.y - target.y) * (point.y - target.y)
    )
    return distance <= threshold
}

private fun isNearLine(point: Offset, start: Offset, end: Offset, threshold: Float): Boolean {
    // If the line is very short, increase the hit area
    val lineLength = sqrt(
        (end.x - start.x).pow(2) + (end.y - start.y).pow(2)
    )
    
    // For very short lines, use a circular hit area
    if (lineLength < threshold * 2) {
        val centerPoint = Offset(
            (start.x + end.x) / 2,
            (start.y + end.y) / 2
        )
        val distanceToCenter = sqrt(
            (point.x - centerPoint.x).pow(2) + (point.y - centerPoint.y).pow(2)
        )
        return distanceToCenter <= threshold
    }

    // For longer lines, use the point-to-line distance formula
    val numerator = abs(
        (end.y - start.y) * point.x -
        (end.x - start.x) * point.y +
        end.x * start.y -
        end.y * start.x
    )
    
    val denominator = sqrt(
        (end.y - start.y).pow(2) + (end.x - start.x).pow(2)
    )
    
    // Calculate the projection point
    val t = ((point.x - start.x) * (end.x - start.x) + 
            (point.y - start.y) * (end.y - start.y)) / 
            (lineLength * lineLength)
    
    // Check if the projection point lies outside the line segment
    return if (t < 0.0f || t > 1.0f) {
        // If outside, use distance to nearest endpoint
        val distToStart = sqrt(
            (point.x - start.x).pow(2) + (point.y - start.y).pow(2)
        )
        val distToEnd = sqrt(
            (point.x - end.x).pow(2) + (point.y - end.y).pow(2)
        )
        minOf(distToStart, distToEnd) <= threshold
    } else {
        // If inside, use perpendicular distance
        numerator / denominator <= threshold
    }
}

private fun getConnectionPoints(
    exitDir: ExitDirection,
    sourceRect: Rect,
    destRect: Rect,
    sourceRoom: Room,
    destRoom: Room
): Pair<Offset, Offset> {
    return when (exitDir) {
        ExitDirection.NORTH -> Pair(
            Offset(sourceRect.center.x, sourceRect.top),
            Offset(destRect.center.x, destRect.bottom)
        )
        ExitDirection.SOUTH -> Pair(
            Offset(sourceRect.center.x, sourceRect.bottom),
            Offset(destRect.center.x, destRect.top)
        )
        ExitDirection.EAST -> Pair(
            Offset(sourceRect.right, sourceRect.center.y),
            Offset(destRect.left, destRect.center.y)
        )
        ExitDirection.WEST -> Pair(
            Offset(sourceRect.left, sourceRect.center.y),
            Offset(destRect.right, destRect.center.y)
        )
        ExitDirection.UP, ExitDirection.DOWN -> {
            val srcCorner = sourceRoom.exitCorners[exitDir] ?: "RIGHT"
            val oppositeDir = if (exitDir == ExitDirection.UP) ExitDirection.DOWN else ExitDirection.UP
            val destCorner = destRoom.exitCorners[oppositeDir] ?: "LEFT"
            
            val srcPoint = when {
                exitDir == ExitDirection.DOWN && srcCorner == "LEFT" -> 
                    Offset(sourceRect.left, sourceRect.bottom)
                exitDir == ExitDirection.DOWN -> 
                    Offset(sourceRect.right, sourceRect.bottom)
                srcCorner == "LEFT" -> 
                    Offset(sourceRect.left, sourceRect.top)
                else -> 
                    Offset(sourceRect.right, sourceRect.top)
            }
            
            val dstPoint = when {
                oppositeDir == ExitDirection.DOWN && destCorner == "LEFT" -> 
                    Offset(destRect.left, destRect.bottom)
                oppositeDir == ExitDirection.DOWN -> 
                    Offset(destRect.right, destRect.bottom)
                destCorner == "LEFT" -> 
                    Offset(destRect.left, destRect.top)
                else -> 
                    Offset(destRect.right, destRect.top)
            }
            Pair(srcPoint, dstPoint)
        }
    }
}
