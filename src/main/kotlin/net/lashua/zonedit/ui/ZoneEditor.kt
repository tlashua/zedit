package net.lashua.zonedit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import net.lashua.zonedit.model.Zone

@Composable
fun ZoneEditor(
    zone: Zone,
    modifier: Modifier = Modifier
) {
    var currentZone by remember { mutableStateOf(zone) }
    
    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            ZoneCanvas(
                zone = currentZone,
                onZoneChanged = { newZone -> 
                    currentZone = newZone
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}