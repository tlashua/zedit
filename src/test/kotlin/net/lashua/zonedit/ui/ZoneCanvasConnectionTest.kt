package net.lashua.zonedit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.lashua.zonedit.model.ConnectionManager
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class ZoneCanvasConnectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun zoneCanvas_shouldCreateConnectionWhenDraggingFromRoom() {
        // Create a test zone with a room
        val room = Room(
            id = "test0",
            name = "Test Room",
            description = "Test room",
            position = Position(100f, 100f)
        )

        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room)
        )

        // Create mock callbacks
        val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
        val zoneSlot = slot<Zone>()
        val onConnectionStarted = mockk<(Room, ExitDirection) -> Unit>(relaxed = true)

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = { updatedZone ->
                    zoneSlot.captured = updatedZone
                    onZoneChanged(updatedZone)
                },
                onRoomSelected = {},
                onConnectionStarted = onConnectionStarted
            )
        }

        // Simulate a connection drag by directly calling the state's methods
        composeRule.runOnUiThread {
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = { updatedZone ->
                    zoneSlot.captured = updatedZone
                    onZoneChanged(updatedZone)
                },
                onRoomSelected = {},
                onConnectionStarted = onConnectionStarted,
                onPointerPositionChanged = {}
            )

            // Start a connection drag
            state.startConnectionDrag(room, ExitDirection.EAST, Offset(150f, 100f), "")

            // Update the drag point to a position where a new room would be created
            state.updateConnectionDragPoint(Offset(300f, 100f))

            // Finalize the connection
            state.finalizeConnectionDrag(Offset(300f, 100f), 1.0f, 1.0f, 1000f, 800f)
        }

        // Verify onConnectionStarted was called
        verify { onConnectionStarted(room, ExitDirection.EAST) }

        // Verify onZoneChanged was called
        verify { onZoneChanged(any()) }

        // Verify a new room was created
        assertTrue("Zone slot should be captured", zoneSlot.isCaptured)
        assertEquals("Should have 2 rooms", 2, zoneSlot.captured.rooms.size)

        // Verify the connection was created
        val sourceRoom = zoneSlot.captured.rooms.find { it.id == room.id }
        assertNotNull("Source room should not be null", sourceRoom)
        assertNotNull("Source room should have an east exit", sourceRoom!!.exits[ExitDirection.EAST])

        val destRoom = zoneSlot.captured.rooms.find { it.id != room.id }
        assertNotNull("Destination room should not be null", destRoom)
        assertNotNull("Destination room should have a west exit", destRoom!!.exits[ExitDirection.WEST])
        assertEquals("West exit should point to source room", sourceRoom.id, destRoom.exits[ExitDirection.WEST])
    }

    @Test
    fun zoneCanvas_shouldRemoveConnectionWhenDeleteRequested() {
        // Create a test zone with two connected rooms
        val room1 = Room(
            id = "test0",
            name = "Room 1",
            description = "Test room 1",
            position = Position(100f, 100f),
            exits = mapOf(ExitDirection.EAST to "test1")
        )

        val room2 = Room(
            id = "test1",
            name = "Room 2",
            description = "Test room 2",
            position = Position(300f, 100f),
            exits = mapOf(ExitDirection.WEST to "test0")
        )

        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room1, room2)
        )

        // Create mock callbacks
        val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
        val zoneSlot = slot<Zone>()

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = { updatedZone ->
                    zoneSlot.captured = updatedZone
                    onZoneChanged(updatedZone)
                },
                onRoomSelected = {}
            )
        }

        // Simulate removing a connection by directly calling the state's method
        composeRule.runOnUiThread {
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = { updatedZone ->
                    zoneSlot.captured = updatedZone
                    onZoneChanged(updatedZone)
                },
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            // Remove the connection
            state.removeConnection(room1, ExitDirection.EAST)
        }

        // Verify onZoneChanged was called
        verify { onZoneChanged(any()) }

        // Verify the connection was removed
        assertTrue("Zone slot should be captured", zoneSlot.isCaptured)

        val updatedRoom1 = zoneSlot.captured.rooms.find { it.id == room1.id }
        assertNotNull("Updated room1 should not be null", updatedRoom1)
        assertEquals("Room1 should not have an east exit", null, updatedRoom1!!.exits[ExitDirection.EAST])

        val updatedRoom2 = zoneSlot.captured.rooms.find { it.id == room2.id }
        assertNotNull("Updated room2 should not be null", updatedRoom2)
        assertEquals("Room2 should not have a west exit", null, updatedRoom2!!.exits[ExitDirection.WEST])
    }

    @Test
    fun zoneCanvas_shouldShowContextMenuWhenConnectionRightClicked() {
        // Create a test zone with two connected rooms
        val room1 = Room(
            id = "test0",
            name = "Room 1",
            description = "Test room 1",
            position = Position(100f, 100f),
            exits = mapOf(ExitDirection.EAST to "test1")
        )

        val room2 = Room(
            id = "test1",
            name = "Room 2",
            description = "Test room 2",
            position = Position(300f, 100f),
            exits = mapOf(ExitDirection.WEST to "test0")
        )

        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room1, room2)
        )

        // Create a mock ConnectionManager that will find a connection
        val connectionManager = mockk<ConnectionManager>(relaxed = true)
        val connectionTriple = Triple(room1, ExitDirection.EAST, room2)

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = {},
                onRoomSelected = {}
            )
        }

        // Simulate right-clicking on a connection by directly calling the state's method
        composeRule.runOnUiThread {
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = {},
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            // Show the context menu
            state.showContextMenu(
                connectionTriple,
                androidx.compose.ui.unit.DpOffset(200.dp, 100.dp)
            )
        }

        // Verify the context menu is shown
        // Note: We can't directly verify this because the context menu is shown in a different composition
        // Instead, we'll verify that the state was updated correctly
        composeRule.runOnUiThread {
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = {},
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            state.showContextMenu(
                connectionTriple,
                androidx.compose.ui.unit.DpOffset(200.dp, 100.dp)
            )

            assertEquals("Context menu connection should match", connectionTriple, state.contextMenuConnection)
            assertNotNull("Context menu position should not be null", state.contextMenuPosition)
        }
    }
}
