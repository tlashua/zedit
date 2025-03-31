package net.lashua.zonedit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*

@Composable
fun RoomNode(
    room: Room,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onPositionChanged: (Position) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Blue else Color.Black
            )
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick()
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

        if (selected) {
            listOf(
                ExitDirection.NORTH to Alignment.TopCenter,
                ExitDirection.SOUTH to Alignment.BottomCenter,
                ExitDirection.EAST to Alignment.CenterEnd,
                ExitDirection.WEST to Alignment.CenterStart
            ).forEach { (direction, alignment) ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Blue, CircleShape)
                        .align(alignment)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { _ ->
                                    onConnectionStarted(room, direction)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                }
                            )
                        }
                )
            }
        }
    }
}