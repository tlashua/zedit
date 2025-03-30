package net.lashua.zonedit.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Position
import kotlin.math.roundToInt

@Composable
fun ZoneCanvas(
    zone: Zone,
    onZoneChanged: (Zone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState)
            .size(5000.dp)
    ) {
        for (room in zone.rooms) {
            key(room.id) {
                var position by remember(room.id) { 
                    mutableStateOf(Offset(room.position.x, room.position.y)) 
                }
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                        .border(
                            width = 1.dp,
                            color = Color.Black
                        )
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                
                                // Calculate new position with bounds checking
                                val newX = if (dragAmount.x < 0) {
                                    maxOf(position.x + dragAmount.x, 0f)
                                } else {
                                    minOf(position.x + dragAmount.x, 4900f)
                                }
                                
                                val newY = if (dragAmount.y < 0) {
                                    maxOf(position.y + dragAmount.y, 0f)
                                } else {
                                    minOf(position.y + dragAmount.y, 4900f)
                                }
                                
                                position = Offset(newX, newY)
                                
                                // Update the zone with new position
                                val updatedRooms = zone.rooms.map { r ->
                                    if (r.id == room.id) {
                                        r.copy(position = Position(newX, newY))
                                    } else r
                                }
                                onZoneChanged(zone.copy(rooms = updatedRooms))
                            }
                        }
                ) {
                    Column {
                        Text(room.name)
                        Text(
                            room.id,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}