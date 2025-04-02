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
import androidx.compose.runtime.remember
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
    val visualGraph = remember(viewModel.zone, viewModel.selectedRoomId) {
        MudZoneAdapter.toVisualGraph(viewModel.zone, viewModel.selectedRoomId)
    }

    // Connection drag preview state
    val connectionPreview = remember(viewModel.connectionDragState) {
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
                            val roomAtPosition = findRoomAtPosition(
                                visualGraph, offset.x / density, offset.y / density
                            )

                            // Select room
                            viewModel.selectRoom(roomAtPosition?.id)
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
                                val room = findRoomAtPosition(
                                    visualGraph, offset.x / density, offset.y / density
                                )

                                if (room != null) {
                                    // Show room context menu
                                    viewModel.showRoomContextMenu(
                                        roomId = room.id,
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
                            // Check if we're starting a connection drag
                            if (viewModel.selectedRoomId != null) {
                                val selectedRoom = visualGraph.getNode(viewModel.selectedRoomId!!)
                                if (selectedRoom != null) {
                                    // Check if we're near a connection point
                                    val connectionPoint = findConnectionPointNearPosition(
                                        selectedRoom, offset.x / density, offset.y / density, 10f
                                    )

                                    if (connectionPoint != null) {
                                        // Start connection drag
                                        viewModel.startConnectionDrag(
                                            roomId = selectedRoom.id,
                                            direction = connectionPoint.direction,
                                            corner = connectionPoint.corner
                                        )
                                        connectionPreview?.currentPoint = offset
                                        return@detectDragGestures
                                    }
                                }
                            }

                            // Check if we're starting a room drag
                            val roomAtPosition = findRoomAtPosition(
                                visualGraph, offset.x / density, offset.y / density
                            )

                            if (roomAtPosition != null) {
                                viewModel.startDraggingRoom(roomAtPosition.id)
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

                                    // Update room position
                                    viewModel.updateRoomPosition(
                                        roomId = room.id,
                                        position = Position(newX, newY)
                                    )
                                }
                            }

                            // Update connection drag preview
                            if (viewModel.connectionDragState != null) {
                                connectionPreview?.currentPoint = connectionPreview?.currentPoint?.plus(dragAmount) ?: Offset.Zero
                            }
                        },
                        onDragEnd = {
                            // Finalize room drag
                            if (viewModel.draggedRoomId != null) {
                                viewModel.stopDraggingRoom()
                            }

                            // Finalize connection drag
                            if (viewModel.connectionDragState != null && connectionPreview != null) {
                                val targetPosition = connectionPreview.currentPoint
                                if (targetPosition != null) {
                                    // Check if we're over a room
                                    val targetRoom = findRoomAtPosition(
                                        visualGraph,
                                        targetPosition.x / density,
                                        targetPosition.y / density
                                    )

                                    if (targetRoom != null) {
                                        // Connect to existing room
                                        viewModel.finalizeConnectionDrag(targetRoom.id)
                                    } else {
                                        // Create new room with connection
                                        viewModel.finalizeConnectionDrag(
                                            targetRoomId = null,
                                            position = Position(
                                                x = (targetPosition.x / (density * zoomLevel))
                                                    .coerceIn(0f, canvasWidthDp.value - viewModel.zone.nodeWidthDp),
                                                y = (targetPosition.y / (density * zoomLevel))
                                                    .coerceIn(0f, canvasHeightDp.value - viewModel.zone.nodeHeightDp)
                                            )
                                        )
                                    }
                                } else {
                                    // Cancel connection drag
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
    return graph.findNodeAt(x, y)
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
)
