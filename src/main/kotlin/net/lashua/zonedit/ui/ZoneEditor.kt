package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.io.ZoneSerializer
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.RoomUtils
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.components.ComboBox
import net.lashua.zonedit.ui.components.GridDimensionField
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val log = LoggerFactory.getLogger("net.lashua.zonedit.ui.ZoneEditor")

@Composable
fun ZoneEditor(
    zone: Zone,
    modifier: Modifier = Modifier
) {
    var currentZone by remember { mutableStateOf(zone) }
    var nodeWidthText by remember { mutableStateOf(currentZone.nodeWidth.toInt().toString()) }
    var nodeHeightText by remember { mutableStateOf(currentZone.nodeHeight.toInt().toString()) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    var canvasWidth by remember { mutableStateOf(1000) }
    var canvasHeight by remember { mutableStateOf(1000) }

    // File chooser state
    var fileChooser by remember { mutableStateOf<JFileChooser?>(null) }

    // Initialize file chooser once
    LaunchedEffect(Unit) {
        fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Zone Files (*.zone)", "zone")
            isAcceptAllFileFilterUsed = false
        }
    }

    // Separate handlers for clarity
    fun handleSaveOperation() {
        fileChooser?.let { chooser ->
            val window = ComposeWindow()
            log.debug("Starting SAVE operation")

            val result = chooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                log.debug("File selected for save: ${file.absolutePath}")
                try {
                    // Ensure .zone extension
                    val saveFile = if (!file.name.endsWith(".zone")) {
                        File(file.parentFile, "${file.name}.zone").also {
                            log.debug("Adding .zone extension. New path: ${it.absolutePath}")
                        }
                    } else file

                    log.debug("About to save zone with ${currentZone.rooms.size} rooms to: ${saveFile.absolutePath}")
                    ZoneSerializer.saveZone(currentZone, saveFile)
                    log.info("Successfully saved zone to ${saveFile.absolutePath}")
                } catch (e: Exception) {
                    log.error("Failed path: {}", file.absolutePath, e)
                    e.printStackTrace()
                }
            } else {
                log.debug("Save operation cancelled by user")
            }
            window.dispose()
        }
    }

    fun handleOpenOperation() {
        fileChooser?.let { chooser ->
            val window = ComposeWindow()
            log.debug("Starting OPEN operation")

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                log.debug("File selected for open: ${file.absolutePath}")
                try {
                    if (!file.exists()) {
                        log.error("File does not exist: ${file.absolutePath}")
                        return@let
                    }
                    currentZone = ZoneSerializer.loadZone(file)
                    nodeWidthText = currentZone.nodeWidth.toInt().toString()
                    nodeHeightText = currentZone.nodeHeight.toInt().toString()
                    selectedRoom = null
                    log.info("Successfully loaded zone from ${file.absolutePath}")
                } catch (e: Exception) {
                    log.error("Open operation failed", e)
                    log.error("Failed path: ${file.absolutePath}")
                    e.printStackTrace()
                }
            } else {
                log.debug("Open operation cancelled by user")
            }
            window.dispose()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            // Zone name input at the top
            Surface(
                modifier = Modifier,  // Removed fillMaxWidth()
                shadowElevation = 4.dp
            ) {
                OutlinedTextField(
                    value = currentZone.name,
                    onValueChange = { newName ->
                        currentZone = currentZone.copy(name = newName.take(20))
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .width(240.dp), // Width for exactly 20 characters
                    label = { Text("Zone Name") },
                    singleLine = true,
                    maxLines = 1
                )
            }

            // Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File operations
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { handleOpenOperation() }
                        ) {
                            Text("Open")
                        }

                        Button(
                            onClick = { handleSaveOperation() }
                        ) {
                            Text("Save")
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .padding(horizontal = 8.dp)
                    )

                    // Renumber button
                    Button(
                        onClick = {
                            if (currentZone.rooms.isNotEmpty()) {
                                currentZone = RoomUtils.renumberRooms(currentZone)
                            }
                        },
                        enabled = currentZone.rooms.isNotEmpty()
                    ) {
                        Text("Renumber Rooms")
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .padding(horizontal = 8.dp)
                    )

                    // Existing buttons start here
                    Button(
                        onClick = {
                            val nextNum = currentZone.getNextRoomNumber()
                            val newRoom = Room(
                                id = RoomUtils.generateRoomId(currentZone, nextNum),
                                name = "New Room",
                                description = "Description",
                                position = Position(100f, 100f)
                            )
                            currentZone = currentZone.copy(
                                rooms = currentZone.rooms + newRoom
                            )
                            log.debug("Created new room with ID: ${newRoom.id}")
                        }
                    ) {
                        Text("Add Room")
                    }

                    // Canvas size controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Canvas Size:")
                        var widthGrids by remember { mutableStateOf((canvasWidth / currentZone.gridSize).toInt()) }
                        var heightGrids by remember { mutableStateOf((canvasHeight / currentZone.gridSize).toInt()) }

                        GridDimensionField(
                            gridCount = widthGrids,
                            onGridCountChange = { gridCount ->
                                widthGrids = gridCount
                                canvasWidth = (gridCount * currentZone.gridSize).toInt()
                            },
                            modifier = Modifier.width(120.dp),
                            minGrids = 1  // Allow any positive number
                        )
                        Text("×")
                        GridDimensionField(
                            gridCount = heightGrids,
                            onGridCountChange = { gridCount ->
                                heightGrids = gridCount
                                canvasHeight = (gridCount * currentZone.gridSize).toInt()
                            },
                            modifier = Modifier.width(120.dp),
                            minGrids = 1  // Allow any positive number
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Zoom controls
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0.1f) },
                        enabled = zoomLevel > 0.1f
                    ) {
                        Text("−")
                    }

                    Text("${(zoomLevel * 100).toInt()}%")

                    IconButton(
                        onClick = { zoomLevel = (zoomLevel + 0.1f).coerceAtMost(3f) },
                        enabled = zoomLevel < 3f
                    ) {
                        Text("+")
                    }

                    // Node size controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Node Size:")
                        OutlinedTextField(
                            value = nodeWidthText,
                            onValueChange = { text ->
                                nodeWidthText = text
                                text.toIntOrNull()?.let { width ->
                                    currentZone = currentZone.copy(nodeWidth = width.toFloat())
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            label = { Text("W") }
                        )
                        Text("×")
                        OutlinedTextField(
                            value = nodeHeightText,
                            onValueChange = { text ->
                                nodeHeightText = text
                                text.toIntOrNull()?.let { height ->
                                    currentZone = currentZone.copy(nodeHeight = height.toFloat())
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            label = { Text("H") }
                        )
                    }

                    // Snap to Grid control
                    Switch(
                        checked = currentZone.snapToGrid,
                        onCheckedChange = { snapEnabled ->
                            currentZone = currentZone.copy(snapToGrid = snapEnabled)
                            // If enabling snap, immediately snap all rooms to grid
                            if (snapEnabled) {
                                val snappedRooms = currentZone.rooms.map { room ->
                                    room.copy(position = currentZone.snapPosition(room.position))
                                }
                                currentZone = currentZone.copy(rooms = snappedRooms)
                            }
                        }
                    )
                    Text("Snap to Grid")

                    // Optional: Grid size control
                    Text("Grid Size:")
                    ComboBox(
                        value = currentZone.gridSize.toString(),
                        onValueChange = { newSize ->
                            newSize.toFloatOrNull()?.let { size ->
                                currentZone = currentZone.copy(gridSize = size)
                            }
                        },
                        items = listOf("10", "20", "40")
                    )
                }
            }

            // Main content
            Row(modifier = Modifier.weight(1f)) {
                ZoneCanvas(
                    zone = currentZone,
                    zoomLevel = zoomLevel,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    selectedRoom = selectedRoom,
                    onZoneChanged = { newZone ->
                        log.trace("Zone updated: {}", newZone.rooms.map { it.id })
                        currentZone = newZone
                    },
                    onRoomSelected = { room ->
                        log.trace("Room selection changed to: {}", room?.id)
                        selectedRoom = room
                    },
                    onConnectionStarted = { room, direction ->
                        log.debug("Connection started from {} in direction {}", room.id, direction)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Side panel for room details
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Text(
                        "Room Details",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (selectedRoom != null) {
                        OutlinedTextField(
                            value = selectedRoom!!.name,
                            onValueChange = { newName ->
                                val updatedRoom = selectedRoom!!.copy(name = newName)
                                currentZone = currentZone.copy(
                                    rooms = currentZone.rooms.map {
                                        if (it.id == selectedRoom!!.id) updatedRoom else it
                                    }
                                )
                                selectedRoom = updatedRoom
                            },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = selectedRoom!!.description,
                            onValueChange = { newDesc ->
                                val updatedRoom = selectedRoom!!.copy(description = newDesc)
                                currentZone = currentZone.copy(
                                    rooms = currentZone.rooms.map {
                                        if (it.id == selectedRoom!!.id) updatedRoom else it
                                    }
                                )
                                selectedRoom = updatedRoom
                            },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    } else {
                        Text("No room selected", color = Color.Gray)
                    }
                }
            }
        }
    }
}
