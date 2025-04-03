package net.lashua.zonedit.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.mockk.verify
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import org.junit.Rule
import org.junit.Test

class ZoneCanvasTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Helper function to create a test zone with rooms
     */
    private fun createTestZone(): Zone {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room 1",
                position = Position(100f, 100f)
            )

            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room 2",
                position = Position(300f, 100f),
                exits = mapOf(ExitDirection.WEST to "test0")
            )

            val room3 = Room(
                id = "test2",
                name = "Room 3",
                description = "Test room 3",
                position = Position(100f, 300f),
                exits = mapOf(ExitDirection.NORTH to "test0")
            )

            val room1WithExits = room1.copy(
                exits = mapOf(
                    ExitDirection.EAST to "test1",
                    ExitDirection.SOUTH to "test2"
                )
            )

            return Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room1WithExits, room2, room3)
            )
    }

    @Test
    fun testZoneCanvasRendersRooms() {
        // Create a test zone
        val zone = createTestZone()

        // Mock callbacks
        val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
        val onRoomSelected = mockk<(Room?) -> Unit>(relaxed = true)

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = onZoneChanged,
                onRoomSelected = onRoomSelected
            )
        }

        // Wait for composition to be ready
        composeRule.waitForIdle()

        // Verify that the canvas is rendered
        // Note: Since rooms are drawn on a Canvas, we can't directly assert their presence
        // using semantics. Instead, we'll verify that the Canvas itself is rendered.
        composeRule.onNodeWithTag("zoneCanvas")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testRoomSelection() {
        // Create a test zone
        val zone = createTestZone()

        // Mock callbacks
        val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
        val onRoomSelected = mockk<(Room?) -> Unit>(relaxed = true)

        // Set up the composition
        composeRule.setContent {
            ZoneCanvas(
                zone = zone,
                canvasWidthDp = 1000.dp,
                canvasHeightDp = 800.dp,
                onZoneChanged = onZoneChanged,
                onRoomSelected = onRoomSelected,
                // Add testTag to make it easier to find in tests
                modifier = androidx.compose.ui.Modifier.semantics { testTag = "zoneCanvas" }
            )
        }

        // Wait for composition to be ready
        composeRule.waitForIdle()

        // Simulate a click at the position of the first room
        // Note: This is an approximation since we can't directly target the room
        // We're clicking at the center of where room1 should be
        composeRule.onNodeWithTag("zoneCanvas")
            .performTouchInput {
                click(Offset(100f, 100f))
            }

        // Verify that onRoomSelected was called with the first room
        verify { onRoomSelected(zone.rooms[0]) }
    }
}
