package net.lashua.zonedit.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.mockk.verify
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import org.junit.Rule
import org.junit.Test

class ContextMenuComponentTest2 {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionContextMenu_shouldDisplayDeleteOption() {
        // Create test data
        val sourceRoom = Room(
            id = "test0",
            name = "Source Room",
            description = "Test source room",
            position = Position(0f, 0f)
        )
        
        val destRoom = Room(
            id = "test1",
            name = "Destination Room",
            description = "Test destination room",
            position = Position(100f, 0f)
        )
        
        val connectionTriple = Triple(sourceRoom, ExitDirection.EAST, destRoom)
        val position = DpOffset(50.dp, 50.dp)
        
        // Set up the composition
        composeRule.setContent {
            ContextMenuComponent.ConnectionContextMenu(
                connectionTriple = connectionTriple,
                position = position,
                onDismiss = {},
                onDeleteConnection = { _, _ -> }
            )
        }
        
        // Verify the menu shows the expected option
        composeRule.onNodeWithText("Delete Connection").assertIsDisplayed()
    }
    
    @Test
    fun connectionContextMenu_shouldCallOnDeleteConnection() {
        // Create test data
        val sourceRoom = Room(
            id = "test0",
            name = "Source Room",
            description = "Test source room",
            position = Position(0f, 0f)
        )
        
        val destRoom = Room(
            id = "test1",
            name = "Destination Room",
            description = "Test destination room",
            position = Position(100f, 0f)
        )
        
        val connectionTriple = Triple(sourceRoom, ExitDirection.EAST, destRoom)
        val position = DpOffset(50.dp, 50.dp)
        
        // Create mock callbacks
        val onDeleteConnection = mockk<(Room, ExitDirection) -> Unit>(relaxed = true)
        
        // Set up the composition
        composeRule.setContent {
            ContextMenuComponent.ConnectionContextMenu(
                connectionTriple = connectionTriple,
                position = position,
                onDismiss = {},
                onDeleteConnection = onDeleteConnection
            )
        }
        
        // Click the "Delete Connection" option
        composeRule.onNodeWithText("Delete Connection").performClick()
        
        // Verify the callback was called with the correct parameters
        verify { onDeleteConnection(sourceRoom, ExitDirection.EAST) }
    }
    
    @Test
    fun connectionContextMenu_shouldNotShowWhenConnectionIsNull() {
        // Set up the composition with null connection
        composeRule.setContent {
            ContextMenuComponent.ConnectionContextMenu(
                connectionTriple = null,
                position = DpOffset(50.dp, 50.dp),
                onDismiss = {},
                onDeleteConnection = { _, _ -> }
            )
        }
        
        // Verify the menu is not shown
        // We can't directly assert that an element doesn't exist, so we'll use a try-catch
        var menuFound = false
        try {
            composeRule.onNodeWithText("Delete Connection").assertIsDisplayed()
            menuFound = true
        } catch (e: AssertionError) {
            // Expected - menu should not be found
        }
        assert(!menuFound) { "Menu should not be displayed" }
    }
}
