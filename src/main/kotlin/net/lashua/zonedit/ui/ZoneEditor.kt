package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import net.lashua.zonedit.io.ZoneSerializer
import java.util.UUID
import androidx.compose.ui.graphics.Color
import org.slf4j.LoggerFactory
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import androidx.compose.ui.awt.ComposeWindow

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

    // Function to handle file operations
    fun handleFileOperation(operation: (JFileChooser) -> Int) {
        fileChooser?.let { chooser ->
            val window = ComposeWindow()
            log.debug("Starting file operation with chooser: ${chooser.currentDirectory?.absolutePath}")
            
            // Determine operation type before executing it
            val isSaveOperation = operation == chooser::showSaveDialog
            log.debug("Operation type: ${if (isSaveOperation) "SAVE" else "OPEN"}")
            
            val result = operation(chooser)
            
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                log.debug("File selected: ${file.absolutePath}")
                try {
                    if (isSaveOperation) {
                        log.debug("Executing SAVE operation")
                        // Ensure .zone extension
                        val saveFile = if (!file.name.endsWith(".zone")) {
                            File(file.parentFile, "${file.name}.zone").also {
                                log.debug("Adding .zone extension. New path: ${it.absolutePath}")
                            }
                        } else file
                        
                        log.debug("About to save zone with ${currentZone.rooms.size} rooms to: ${saveFile.absolutePath}")
                        ZoneSerializer.saveZone(currentZone, saveFile)
                        log.info("Successfully saved zone to ${saveFile.absolutePath}")
                    } else {
                        log.debug("Executing OPEN operation")
                        currentZone = ZoneSerializer.loadZone(file)
                        nodeWidthText = currentZone.nodeWidth.toInt().toString()
                        nodeHeightText = currentZone.nodeHeight.toInt().toString()
                        selectedRoom = null
                        log.info("Successfully loaded zone from ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    log.error("File operation failed", e)
                    log.error("Failed path: ${file.absolutePath}")
                    e.printStackTrace()
                }
            } else {
                log.debug("File operation cancelled by user")
            }
            window.dispose()
        }
    }
    
    Surface(modifier = modifier.fillMaxSize()) {
        Column {
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
                            onClick = { handleFileOperation { it.showOpenDialog(null) } }
                        ) {
                            Text("Open")
                        }
                        
                        Button(
                            onClick = { handleFileOperation { it.showSaveDialog(null) } }
                        ) {
                            Text("Save")
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .padding(horizontal = 8.dp)
                    )

                    // Zone name input and renumber button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = currentZone.name,
                            onValueChange = { newName ->
                                currentZone = currentZone.copy(name = newName.take(20))
                            },
                            modifier = Modifier.width(200.dp),
                            label = { Text("Zone Name") },
                            singleLine = true
                        )
                        
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
                    }

                    Divider(
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
                        Text("Canvas:")
                        OutlinedTextField(
                            value = canvasWidth.toString(),
                            onValueChange = { 
                                canvasWidth = it.toIntOrNull()?.coerceIn(100, 10000) ?: canvasWidth 
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("×")
                        OutlinedTextField(
                            value = canvasHeight.toString(),
                            onValueChange = { 
                                canvasHeight = it.toIntOrNull()?.coerceIn(100, 10000) ?: canvasHeight 
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
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
                        log.debug("Zone updated: {}", newZone.rooms.map { it.id })
                        currentZone = newZone
                    },
                    onRoomSelected = { room ->
                        log.debug("Room selection changed to: {}", room?.id)
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