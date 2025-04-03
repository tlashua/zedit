package net.lashua.zonedit.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone

class ZoneEditorIntegrationTest : FunSpec({

    context("Zone editing") {
        test("Adding a room should update the zone") {
            // Create a test zone
            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = emptyList()
            )

            // Create a new room
            val newRoom = Room(
                id = "test0",
                name = "New Room",
                description = "A new room",
                position = Position(100f, 100f)
            )

            // Add the room to the zone
            val updatedZone = zone.copy(
                rooms = zone.rooms + newRoom
            )

            // Verify the room was added
            updatedZone.rooms.size shouldBe 1
            updatedZone.rooms[0] shouldBe newRoom
        }

        test("Updating room properties should update the zone") {
            // Create a test zone with a room
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test description",
                position = Position(100f, 100f)
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room)
            )

            // Update the room
            val updatedRoom = room.copy(
                name = "Updated Room",
                description = "Updated description"
            )

            // Update the zone
            val updatedZone = zone.copy(
                rooms = zone.rooms.map {
                    if (it.id == updatedRoom.id) updatedRoom else it
                }
            )

            // Verify the room was updated
            updatedZone.rooms.size shouldBe 1
            updatedZone.rooms[0].name shouldBe "Updated Room"
            updatedZone.rooms[0].description shouldBe "Updated description"
        }

        test("Changing zone properties should update the zone") {
            // Create a test zone
            val zone = Zone(
                id = "test",
                name = "Test Zone",
                nodeWidthDp = 100f,
                nodeHeightDp = 80f,
                gridSizeDp = 20f,
                snapToGrid = true
            )

            // Update zone properties
            val updatedZone = zone.copy(
                name = "Updated Zone",
                nodeWidthDp = 120f,
                nodeHeightDp = 90f,
                gridSizeDp = 25f,
                snapToGrid = false
            )

            // Verify the properties were updated
            updatedZone.name shouldBe "Updated Zone"
            updatedZone.nodeWidthDp shouldBe 120f
            updatedZone.nodeHeightDp shouldBe 90f
            updatedZone.gridSizeDp shouldBe 25f
            updatedZone.snapToGrid shouldBe false
        }
    }
})
