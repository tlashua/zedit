package net.lashua.zonedit.ui

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

@Composable
fun ZoneEditor(
    zone: Zone,
    modifier: Modifier = Modifier
) {
    var currentZone by remember { mutableStateOf(zone) }
    var zoomLevel by remember { mutableStateOf(1f) }
    
    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            //  Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add Room button
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

                    // Canvas size display
                    Text("Canvas: 1000 × 1000")
                    
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
                }
            }
            
            // Canvas
            ZoneCanvas(
                zone = currentZone,
                zoomLevel = zoomLevel,
                onZoneChanged = { newZone ->
                    currentZone = newZone
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}