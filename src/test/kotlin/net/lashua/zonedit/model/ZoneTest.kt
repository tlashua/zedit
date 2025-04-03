package net.lashua.zonedit.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ZoneTest : FunSpec({

    context("Zone creation") {
        test("should create a zone with default values") {
            val zone = Zone(id = "test", name = "Test Zone")

            zone.id shouldBe "test"
            zone.name shouldBe "Test Zone"
            zone.rooms shouldBe emptyList()
            zone.nodeWidthDp shouldBe 100f
            zone.nodeHeightDp shouldBe 60f
            zone.gridSizeDp shouldBe 20f
            zone.snapToGrid shouldBe true
        }
    }

    context("Room numbering") {
        test("getNextRoomNumber should return 0 for empty zone") {
            val zone = Zone(id = "test", name = "Test Zone")

            zone.getNextRoomNumber() shouldBe 0
        }

        test("getNextRoomNumber should return next available number") {
            val zone = Zone(id = "test", name = "test")
            val room1 = Room(id = "test0", name = "Room 0", description = "Test room", position = Position(0f, 0f))
            val room2 = Room(id = "test1", name = "Room 1", description = "Test room", position = Position(100f, 0f))
            val zoneWithRooms = zone.copy(rooms = listOf(room1, room2))

            zoneWithRooms.getNextRoomNumber() shouldBe 2
        }

        test("getNextRoomNumber should handle non-sequential numbers") {
            val zone = Zone(id = "test", name = "test")
            val room1 = Room(id = "test0", name = "Room 0", description = "Test room", position = Position(0f, 0f))
            val room2 = Room(id = "test5", name = "Room 5", description = "Test room", position = Position(100f, 0f))
            val zoneWithRooms = zone.copy(rooms = listOf(room1, room2))

            zoneWithRooms.getNextRoomNumber() shouldBe 6
        }
    }

    context("Position snapping") {
        test("snapPosition should not modify position when snapToGrid is false") {
            val zone = Zone(id = "test", name = "Test Zone", snapToGrid = false)
            val position = Position(x = 25.3f, y = 37.8f)

            val snapped = zone.snapPosition(position)

            snapped shouldBe position
        }

        test("snapPosition should snap position to grid when snapToGrid is true") {
            val zone = Zone(id = "test", name = "Test Zone", gridSizeDp = 20f, snapToGrid = true)
            val position = Position(x = 25.3f, y = 37.8f)

            val snapped = zone.snapPosition(position)

            snapped.x shouldBe 20f
            snapped.y shouldBe 40f
        }
    }

    context("Room creation") {
        test("createRoomWithConnection should create a new room with connection") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Source room",
                position = Position(0f, 0f)
            )
            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(sourceRoom),
                snapToGrid = false // Disable snapping for predictable test results
            )

            val newPosition = Position(x = 200f, y = 0f)
            val updatedZone = zone.createRoomWithConnection(
                sourceRoom = sourceRoom,
                direction = ExitDirection.EAST,
                position = newPosition
            )

            // Verify the new room was created
            updatedZone.rooms.size shouldBe 2

            // Find the new room
            val newRoom = updatedZone.rooms.find { it.id != sourceRoom.id }
            newRoom shouldNotBe null

            // Verify the new room properties
            newRoom?.position shouldBe newPosition
            newRoom?.name shouldBe "New Room"

            // Verify the connection was created
            val updatedSource = updatedZone.rooms.find { it.id == sourceRoom.id }
            updatedSource?.exits?.get(ExitDirection.EAST) shouldBe newRoom?.id
            newRoom?.exits?.get(ExitDirection.WEST) shouldBe sourceRoom.id
        }
    }

    context("Room connections") {
        test("connectRooms should create a bi-directional connection between rooms") {
            val room1 = Room(id = "test0", name = "Room 1", description = "Test room", position = Position(0f, 0f))
            val room2 = Room(id = "test1", name = "Room 2", description = "Test room", position = Position(200f, 0f))
            val zone = Zone(id = "test", name = "Test Zone", rooms = listOf(room1, room2))

            val updatedZone = zone.connectRooms(
                sourceRoom = room1,
                targetRoom = room2,
                direction = ExitDirection.EAST
            )

            // Verify the connections were created
            val updatedRoom1 = updatedZone.rooms.find { it.id == room1.id }
            val updatedRoom2 = updatedZone.rooms.find { it.id == room2.id }

            updatedRoom1?.exits?.get(ExitDirection.EAST) shouldBe room2.id
            updatedRoom2?.exits?.get(ExitDirection.WEST) shouldBe room1.id
        }

        test("removeRoomConnection should remove a bi-directional connection") {
            // Create rooms with existing connections
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1")
            )
            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room",
                position = Position(200f, 0f),
                exits = mapOf(ExitDirection.WEST to "test0")
            )
            val zone = Zone(id = "test", name = "Test Zone", rooms = listOf(room1, room2))

            val updatedZone = zone.removeRoomConnection(
                sourceRoom = room1,
                direction = ExitDirection.EAST
            )

            // Verify the connections were removed
            val updatedRoom1 = updatedZone.rooms.find { it.id == room1.id }
            val updatedRoom2 = updatedZone.rooms.find { it.id == room2.id }

            updatedRoom1?.exits?.get(ExitDirection.EAST) shouldBe null
            updatedRoom2?.exits?.get(ExitDirection.WEST) shouldBe null
        }
    }
})
