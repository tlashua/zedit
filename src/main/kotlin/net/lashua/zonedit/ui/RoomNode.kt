package net.lashua.zonedit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.ExitDirection

@Composable
fun RoomNode(
    room: Room,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onPositionChanged: (Position) -> Unit,
    onConnectionStarted: (Room, ExitDirection) -> Unit = { _, _ -> },
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Blue else Color.Black
            )
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onPositionChanged(Position(
                            x = room.position.x + dragAmount.x,
                            y = room.position.y + dragAmount.y
                        ))
                    }
                )
            }
    ) {
        Column {
            Text(room.name)
            Text(
                room.id,
                color = Color.Gray
            )
        }

        // Show connection points when selected
        if (selected) {
            // North connection point
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Blue, CircleShape)
                    .align(Alignment.TopCenter)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                onConnectionStarted(room, ExitDirection.NORTH)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                            }
                        )
                    }
            )
            // South connection point
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Blue, CircleShape)
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                onConnectionStarted(room, ExitDirection.SOUTH)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                            }
                        )
                    }
            )
            // East connection point
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Blue, CircleShape)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                onConnectionStarted(room, ExitDirection.EAST)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                            }
                        )
                    }
            )
            // West connection point
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Blue, CircleShape)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                onConnectionStarted(room, ExitDirection.WEST)
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