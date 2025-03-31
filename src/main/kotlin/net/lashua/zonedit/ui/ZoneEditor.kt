package net.lashua.zonedit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Position
import java.util.UUID
import androidx.compose.ui.graphics.Color

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
                    Button(
                        onClick = {
                            val newRoom = Room(
                                id = UUID.randomUUID().toString(),
                                name = "New Room",
                                description = "Description",
                                position = Position(100f, 100f)
                            )
                            currentZone = currentZone.copy(
                                rooms = currentZone.rooms + newRoom
                            )
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

                    // Add node size control
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
            
            // Main content area with updated canvas size
            Row(modifier = Modifier.weight(1f)) {
                // Left panel - Room List/Navigation (can be added later)
                Surface(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .border(1.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween // This will push minimap to bottom
                    ) {
                        Column {
                            Text("Room Navigator", style = MaterialTheme.typography.titleMedium)
                            // Room list will go here
                        }
                        
                        // Minimap at bottom
                        Column {
                            Text("Overview", style = MaterialTheme.typography.titleSmall)
                            ZoneMinimap(
                                zone = currentZone,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                minimapSize = 184  // 200 - 2*8dp padding
                            )
                        }
                    }
                }

                // Center - Canvas with new size parameters
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ZoneCanvas(
                        zone = currentZone,
                        zoomLevel = zoomLevel,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        onZoneChanged = { newZone ->
                            currentZone = newZone
                        },
                        onRoomSelected = { room ->
                            selectedRoom = room
                        },
                        selectedRoom = selectedRoom
                    )
                }

                // Right panel - Room Editor
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .border(1.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Room Properties", style = MaterialTheme.typography.titleMedium)
                        
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                            
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
}