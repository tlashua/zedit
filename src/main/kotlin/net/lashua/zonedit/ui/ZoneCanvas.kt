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

// Add spatial indexing for room hit detection
private class QuadTree<T>(private val bounds: Rect) {
    private val maxObjects = 10
    private val objects = mutableListOf<Pair<T, Rect>>()
    private var subdivided = false
    private var northwest: QuadTree<T>? = null
    private var northeast: QuadTree<T>? = null
    private var southwest: QuadTree<T>? = null
    private var southeast: QuadTree<T>? = null

    fun clear() {
        objects.clear()
        northwest = null
        northeast = null
        southwest = null
        southeast = null
        subdivided = false
    }

    fun insert(item: T, itemBounds: Rect): Boolean {
        if (!bounds.overlaps(itemBounds)) return false

        if (objects.size < maxObjects) {
            objects.add(item to itemBounds)
            return true
        }

        if (!subdivided) {
            subdivide()
        }

        return northwest!!.insert(item, itemBounds) ||
               northeast!!.insert(item, itemBounds) ||
               southwest!!.insert(item, itemBounds) ||
               southeast!!.insert(item, itemBounds)
    }

    fun query(point: Offset): List<T> {
        if (!bounds.contains(point)) return emptyList()

        val found = objects.filter { (_, rect) -> 
            rect.contains(point) 
        }.map { it.first }

        if (subdivided) {
            found += northwest!!.query(point)
            found += northeast!!.query(point)
            found += southwest!!.query(point)
            found += southeast!!.query(point)
        }

        return found
    }

    private fun subdivide() {
        val x = bounds.left
        val y = bounds.top
        val w = bounds.width / 2
        val h = bounds.height / 2

        northwest = QuadTree(Rect(x, y, w, h))
        northeast = QuadTree(Rect(x + w, y, w, h))
        southwest = QuadTree(Rect(x, y + h, w, h))
        southeast = QuadTree(Rect(x + w, y + h, w, h))
        
        subdivided = true
    }
}

// Add viewport culling
private fun DrawScope.drawVisibleRooms(
    rooms: List<Room>,
    viewport: Rect,
    zone: Zone,
    density: Float,
    zoomLevel: Float,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    rooms.filter { room ->
        getRoomRect(room, zone, density, zoomLevel)
            .overlaps(viewport)
    }.forEach { room ->
        drawRoom(room, zone, density, zoomLevel, room.id == selectedRoom?.id, textMeasurer)
    }
}
