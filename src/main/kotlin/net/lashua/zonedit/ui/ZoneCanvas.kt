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
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

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
    var lastSnapTime = 0L
    var pushStartTime = remember { mutableStateOf(0L) }
    var lastPosition = remember { mutableStateOf<Position?>(null) }
    val snapDelay = 50L  // Keep the same delay
    val snapThreshold = 0.4f  // Slightly more generous position threshold
    val velocityThreshold = 2.5f  // Slightly more forgiving velocity threshold
    val breakFreeThreshold = 1.5f  // New: easier to break free than to initially snap
    val breakFreeTime = 400L  // Break free after 400ms of continuous pushing
    val smoothingFactor = 0.8f    // New: helps reduce jitter (0-1, higher = smoother)

    LaunchedEffect(selectedRoom) {
        currentSelectedRoom = selectedRoom
        log.debug("ZoneCanvas - Selected room updated: ${selectedRoom?.id}")
    }

    LaunchedEffect(zone) {
        log.debug("ZoneCanvas - Zone updated: ${zone.rooms.map { it.id }}")
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
                            log.debug("=== Drag Start ===")
                            log.debug("Initial offset: $offset")
                            log.debug("Selected room: ${currentSelectedRoom?.id}")

                            // First check if we're near any connection point of the selected room
                            if (currentSelectedRoom != null) {
                                val roomRect = getRoomRect(currentSelectedRoom!!, currentZone, density, zoomLevel)
                                val connectionPointSize = 12f * density * zoomLevel

                                // Check if UP or DOWN exits already exist
                                val hasUpExit = currentSelectedRoom!!.exits.containsKey(ExitDirection.UP)
                                val hasDownExit = currentSelectedRoom!!.exits.containsKey(ExitDirection.DOWN)

                                val direction = when {
                                    isNearPoint(offset, Offset(roomRect.center.x, roomRect.top), connectionPointSize) ->
                                        ExitDirection.NORTH to null

                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.center.x, roomRect.bottom),
                                        connectionPointSize
                                    ) ->
                                        ExitDirection.SOUTH to null

                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.right, roomRect.center.y),
                                        connectionPointSize
                                    ) ->
                                        ExitDirection.EAST to null

                                    isNearPoint(
                                        offset,
                                        Offset(roomRect.left, roomRect.center.y),
                                        connectionPointSize
                                    ) ->
                                        ExitDirection.WEST to null
                                    // Only allow UP if no UP exit exists
                                    !hasUpExit && (
                                            isNearPoint(
                                                offset,
                                                Offset(roomRect.right, roomRect.top),
                                                connectionPointSize
                                            ) ||
                                                    isNearPoint(
                                                        offset,
                                                        Offset(roomRect.left, roomRect.top),
                                                        connectionPointSize
                                                    )
                                            ) -> ExitDirection.UP to (
                                            if (isNearPoint(
                                                    offset,
                                                    Offset(roomRect.left, roomRect.top),
                                                    connectionPointSize
                                                )
                                            )
                                                "LEFT" else "RIGHT"
                                            )
                                    // Only allow DOWN if no DOWN exit exists
                                    !hasDownExit && (
                                            isNearPoint(
                                                offset,
                                                Offset(roomRect.right, roomRect.bottom),
                                                connectionPointSize
                                            ) ||
                                                    isNearPoint(
                                                        offset,
                                                        Offset(roomRect.left, roomRect.bottom),
                                                        connectionPointSize
                                                    )
                                            ) -> ExitDirection.DOWN to (
                                            if (isNearPoint(
                                                    offset,
                                                    Offset(roomRect.left, roomRect.bottom),
                                                    connectionPointSize
                                                )
                                            )
                                                "LEFT" else "RIGHT"
                                            )

                                    else -> null to null
                                }

                                if (direction.first != null) {
                                    log.debug("Starting connection drag from ${currentSelectedRoom!!.id} in direction ${direction.first}")
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
                                val modelDragX = dragAmount.x / (density * zoomLevel)
                                val modelDragY = dragAmount.y / (density * zoomLevel)

                                val room = currentZone.rooms.first { it.id == draggedRoomId }
                                val oldPos = room.position

                                // Calculate new position first without snapping
                                val rawX = (oldPos.x + modelDragX).coerceIn(0f, canvasWidth.toFloat() - currentZone.nodeWidth)
                                val rawY = (oldPos.y + modelDragY).coerceIn(0f, canvasHeight.toFloat() - currentZone.nodeHeight)

                                // Only snap when we're close to a grid line
                                val newPos = if (currentZone.snapToGrid) {
                                    val snappedX = (rawX / currentZone.gridSize).roundToInt() * currentZone.gridSize
                                    val snappedY = (rawY / currentZone.gridSize).roundToInt() * currentZone.gridSize
                                    
                                    // Calculate how far we are from grid lines
                                    val distanceToGridX = abs(rawX - snappedX)
                                    val distanceToGridY = abs(rawY - snappedY)
                                    
                                    // Snap only when very close to grid lines (15% of grid size)
                                    // This makes it much easier to break free
                                    val snapThreshold = currentZone.gridSize * 0.15f
                                    
                                    Position(
                                        x = if (distanceToGridX < snapThreshold) snappedX else rawX,
                                        y = if (distanceToGridY < snapThreshold) snappedY else rawY
                                    )
                                } else {
                                    Position(rawX, rawY)
                                }

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
                            log.debug("Final connection state: $connectionDragState")
                            log.debug("Final dragged room: $draggedRoomId")

                            connectionDragState?.let { state ->
                                val targetRoom = currentZone.rooms.firstOrNull { room ->
                                    val roomRect = getRoomRect(room, currentZone, density, zoomLevel)
                                    roomRect.contains(state.currentPoint) && room.id != state.sourceRoomId
                                }

                                if (targetRoom != null) {
                                    // Always create bi-directional connections
                                    val oppositeDirection = when (state.direction) {
                                        ExitDirection.NORTH -> ExitDirection.SOUTH
                                        ExitDirection.SOUTH -> ExitDirection.NORTH
                                        ExitDirection.EAST -> ExitDirection.WEST
                                        ExitDirection.WEST -> ExitDirection.EAST
                                        ExitDirection.UP -> ExitDirection.DOWN
                                        ExitDirection.DOWN -> ExitDirection.UP
                                    }

                                    val updatedRooms = currentZone.rooms.map { room ->
                                        when (room.id) {
                                            state.sourceRoomId -> room.copy(
                                                exits = room.exits + (state.direction to targetRoom.id)
                                            )

                                            targetRoom.id -> room.copy(
                                                exits = room.exits + (oppositeDirection to state.sourceRoomId)
                                            )

                                            else -> room
                                        }
                                    }
                                    currentZone = currentZone.copy(rooms = updatedRooms)
                                    onZoneChanged(currentZone)
                                } else {
                                    // Create new room at drop location
                                    val sourceRoom = currentZone.rooms.find { it.id == state.sourceRoomId }!!

                                    // Convert screen coordinates back to model coordinates
                                    val modelX = (state.currentPoint.x / (density * zoomLevel)).coerceIn(
                                        0f,
                                        canvasWidth.toFloat() - currentZone.nodeWidth
                                    )
                                    val modelY = (state.currentPoint.y / (density * zoomLevel)).coerceIn(
                                        0f,
                                        canvasHeight.toFloat() - currentZone.nodeHeight
                                    )
                                    val snappedPos = currentZone.snapPosition(Position(modelX, modelY))

                                    // Create the new room with zone-based ID and snapped position
                                    val nextNum = currentZone.getNextRoomNumber()
                                    val newRoom = Room(
                                        id = "${currentZone.name.lowercase()}$nextNum",
                                        name = "New Room",
                                        description = "Description",
                                        position = snappedPos
                                    )

                                    // Set up bi-directional connection
                                    val oppositeDirection = when (state.direction) {
                                        ExitDirection.NORTH -> ExitDirection.SOUTH
                                        ExitDirection.SOUTH -> ExitDirection.NORTH
                                        ExitDirection.EAST -> ExitDirection.WEST
                                        ExitDirection.WEST -> ExitDirection.EAST
                                        ExitDirection.UP -> ExitDirection.DOWN
                                        ExitDirection.DOWN -> ExitDirection.UP
                                    }

                                    val updatedRooms = currentZone.rooms.map { room ->
                                        if (room.id == state.sourceRoomId) {
                                            room.copy(exits = room.exits + (state.direction to newRoom.id))
                                        } else room
                                    } + newRoom.copy(exits = mapOf(oppositeDirection to state.sourceRoomId))

                                    currentZone = currentZone.copy(rooms = updatedRooms)
                                }
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
    val nameStyle = TextStyle(
        fontSize = (14 * zoomLevel).sp,
        color = Color.Black
    )

    val idStyle = TextStyle(
        fontSize = (10 * zoomLevel).sp,
        color = Color.Gray
    )

    // Draw name
    drawText(
        textMeasurer = textMeasurer,
        text = room.name,
        topLeft = rect.topLeft + Offset(8f * zoomLevel, 8f * zoomLevel),
        style = nameStyle
    )

    // Draw ID below name
    drawText(
        textMeasurer = textMeasurer,
        text = room.id,
        topLeft = rect.topLeft + Offset(8f * zoomLevel, 24f * zoomLevel),
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
        val upColor = if (hasUpExit) Color.Gray else Color.Green
        val downColor = if (hasDownExit) Color.Gray else Color.Green

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

    for ((exitDir, destId) in room.exits) {
        val destRoom = zone.rooms.find { it.id == destId } ?: continue

        // For all directions, only draw the connection once
        // For UP/DOWN and EAST/WEST, draw from the room with lower ID
        // For NORTH/SOUTH, draw from the room with higher Y coordinate
        if (when (exitDir) {
                ExitDirection.NORTH, ExitDirection.SOUTH -> room.position.y >= destRoom.position.y
                ExitDirection.EAST, ExitDirection.WEST,
                ExitDirection.UP, ExitDirection.DOWN -> room.id <= destRoom.id
            }
        ) {
            val destRect = getRoomRect(destRoom, zone, density, zoomLevel)

            // Get the correct source and destination points based on direction
            val sourcePoint = when (exitDir) {
                ExitDirection.NORTH -> Offset(sourceRect.center.x, sourceRect.top)
                ExitDirection.SOUTH -> Offset(sourceRect.center.x, sourceRect.bottom)
                ExitDirection.EAST -> Offset(sourceRect.right, sourceRect.center.y)
                ExitDirection.WEST -> Offset(sourceRect.left, sourceRect.center.y)
                ExitDirection.UP -> Offset(sourceRect.right, sourceRect.top)
                ExitDirection.DOWN -> Offset(sourceRect.right, sourceRect.bottom)
            }

            val destPoint = when (exitDir) {
                ExitDirection.NORTH -> Offset(destRect.center.x, destRect.bottom)
                ExitDirection.SOUTH -> Offset(destRect.center.x, destRect.top)
                ExitDirection.EAST -> Offset(destRect.left, destRect.center.y)
                ExitDirection.WEST -> Offset(destRect.right, destRect.center.y)
                ExitDirection.UP -> Offset(destRect.left, destRect.bottom)
                ExitDirection.DOWN -> Offset(destRect.left, destRect.top)
            }

            val connectionColor = when (exitDir) {
                ExitDirection.UP, ExitDirection.DOWN -> Color.Green
                else -> Color.Gray
            }

            // Draw the main line
            drawLine(
                color = connectionColor,
                start = sourcePoint,
                end = destPoint,
                strokeWidth = 2f * zoomLevel
            )

            // Draw arrows at both ends for all directions
            val arrowLength = 20f * zoomLevel
            val arrowAngle = (kotlin.math.PI / 6).toFloat()

            // Arrow at destination end
            val angleToDestination = kotlin.math.atan2(
                (destPoint.y - sourcePoint.y),
                (destPoint.x - sourcePoint.x)
            )
            drawArrow(destPoint, angleToDestination, arrowLength, arrowAngle, zoomLevel, connectionColor)

            // Arrow at source end
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

private fun handleNormalSnapping(
    rawX: Float, rawY: Float, velocity: Float, 
    currentTime: Long, lastSnapTime: Long,
    snapDelay: Long, velocityThreshold: Float,
    snappedX: Float, snappedY: Float,
    shouldSnapX: Boolean, shouldSnapY: Boolean,
    smoothingFactor: Float
): Position {
    val x = if (velocity < velocityThreshold && 
               currentTime - lastSnapTime > snapDelay && 
               shouldSnapX) {
        rawX * (1 - smoothingFactor) + snappedX * smoothingFactor
    } else {
        rawX
    }
    
    val y = if (velocity < velocityThreshold && 
               currentTime - lastSnapTime > snapDelay && 
               shouldSnapY) {
        rawY * (1 - smoothingFactor) + snappedY * smoothingFactor
    } else {
        rawY
    }
    
    return Position(x, y)
}
