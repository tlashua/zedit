package net.lashua.zonedit.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.rememberZoneCanvasState
import org.junit.Rule
import org.junit.Test

class RememberZoneCanvasStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testRememberZoneCanvasState() {
        // Create test data
        val room = Room(
            id = "test0",
            name = "Test Room",
            description = "Test room",
            position = Position(0f, 0f)
        )

        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room)
        )

        var capturedState: Any? = null

        // Set up the composition
        composeRule.setContent {
            val state = rememberZoneCanvasState(
                zone = zone,
                selectedRoom = room,
                onZoneChanged = {},
                onRoomSelected = {}
            )

            // Capture the state
            capturedState = state
        }

        // Wait for composition to complete
        composeRule.waitForIdle()

        // Verify the state was created correctly
        capturedState shouldNotBe null

        // Access the state properties
        val state = capturedState as net.lashua.zonedit.ui.state.ZoneCanvasState
        state.zone shouldBe zone
        state.selectedRoom shouldBe room
    }

    @Test
    fun testRememberZoneCanvasStateWithNullSelectedRoom() {
        // Create test data
        val room = Room(
            id = "test0",
            name = "Test Room",
            description = "Test room",
            position = Position(0f, 0f)
        )

        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room)
        )

        var capturedState: Any? = null

        // Set up the composition
        composeRule.setContent {
            val state = rememberZoneCanvasState(
                zone = zone,
                selectedRoom = null,
                onZoneChanged = {},
                onRoomSelected = {}
            )

            // Capture the state
            capturedState = state
        }

        // Wait for composition to complete
        composeRule.waitForIdle()

        // Verify the state was created correctly
        capturedState shouldNotBe null

        // Access the state properties
        val state = capturedState as net.lashua.zonedit.ui.state.ZoneCanvasState
        state.zone shouldBe zone
        state.selectedRoom shouldBe null
    }

    @Test
    fun testRememberZoneCanvasStateWithCallbacks() {
        // Create test data
        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = emptyList()
        )

        var zoneChangedCalled = false
        var roomSelectedCalled = false
        var connectionStartedCalled = false
        var pointerPositionChangedCalled = false

        // Set up the composition
        composeRule.setContent {
            val state = rememberZoneCanvasState(
                zone = zone,
                selectedRoom = null,
                onZoneChanged = { zoneChangedCalled = true },
                onRoomSelected = { roomSelectedCalled = true },
                onConnectionStarted = { _, _ -> connectionStartedCalled = true },
                onPointerPositionChanged = { pointerPositionChangedCalled = true }
            )

            // Capture the state and trigger callbacks
            state.updateZone(zone.copy(name = "Updated Zone"))
            state.updateSelectedRoom(Room(id = "test0", name = "Test Room", description = "Test", position = Position(0f, 0f)))
            state.updatePointerPosition(androidx.compose.ui.geometry.Offset(0f, 0f))
        }

        // Wait for composition to complete
        composeRule.waitForIdle()

        // Verify callbacks were called
        zoneChangedCalled shouldBe true
        roomSelectedCalled shouldBe true
        pointerPositionChangedCalled shouldBe true
    }
}
