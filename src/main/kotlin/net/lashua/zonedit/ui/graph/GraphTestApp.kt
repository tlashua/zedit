package net.lashua.zonedit.ui.graph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.MudRoom
import net.lashua.zonedit.model.MudZone
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.RoomData
import net.lashua.zonedit.viewmodel.MudZoneViewModel

/**
 * A test application for the graph editor.
 */
@Composable
fun GraphTestApp() {
    // Create a sample zone
    var zone by remember { mutableStateOf(createSampleZone()) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Graph Editor Test",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    zone = createSampleZone()
                    selectedRoomId = null
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Reset Zone")
            }

            // Create a view model for the zone
            val viewModel = remember(zone, selectedRoomId) {
                MudZoneViewModel(zone).apply {
                    // Set the selected room ID
                    selectRoom(selectedRoomId)
                }
            }

            // Handle zone changes
            LaunchedEffect(viewModel.zone) {
                if (viewModel.zone != zone) {
                    zone = viewModel.zone
                }
            }

            // Handle room selection changes
            LaunchedEffect(viewModel.selectedRoomId) {
                if (viewModel.selectedRoomId != selectedRoomId) {
                    selectedRoomId = viewModel.selectedRoomId
                }
            }

            GraphCanvas(
                viewModel = viewModel
            )
        }
    }
}

/**
 * Creates a sample zone for testing.
 */
private fun createSampleZone(): MudZone {
    // Create a new zone
    var zone = MudZone("Test Zone")

    // Create some rooms
    val room1 = MudRoom(
        id = "room1",
        data = RoomData(
            name = "Room 1",
            description = "This is room 1",
            position = Position(100f, 100f)
        )
    )

    val room2 = MudRoom(
        id = "room2",
        data = RoomData(
            name = "Room 2",
            description = "This is room 2",
            position = Position(300f, 100f)
        )
    )

    val room3 = MudRoom(
        id = "room3",
        data = RoomData(
            name = "Room 3",
            description = "This is room 3",
            position = Position(100f, 300f)
        )
    )

    // Add rooms to the zone
    zone = zone.addRoom(room1)
    zone = zone.addRoom(room2)
    zone = zone.addRoom(room3)

    // Connect the rooms
    zone = zone.connectRooms(room1, room2, ExitDirection.EAST)
    zone = zone.connectRooms(room1, room3, ExitDirection.SOUTH)

    return zone
}
