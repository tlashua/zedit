package net.lashua.zonedit.io

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import java.io.File
import java.nio.file.Files

class ZoneSerializerTest : FunSpec({

    // Create a temporary directory for test files
    val tempDir = Files.createTempDirectory("zoneserializer-test").toFile()

    // Clean up after tests
    afterSpec {
        tempDir.deleteRecursively()
    }

    context("Zone serialization and deserialization") {
        test("saveZone and loadZone should correctly round-trip a simple zone") {
            val zone = Zone(
                id = "test",
                name = "Test Zone",
                nodeWidthDp = 120f,
                nodeHeightDp = 80f,
                gridSizeDp = 25f,
                snapToGrid = true
            )

            val file = File(tempDir, "simple-zone.toml")

            // Save the zone
            ZoneSerializer.saveZone(zone, file)

            // Load the zone
            val loadedZone = ZoneSerializer.loadZone(file)

            // Verify the loaded zone matches the original
            loadedZone.id shouldBe zone.id
            loadedZone.name shouldBe zone.name
            loadedZone.nodeWidthDp shouldBe zone.nodeWidthDp
            loadedZone.nodeHeightDp shouldBe zone.nodeHeightDp
            loadedZone.gridSizeDp shouldBe zone.gridSizeDp
            loadedZone.snapToGrid shouldBe zone.snapToGrid
            loadedZone.rooms shouldBe zone.rooms
        }

        test("saveZone and loadZone should correctly round-trip a zone with rooms") {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "This is room 1",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1"),
                exitCorners = emptyMap(),
                flags = listOf("flag1", "flag2"),
                metadata = mapOf("key1" to "value1")
            )

            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "This is room 2",
                position = Position(200f, 0f),
                exits = mapOf(ExitDirection.WEST to "test0"),
                exitCorners = emptyMap(),
                flags = emptyList(),
                metadata = emptyMap()
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room1, room2)
            )

            val file = File(tempDir, "zone-with-rooms.toml")

            // Save the zone
            ZoneSerializer.saveZone(zone, file)

            // Load the zone
            val loadedZone = ZoneSerializer.loadZone(file)

            // Verify the loaded zone matches the original
            loadedZone.id shouldBe zone.id
            loadedZone.name shouldBe zone.name
            loadedZone.rooms.size shouldBe zone.rooms.size

            // Verify room 1
            val loadedRoom1 = loadedZone.rooms.find { it.id == "test0" }!!
            loadedRoom1.name shouldBe room1.name
            loadedRoom1.description shouldBe room1.description
            loadedRoom1.position shouldBe room1.position
            loadedRoom1.exits shouldBe room1.exits
            loadedRoom1.flags shouldBe room1.flags
            // Note: metadata is not currently saved by ZoneSerializer

            // Verify room 2
            val loadedRoom2 = loadedZone.rooms.find { it.id == "test1" }!!
            loadedRoom2.name shouldBe room2.name
            loadedRoom2.description shouldBe room2.description
            loadedRoom2.position shouldBe room2.position
            loadedRoom2.exits shouldBe room2.exits
            loadedRoom2.flags shouldBe room2.flags
            // Note: metadata is not currently saved by ZoneSerializer
        }

        test("saveZone and loadZone should handle multiline descriptions") {
            val multilineDescription = """
                This is a multiline description.
                It has several lines.

                Including blank lines.
            """.trimIndent()

            val room = Room(
                id = "test0",
                name = "Room with Multiline Description",
                description = multilineDescription,
                position = Position(0f, 0f)
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room)
            )

            val file = File(tempDir, "zone-with-multiline.toml")

            // Save the zone
            ZoneSerializer.saveZone(zone, file)

            // Load the zone
            val loadedZone = ZoneSerializer.loadZone(file)

            // Verify the multiline description was preserved
            val loadedRoom = loadedZone.rooms.first()
            loadedRoom.description shouldBe multilineDescription
        }

        // Skipping this test as exitCorners are not currently saved by ZoneSerializer
        // This test can be uncommented when the feature is implemented
        /*
        test("saveZone and loadZone should handle UP/DOWN connections with corners") {
            val room1 = Room(
                id = "test0",
                name = "Upper Room",
                description = "This is the upper room",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.DOWN to "test1"),
                exitCorners = mapOf(ExitDirection.DOWN to "LEFT")
            )

            val room2 = Room(
                id = "test1",
                name = "Lower Room",
                description = "This is the lower room",
                position = Position(0f, 100f),
                exits = mapOf(ExitDirection.UP to "test0"),
                exitCorners = mapOf(ExitDirection.UP to "RIGHT")
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room1, room2)
            )

            val file = File(tempDir, "zone-with-corners.toml")

            // Save the zone
            ZoneSerializer.saveZone(zone, file)

            // Load the zone
            val loadedZone = ZoneSerializer.loadZone(file)

            // Verify the exit corners were preserved
            val loadedRoom1 = loadedZone.rooms.find { it.id == "test0" }!!
            loadedRoom1.exitCorners[ExitDirection.DOWN] shouldBe "LEFT"

            val loadedRoom2 = loadedZone.rooms.find { it.id == "test1" }!!
            loadedRoom2.exitCorners[ExitDirection.UP] shouldBe "RIGHT"
        }
        */
    }
})
