package net.lashua.zonedit

import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.ZoneEditor
import net.lashua.zonedit.ui.theme.ZoneEditorTheme
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
        ZoneEditorTheme {
            Surface {
                ZoneEditor(zone = testZone)
            }
        }
    }
}


