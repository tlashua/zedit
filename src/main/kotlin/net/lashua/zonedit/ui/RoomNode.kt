package net.lashua.zonedit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.Room

@Composable
fun RoomNode(
    room: Room,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Blue else Color.Black
            )
            .padding(8.dp)
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