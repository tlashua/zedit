package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset

data class ConnectionDragState(
    val sourceRoomId: String,
    val direction: ExitDirection,
    val currentPoint: Offset
)