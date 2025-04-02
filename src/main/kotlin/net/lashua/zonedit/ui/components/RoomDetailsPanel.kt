package net.lashua.zonedit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.Room

@Composable
fun RoomDetailsPanel(
    selectedRoom: Room?,
    onRoomUpdated: (Room) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Room Details",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (selectedRoom != null) {
            OutlinedTextField(
                value = selectedRoom.name,
                onValueChange = { newName ->
                    onRoomUpdated(selectedRoom.copy(name = newName))
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedLabelColor = LocalContentColor.current,
                    focusedLabelColor = LocalContentColor.current
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = selectedRoom.description,
                onValueChange = { newDesc ->
                    onRoomUpdated(selectedRoom.copy(description = newDesc))
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedLabelColor = LocalContentColor.current,
                    focusedLabelColor = LocalContentColor.current
                )
            )
        } else {
            Text("No room selected", color = Color.Gray)
        }
    }
}