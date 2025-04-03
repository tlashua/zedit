package net.lashua.zonedit.ui

import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.ZoneCanvasState

class ZoneCanvasStateConnectionTest : FunSpec({

    context("Connection drag operations") {
        test("startConnectionDrag should initialize connection drag state") {
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

            val startPoint = Offset(50f, 15f)
            state.startConnectionDrag(room, ExitDirection.EAST, startPoint, "")

            state.connectionDragState shouldNotBe null
            state.connectionDragState?.sourceRoomId shouldBe room.id
            state.connectionDragState?.direction shouldBe ExitDirection.EAST
            state.connectionDragState?.currentPoint shouldBe startPoint
        }

        test("finalizeConnectionDrag should create a new room when not over existing room") {
            val room = Room(id = "test0", name = "Room", description = "Test room", position = Position(0f, 0f))
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room))

            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)

            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = onZoneChanged,
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            // Start drag
            val startPoint = Offset(50f, 15f)
            state.startConnectionDrag(room, ExitDirection.EAST, startPoint, "")

            // End drag at a point where there's no existing room
            val endPoint = Offset(150f, 15f)
            state.updateConnectionDragPoint(endPoint)

            // Finalize drag
            state.finalizeConnectionDrag(endPoint, 1.0f, 1.0f, 1000f, 800f)

            // Verify zone was updated with a new room
            val zoneSlot = slot<Zone>()
            verify { onZoneChanged(capture(zoneSlot)) }

            val updatedZone = zoneSlot.captured
            updatedZone.rooms.size shouldBe 2

            // Verify connection was created
            val newRoom = updatedZone.rooms.find { it.id != room.id }
            newRoom shouldNotBe null

            val updatedSourceRoom = updatedZone.rooms.find { it.id == room.id }
            updatedSourceRoom?.exits?.get(ExitDirection.EAST) shouldBe newRoom?.id
            newRoom?.exits?.get(ExitDirection.WEST) shouldBe room.id
        }
    }

    context("Connection removal") {
        test("removeConnection should remove a bi-directional connection") {
            // Create a zone with connected rooms
            val room1 = Room(
                id = "test0",
                name = "Room1",
                description = "Test room1",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1")
            )
            val room2 = Room(
                id = "test1",
                name = "Room2",
                description = "Test room2",
                position = Position(100f, 0f),
                exits = mapOf(ExitDirection.WEST to "test0")
            )
            val zone = Zone(id = "test", name = "Test", rooms = listOf(room1, room2))

            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)

            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = onZoneChanged,
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )

            // Remove connection
            state.removeConnection(room1, ExitDirection.EAST)

            // Verify zone was updated
            val zoneSlot = slot<Zone>()
            verify { onZoneChanged(capture(zoneSlot)) }

            val updatedZone = zoneSlot.captured

            // Verify connection was removed
            val updatedRoom1 = updatedZone.rooms.find { it.id == room1.id }
            val updatedRoom2 = updatedZone.rooms.find { it.id == room2.id }

            updatedRoom1?.exits?.get(ExitDirection.EAST) shouldBe null
            updatedRoom2?.exits?.get(ExitDirection.WEST) shouldBe null
        }
    }
})
