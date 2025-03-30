package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import net.lashua.zonedit.model.Zone

@Composable
fun ZoneCanvas(
    zone: Zone,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // TODO: Implement zone canvas with draggable rooms and connections
        zone.rooms.forEach { room ->
            // We'll implement draggable room nodes here
        }
    }
}