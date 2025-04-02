package net.lashua.zonedit.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.ExitData
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.MudZone
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.RoomData
import net.lashua.zonedit.ui.adapters.MudZoneAdapter
import net.lashua.zonedit.viewmodel.ContextMenuInfo
import net.lashua.zonedit.viewmodel.MudZoneViewModel
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("net.lashua.zonedit.ui.graph.GraphCanvas")

/**
 * A canvas for displaying and editing a graph.
 *
 * @param viewModel The MudZoneViewModel to use
 * @param canvasWidthDp The width of the canvas in dp
 * @param canvasHeightDp The height of the canvas in dp
 * @param zoomLevel The zoom level of the canvas
 */
@Composable
fun GraphCanvas(
    viewModel: MudZoneViewModel,
    canvasWidthDp: Dp = 1000.dp,
    canvasHeightDp: Dp = 800.dp,
    zoomLevel: Float = 1f
) {
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    // Convert the MudZone to a VisualGraph
    // Create a new VisualGraph every time the zone or selectedRoomId changes
    // This ensures that the VisualNodes have the correct positions
    val visualGraph = remember(viewModel.zone, viewModel.selectedRoomId) {
        val graph = MudZoneAdapter.toVisualGraph(viewModel.zone, viewModel.selectedRoomId)
        log.debug("GraphCanvas: Created visual graph with {} nodes and {} edges",
            graph.nodes.size, graph.edges.size)

        // Log the positions of all nodes
        graph.nodes.forEach { node ->
            val room = viewModel.zone.getRoom(node.id)
            log.debug("GraphCanvas: Node {} at position ({}, {}) with size ({}, {}), room position: ({}, {})",
                node.id, node.x, node.y, node.width, node.height, room?.position?.x, room?.position?.y)
        }

        graph
    }

    // Connection drag preview state
    var connectionPreview by remember(viewModel.connectionDragState) {
        mutableStateOf(
            viewModel.connectionDragState?.let { state ->
                val sourceRoom = viewModel.zone.getRoom(state.sourceRoomId)
                if (sourceRoom != null) {
                    ConnectionPreview(
                        sourceRoom = sourceRoom,
                        direction = state.direction,
                        corner = state.corner
                    )
                } else null
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .size(canvasWidthDp, canvasHeightDp)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                // Handle pointer movement
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val position = event.changes.firstOrNull()?.position

                            // Update connection drag preview
                            if (viewModel.connectionDragState != null && position != null) {
                                connectionPreview?.currentPoint = position
                            }
                        }
                    }
                }
                // Handle taps for selection
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Find room at tap position
                            val clickX = offset.x / density
                            val clickY = offset.y / density

                            // Find the room at the click position
                            val roomId = viewModel.zone.rooms.find { room ->
                                val roomX = room.position.x
                                val roomY = room.position.y
                                val roomWidth = viewModel.zone.nodeWidthDp
                                val roomHeight = viewModel.zone.nodeHeightDp

                                // Check if the click is inside the room's bounds
                                clickX >= roomX && clickX <= roomX + roomWidth &&
                                clickY >= roomY && clickY <= roomY + roomHeight
                            }?.id

                            log.debug("GraphCanvas: Room at tap position ({}, {}): {}",
                                clickX, clickY, roomId)

                            // Select room
                            viewModel.selectRoom(roomId)
                        }
                    )
                }
                // Handle right-clicks for context menus
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (!event.buttons.isSecondaryPressed) {
                                continue
                            }

                            val offset = event.changes.first().position
                            val dpOffset = DpOffset(
                                x = (offset.x / density).dp,
                                y = (offset.y / density).dp
                            )

                            // Check if clicked on a connection
                            val connection = findConnectionNearPosition(
                                visualGraph, offset.x / density, offset.y / density, 10f
                            )

                            if (connection != null) {
                                // Show connection context menu
                                viewModel.showConnectionContextMenu(
                                    sourceRoomId = connection.sourceId,
                                    direction = connection.data.direction,
                                    targetRoomId = connection.targetId,
                                    position = dpOffset
                                )
                                event.changes.first().consume()
                            } else {
                                // Check if clicked on a room
                                val clickX = offset.x / density
                                val clickY = offset.y / density

                                // Find the room at the click position
                                val roomId = viewModel.zone.rooms.find { room ->
                                    val roomX = room.position.x
                                    val roomY = room.position.y
                                    val roomWidth = viewModel.zone.nodeWidthDp
                                    val roomHeight = viewModel.zone.nodeHeightDp

                                    // Check if the click is inside the room's bounds
                                    clickX >= roomX && clickX <= roomX + roomWidth &&
                                    clickY >= roomY && clickY <= roomY + roomHeight
                                }?.id

                                log.debug("GraphCanvas: Room at context menu position ({}, {}): {}",
                                    clickX, clickY, roomId)

                                if (roomId != null) {
                                    // Show room context menu
                                    viewModel.showRoomContextMenu(
                                        roomId = roomId,
                                        position = dpOffset
                                    )
                                    event.changes.first().consume()
                                }
                            }
                        }
                    }
                }
                // Handle drags for room movement and connection creation
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate the click position
                            val clickX = offset.x / density
                            val clickY = offset.y / density

                            // First, check if we're starting a connection drag
                            // This takes precedence over room dragging
                            var connectionDragStarted = false

                            // Check all rooms for connection points
                            for (room in viewModel.zone.rooms) {
                                // Calculate the connection points for the room
                                val roomX = room.position.x
                                val roomY = room.position.y
                                val roomWidth = viewModel.zone.nodeWidthDp
                                val roomHeight = viewModel.zone.nodeHeightDp

                                // Check if we're near a connection point
                                val connectionPoints = listOf(
                                    Triple(ExitDirection.NORTH, roomX + roomWidth / 2, roomY), // North
                                    Triple(ExitDirection.EAST, roomX + roomWidth, roomY + roomHeight / 2), // East
                                    Triple(ExitDirection.SOUTH, roomX + roomWidth / 2, roomY + roomHeight), // South
                                    Triple(ExitDirection.WEST, roomX, roomY + roomHeight / 2), // West
                                    Triple(ExitDirection.UP, roomX + roomWidth / 4, roomY + roomHeight / 4), // Up (top-left corner)
                                    Triple(ExitDirection.DOWN, roomX + 3 * roomWidth / 4, roomY + 3 * roomHeight / 4) // Down (bottom-right corner)
                                )

                                // Find the closest connection point
                                val connectionPoint = connectionPoints.find { (_, x, y) ->
                                    val distance = Math.sqrt(Math.pow((clickX - x).toDouble(), 2.0) + Math.pow((clickY - y).toDouble(), 2.0))
                                    log.debug("GraphCanvas: Distance to connection point: {} (threshold: 10)", distance)
                                    distance < 10
                                }

                                if (connectionPoint != null) {
                                    // Start connection drag
                                    log.debug("GraphCanvas: Starting connection drag from {} in direction {}",
                                        room.id, connectionPoint.first)

                                    // Select the room first
                                    viewModel.selectRoom(room.id)

                                    // Then start the connection drag
                                    viewModel.startConnectionDrag(
                                        roomId = room.id,
                                        direction = connectionPoint.first,
                                        corner = null
                                    )

                                    // Create a connection preview
                                    connectionPreview = ConnectionPreview(
                                        sourceRoom = room,
                                        direction = connectionPoint.first,
                                        corner = null,
                                        currentPoint = offset
                                    )

                                    connectionDragStarted = true
                                    break
                                }
                            }

                            // If we're not starting a connection drag, check if we're starting a room drag
                            if (!connectionDragStarted) {
                                // Find the room at the click position
                                val roomId = viewModel.zone.rooms.find { room ->
                                    val roomX = room.position.x
                                    val roomY = room.position.y
                                    val roomWidth = viewModel.zone.nodeWidthDp
                                    val roomHeight = viewModel.zone.nodeHeightDp

                                    // Check if the click is inside the room's bounds
                                    clickX >= roomX && clickX <= roomX + roomWidth &&
                                    clickY >= roomY && clickY <= roomY + roomHeight
                                }?.id

                                log.debug("GraphCanvas: Room at position ({}, {}): {}",
                                    clickX, clickY, roomId)

                                if (roomId != null) {
                                    log.debug("GraphCanvas: Starting drag for room {}", roomId)
                                    viewModel.startDraggingRoom(roomId)
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            // Handle room dragging
                            if (viewModel.draggedRoomId != null) {
                                val room = viewModel.zone.getRoom(viewModel.draggedRoomId!!)
                                if (room != null) {
                                    // Calculate new position
                                    val modelDragX = dragAmount.x / (density * zoomLevel)
                                    val modelDragY = dragAmount.y / (density * zoomLevel)

                                    val newX = (room.position.x + modelDragX)
                                        .coerceIn(0f, canvasWidthDp.value - viewModel.zone.nodeWidthDp)
                                    val newY = (room.position.y + modelDragY)
                                        .coerceIn(0f, canvasHeightDp.value - viewModel.zone.nodeHeightDp)

                                    log.debug("GraphCanvas: Dragging room {} to position ({}, {})",
                                        room.id, newX, newY)

                                    // Update room position
                                    viewModel.updateRoomPosition(
                                        roomId = room.id,
                                        position = Position(newX, newY)
                                    )
                                }
                            }

                            // Update connection drag preview
                            if (viewModel.connectionDragState != null && connectionPreview != null) {
                                // Use a local variable to avoid smart cast issues
                                val preview = connectionPreview
                                if (preview != null) {
                                    log.debug("GraphCanvas: Updating connection drag preview, current point: {}, drag amount: {}",
                                        preview.currentPoint, dragAmount)
                                    preview.currentPoint = preview.currentPoint?.plus(dragAmount) ?: Offset.Zero
                                    log.debug("GraphCanvas: Updated connection drag preview, new point: {}",
                                        preview.currentPoint)
                                }
                            }
                        },
                        onDragEnd = {
                            // Finalize room drag
                            if (viewModel.draggedRoomId != null) {
                                // Get the current room position
                                val room = viewModel.zone.getRoom(viewModel.draggedRoomId!!)
                                if (room != null) {
                                    // Snap the position to the grid
                                    viewModel.updateRoomPosition(
                                        roomId = room.id,
                                        position = room.position,
                                        snap = true
                                    )
                                }
                                viewModel.stopDraggingRoom()
                            }

                            // Finalize connection drag
                            if (viewModel.connectionDragState != null && connectionPreview != null) {
                                // Use a local variable to avoid smart cast issues
                                val preview = connectionPreview
                                if (preview != null) {
                                    log.debug("GraphCanvas: Finalizing connection drag, state: {}, preview: {}",
                                        viewModel.connectionDragState, preview.currentPoint)

                                    val targetPosition = preview.currentPoint
                                    if (targetPosition != null) {
                                    // Check if we're over a room
                                    val clickX = targetPosition.x / density
                                    val clickY = targetPosition.y / density

                                    log.debug("GraphCanvas: Connection drag end position: ({}, {})", clickX, clickY)

                                    // Find the room at the click position
                                    val targetRoomId = viewModel.zone.rooms.find { room ->
                                        val roomX = room.position.x
                                        val roomY = room.position.y
                                        val roomWidth = viewModel.zone.nodeWidthDp
                                        val roomHeight = viewModel.zone.nodeHeightDp

                                        // Check if the click is inside the room's bounds
                                        val contains = clickX >= roomX && clickX <= roomX + roomWidth &&
                                            clickY >= roomY && clickY <= roomY + roomHeight

                                        log.debug("GraphCanvas: Checking if room {} at ({}, {}) contains point ({}, {}): {}",
                                            room.id, roomX, roomY, clickX, clickY, contains)

                                        contains
                                    }?.id

                                    log.debug("GraphCanvas: Target room at position ({}, {}): {}",
                                        clickX, clickY, targetRoomId)

                                    if (targetRoomId != null) {
                                        // Connect to existing room
                                        log.debug("GraphCanvas: Connecting to existing room: {}", targetRoomId)
                                        viewModel.finalizeConnectionDrag(targetRoomId)
                                    } else {
                                        // Create new room with connection
                                        val newPosition = Position(
                                            x = (targetPosition.x / (density * zoomLevel))
                                                .coerceIn(0f, canvasWidthDp.value - viewModel.zone.nodeWidthDp),
                                            y = (targetPosition.y / (density * zoomLevel))
                                                .coerceIn(0f, canvasHeightDp.value - viewModel.zone.nodeHeightDp)
                                        )

                                        log.debug("GraphCanvas: Creating new room at position: {}", newPosition)
                                        viewModel.finalizeConnectionDrag(
                                            targetRoomId = null,
                                            position = newPosition
                                        )
                                    }
                                    } else {
                                        // Cancel connection drag
                                        log.debug("GraphCanvas: Canceling connection drag (no target position)")
                                        viewModel.cancelConnectionDrag()
                                    }
                                } else {
                                    // Cancel connection drag (no preview)
                                    log.debug("GraphCanvas: Canceling connection drag (no preview)")
                                    viewModel.cancelConnectionDrag()
                                }
                            }
                        }
                    )
                }
        ) {
            // Draw the graph
            GraphRenderer.draw(
                drawScope = this,
                graph = visualGraph,
                textMeasurer = textMeasurer,
                zoomLevel = zoomLevel,
                showConnectionPoints = viewModel.selectedRoomId != null
            )

            // Draw connection preview
            connectionPreview?.let { preview ->
                val sourceNode = visualGraph.getNode(preview.sourceRoom.id)
                if (sourceNode != null && preview.currentPoint != null) {
                    // Draw preview line
                    val sourcePoint = getConnectionPoint(
                        sourceNode, preview.direction, preview.corner
                    )

                    drawLine(
                        color = androidx.compose.ui.graphics.Color.Blue,
                        start = sourcePoint,
                        end = preview.currentPoint!!,
                        strokeWidth = 2f * zoomLevel,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }
        }

        // Context menu for connections
        when (val contextMenu = viewModel.contextMenuInfo) {
            is ContextMenuInfo.ConnectionMenu -> {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { viewModel.hideContextMenu() },
                    offset = contextMenu.position
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Connection") },
                        onClick = {
                            viewModel.removeConnection(
                                sourceRoomId = contextMenu.sourceRoomId,
                                direction = contextMenu.direction,
                                targetRoomId = contextMenu.targetRoomId
                            )
                        }
                    )
                }
            }
            is ContextMenuInfo.RoomMenu -> {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { viewModel.hideContextMenu() },
                    offset = contextMenu.position
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Room") },
                        onClick = {
                            viewModel.removeRoom(contextMenu.roomId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Description") },
                        onClick = {
                            // TODO: Show description editor
                            viewModel.hideContextMenu()
                        }
                    )
                }
            }
            null -> { /* No context menu */ }
        }
    }
}

/**
 * Finds a room at a specific position.
 *
 * @param graph The visual graph to search in
 * @param x The x-coordinate in dp
 * @param y The y-coordinate in dp
 * @return The room at the position, or null if none found
 */
private fun findRoomAtPosition(
    graph: VisualGraph<RoomData, ExitData>,
    x: Float,
    y: Float
): VisualNode<RoomData>? {
    val node = graph.findNodeAt(x, y)
    log.debug("findRoomAtPosition: Searching for room at ({}, {}), found: {}", x, y, node?.id)
    return node
}

/**
 * Finds a connection near a specific position.
 *
 * @param graph The visual graph to search in
 * @param x The x-coordinate in dp
 * @param y The y-coordinate in dp
 * @param threshold The distance threshold in dp
 * @return The connection near the position, or null if none found
 */
private fun findConnectionNearPosition(
    graph: VisualGraph<RoomData, ExitData>,
    x: Float,
    y: Float,
    threshold: Float
): VisualEdge<ExitData>? {
    return graph.findEdgeNear(x, y, threshold)
}

/**
 * Information about a connection point.
 *
 * @property direction The direction of the connection point
 * @property corner The corner for UP/DOWN connection points
 */
private data class ConnectionPoint(
    val direction: ExitDirection,
    val corner: String? = null
)

/**
 * Finds a connection point near a specific position.
 *
 * @param node The node to check
 * @param x The x-coordinate in dp
 * @param y The y-coordinate in dp
 * @param threshold The distance threshold in dp
 * @return The connection point near the position, or null if none found
 */
private fun findConnectionPointNearPosition(
    node: VisualNode<RoomData>,
    x: Float,
    y: Float,
    threshold: Float
): ConnectionPoint? {
    val position = Offset(x, y)
    val bounds = node.bounds

    // Check cardinal directions
    val northPoint = Offset(bounds.center.x, bounds.top)
    if ((position - northPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.NORTH)
    }

    val southPoint = Offset(bounds.center.x, bounds.bottom)
    if ((position - southPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.SOUTH)
    }

    val eastPoint = Offset(bounds.right, bounds.center.y)
    if ((position - eastPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.EAST)
    }

    val westPoint = Offset(bounds.left, bounds.center.y)
    if ((position - westPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.WEST)
    }

    // Check UP corners
    val upRightPoint = Offset(bounds.right, bounds.top)
    if ((position - upRightPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.UP, "RIGHT")
    }

    val upLeftPoint = Offset(bounds.left, bounds.top)
    if ((position - upLeftPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.UP, "LEFT")
    }

    // Check DOWN corners
    val downRightPoint = Offset(bounds.right, bounds.bottom)
    if ((position - downRightPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.DOWN, "RIGHT")
    }

    val downLeftPoint = Offset(bounds.left, bounds.bottom)
    if ((position - downLeftPoint).getDistance() <= threshold) {
        return ConnectionPoint(ExitDirection.DOWN, "LEFT")
    }

    return null
}

/**
 * Gets the connection point for a specific direction on a node.
 *
 * @param node The node to get the connection point for
 * @param direction The direction of the connection point
 * @param corner The corner for UP/DOWN connection points
 * @return The connection point
 */
private fun getConnectionPoint(
    node: VisualNode<*>,
    direction: ExitDirection,
    corner: String? = null
): Offset {
    val bounds = node.bounds

    return when (direction) {
        ExitDirection.NORTH -> Offset(bounds.center.x, bounds.top)
        ExitDirection.SOUTH -> Offset(bounds.center.x, bounds.bottom)
        ExitDirection.EAST -> Offset(bounds.right, bounds.center.y)
        ExitDirection.WEST -> Offset(bounds.left, bounds.center.y)
        ExitDirection.UP -> {
            if (corner == "LEFT") Offset(bounds.left, bounds.top) else Offset(bounds.right, bounds.top)
        }
        ExitDirection.DOWN -> {
            if (corner == "LEFT") Offset(bounds.left, bounds.bottom) else Offset(bounds.right, bounds.bottom)
        }
    }
}

/**
 * State for a connection preview.
 *
 * @property sourceRoom The source room
 * @property direction The direction of the connection
 * @property corner The corner for UP/DOWN connections
 * @property currentPoint The current point of the drag
 */
private data class ConnectionPreview(
    val sourceRoom: net.lashua.zonedit.model.MudRoom,
    val direction: ExitDirection,
    val corner: String? = null,
    var currentPoint: Offset? = null
) {
    override fun toString(): String {
        return "ConnectionPreview(sourceRoom=${sourceRoom.id}, direction=$direction, corner=$corner, currentPoint=$currentPoint)"
    }
}
