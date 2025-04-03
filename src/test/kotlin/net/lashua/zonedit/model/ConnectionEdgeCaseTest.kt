package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.lashua.zonedit.ui.state.ZoneCanvasState

class ConnectionEdgeCaseTest : FunSpec({

    context("Edge cases") {
        test("ConnectionManager should handle connections to non-existent rooms") {
            val room = Room(id = "test0", name = "Room", description = "Test room", position = Position(0f, 0f))
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room))

            val connectionManager = ConnectionManager()

            // Try to remove a connection to a non-existent room
            val result = connectionManager.removeConnection(
                room,
                ExitDirection.EAST,
                zone
            )

            // Should return null or handle gracefully
            result shouldBe null
        }

        test("ZoneCanvasState should handle finalizeConnectionDrag when no drag is in progress") {
            val room = Room(id = "test0", name = "Room", description = "Test room", position = Position(0f, 0f))
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room))

            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = {},
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            // Finalize without starting a drag
            state.finalizeConnectionDrag(Offset(150f, 15f), 1.0f, 1.0f, 1000f, 800f)

            // Should not throw an exception
            // This is a negative test - we're just verifying it doesn't crash
        }

        test("ConnectionManager should handle removing non-existent connections") {
            val room1 = Room(id = "test0", name = "Room1", description = "Test room1", position = Position(0f, 0f))
            val room2 = Room(id = "test1", name = "Room2", description = "Test room2", position = Position(100f, 0f))
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room1, room2))

            val connectionManager = ConnectionManager()

            // Try to remove a connection that doesn't exist
            val result = connectionManager.removeConnection(room1, ExitDirection.EAST, zone)

            // Should return null or handle gracefully
            result shouldBe null
        }

        test("ConnectionManager should handle finding connections when none exist") {
            val room = Room(id = "test0", name = "Room", description = "Test room", position = Position(0f, 0f))
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room))

            val connectionManager = ConnectionManager()

            // Try to find a connection near a point when none exist
            val result = connectionManager.findConnectionNearPoint(
                zone,
                Offset(50f, 15f),
                5f,
                1.0f,
                1.0f
            )

            // Should return null
            result shouldBe null
        }
    }
})
