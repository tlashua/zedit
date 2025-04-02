package net.lashua.zonedit.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Room
import org.slf4j.LoggerFactory

/**
 * Component for displaying context menus
 */
object ContextMenuComponent {
    private val log = LoggerFactory.getLogger(ContextMenuComponent::class.java)

    /**
     * Displays a connection context menu
     *
     * @param connectionTriple The connection triple (source room, direction, destination room)
     * @param position The position of the context menu
     * @param onDismiss Callback when the menu is dismissed
     * @param onDeleteConnection Callback when the delete connection option is selected
     */
    @Composable
    fun ConnectionContextMenu(
        connectionTriple: Triple<Room, ExitDirection, Room>?,
        position: DpOffset?,
        onDismiss: () -> Unit,
        onDeleteConnection: (Room, ExitDirection) -> Unit
    ) {
        if (connectionTriple == null || position == null) return

        val (sourceRoom, direction, destRoom) = connectionTriple
        log.debug("Showing context menu for connection: {} -> {} ({})", sourceRoom.id, destRoom.id, direction)

        DropdownMenu(
            expanded = true,
            onDismissRequest = {
                log.debug("Context menu dismissed")
                onDismiss()
            },
            offset = position
        ) {
            DropdownMenuItem(
                text = { Text("Delete Connection") },
                onClick = {
                    log.debug("Delete connection menu item clicked")
                    log.debug("Removing connection: {} --[{}]--> {}", sourceRoom.id, direction, destRoom.id)
                    onDeleteConnection(sourceRoom, direction)
                }
            )
        }
    }
}
