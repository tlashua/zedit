package net.lashua.zonedit

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import net.lashua.zonedit.ui.ZoneEditor

fun main() = application {
    val windowState = remember { WindowState(width = 1500.dp, height = 1000.dp) }
    
    // Sample zone for testing
    val testZone = Zone(
        id = "test-zone",
        name = "Test Zone",
        nodeWidth = 100f,
        nodeHeight = 60f,
        rooms = listOf(
            Room(
                id = "room1",
                name = "Starting Room",
                description = "This is where it all begins",
                position = Position(100f, 100f)
            )
        )
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Zone Editor",
        state = windowState
    ) {
        Surface {
            ZoneEditor(zone = testZone)
        }
    }
}


