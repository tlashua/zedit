package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import java.util.UUID
import androidx.compose.ui.graphics.Color
import org.slf4j.LoggerFactory

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
    
    // Add this effect to update selectedRoom when rooms change
    LaunchedEffect(currentZone) {
        if (selectedRoom != null) {
            // Update selected room reference if it still exists in the zone
            selectedRoom = currentZone.rooms.find { it.id == selectedRoom?.id }
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