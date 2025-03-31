package net.lashua.zonedit

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import net.lashua.zonedit.model.*
import net.lashua.zonedit.ui.ZoneEditor
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("net.lashua.zonedit.Main")

fun main() = application {
    logger.info("Starting Zone Editor application")
    val windowState = remember { WindowState(width = 1500.dp, height = 1000.dp) }
    
    // Empty initial zone
    val testZone = Zone(
        id = "scratchpad",
        name = "scratchpad",
        nodeWidth = 100f,
        nodeHeight = 60f
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


