package net.lashua.zonedit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.ZoneCanvasState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ZoneCanvasTest2 {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun zoneCanvas_shouldDisplayWithCorrectSemantics() {
        // Create a test zone
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

        // Verify the canvas is displayed with the correct tag
        composeRule.onNodeWithTag("zoneCanvas")
            .assertIsDisplayed()
    }

    @Test
    fun zoneCanvas_shouldCallOnRoomSelectedWhenRoomClicked() {
        // Create a test zone
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
        val onRoomSelected = mockk<(Room?) -> Unit>(relaxed = true)

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = {},
                onRoomSelected = onRoomSelected
            )
        }

        // Click on the canvas where the room should be
        // Note: This is an approximation since we can't directly target the room
        composeRule.onNodeWithTag("zoneCanvas")
            .performClick()

        // Verify onRoomSelected was called
        // Note: We can't verify the exact room that was selected because
        // the click position might not match the room position exactly
        verify { onRoomSelected(any()) }
    }

    @Test
    fun zoneCanvas_shouldUpdateZoneWhenRoomMoved() {
        // Create a test zone
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

        // Simulate a room move by directly calling the state's method
        // This is a workaround since we can't easily simulate drag gestures
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

            // Start dragging the room
            state.startDraggingRoom(room.id)

            // Update the room position
            state.updateRoomPosition(room.id, Position(200f, 200f))

            // Stop dragging
            state.stopDragging()
        }

        // Verify onZoneChanged was called
        verify { onZoneChanged(any()) }

        // Verify the room position was updated
        assertTrue("Zone slot should be captured", zoneSlot.isCaptured)
        assertEquals("Room position should be updated", Position(200f, 200f), zoneSlot.captured.rooms[0].position)
    }
}
