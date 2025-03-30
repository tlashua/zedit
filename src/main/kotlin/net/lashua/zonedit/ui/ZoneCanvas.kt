package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Position

@Composable
fun ZoneCanvas(
    zone: Zone,
    onZoneChanged: (Zone) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        for (room in zone.rooms) {
            key(room.id) {  // Add key to help Compose track individual rooms
                RoomNode(
                    room = room,
                    modifier = Modifier.offset(
                        x = room.position.x.dp,
                        y = room.position.y.dp
                    ),
                    onPositionChanged = { newPosition ->
                        val updatedRooms = zone.rooms.map { r ->
                            if (r.id == room.id) r.copy(position = newPosition) else r
                        }
                        onZoneChanged(zone.copy(rooms = updatedRooms))
                    }
                )
            }
        }
    }
}