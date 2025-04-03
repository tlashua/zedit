package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf

class ConnectionManagerTest : FunSpec({

    val connectionManager = ConnectionManager()

    // Helper function to create a test zone with two connected rooms
    fun createTestZoneWithConnectedRooms(): Triple<Zone, Room, Room> {
        val room1 = Room(
            id = "test0",
            name = "Source Room",
            description = "Source room",
            position = Position(0f, 0f),
            exits = mapOf(ExitDirection.EAST to "test1"),
            exitCorners = emptyMap()
        )
        val room2 = Room(
            id = "test1",
            name = "Destination Room",
            description = "Destination room",
            position = Position(200f, 0f),
            exits = mapOf(ExitDirection.WEST to "test0"),
            exitCorners = emptyMap()
        )
        val zone = Zone(
            id = "test",
            name = "Test Zone",
            rooms = listOf(room1, room2)
        )

        return Triple(zone, room1, room2)
    }

    context("Direction operations") {
        test("getOppositeDirection should return the correct opposite direction") {
            connectionManager.getOppositeDirection(ExitDirection.NORTH) shouldBe ExitDirection.SOUTH
            connectionManager.getOppositeDirection(ExitDirection.SOUTH) shouldBe ExitDirection.NORTH
            connectionManager.getOppositeDirection(ExitDirection.EAST) shouldBe ExitDirection.WEST
            connectionManager.getOppositeDirection(ExitDirection.WEST) shouldBe ExitDirection.EAST
            connectionManager.getOppositeDirection(ExitDirection.UP) shouldBe ExitDirection.DOWN
            connectionManager.getOppositeDirection(ExitDirection.DOWN) shouldBe ExitDirection.UP
        }
    }

    context("Connection point operations") {
        test("getConnectionPoints should return all connection points for a room") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)

            val points = connectionManager.getConnectionPoints(room, rect)

            // Should have 8 connection points (N, S, E, W, UP-LEFT, UP-RIGHT, DOWN-LEFT, DOWN-RIGHT)
            points.size shouldBe 8

            // Verify the cardinal direction points
            points.find { it.direction == ExitDirection.NORTH }?.position shouldBe Offset(50f, 0f)
            points.find { it.direction == ExitDirection.SOUTH }?.position shouldBe Offset(50f, 60f)
            points.find { it.direction == ExitDirection.EAST }?.position shouldBe Offset(100f, 30f)
            points.find { it.direction == ExitDirection.WEST }?.position shouldBe Offset(0f, 30f)

            // Verify the UP points
            val upLeft = points.find { it.direction == ExitDirection.UP && it.corner == "LEFT" }
            upLeft shouldNotBe null
            upLeft?.position shouldBe Offset(0f, 0f)

            val upRight = points.find { it.direction == ExitDirection.UP && it.corner == "RIGHT" }
            upRight shouldNotBe null
            upRight?.position shouldBe Offset(100f, 0f)

            // Verify the DOWN points
            val downLeft = points.find { it.direction == ExitDirection.DOWN && it.corner == "LEFT" }
            downLeft shouldNotBe null
            downLeft?.position shouldBe Offset(0f, 60f)

            val downRight = points.find { it.direction == ExitDirection.DOWN && it.corner == "RIGHT" }
            downRight shouldNotBe null
            downRight?.position shouldBe Offset(100f, 60f)
        }

        test("getConnectionPoint should return the correct point for a direction") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)

            // Test cardinal directions
            connectionManager.getConnectionPoint(room, rect, ExitDirection.NORTH) shouldBe Offset(50f, 0f)
            connectionManager.getConnectionPoint(room, rect, ExitDirection.SOUTH) shouldBe Offset(50f, 60f)
            connectionManager.getConnectionPoint(room, rect, ExitDirection.EAST) shouldBe Offset(100f, 30f)
            connectionManager.getConnectionPoint(room, rect, ExitDirection.WEST) shouldBe Offset(0f, 30f)

            // Test UP with explicit corner
            connectionManager.getConnectionPoint(room, rect, ExitDirection.UP, "LEFT") shouldBe Offset(0f, 0f)
            connectionManager.getConnectionPoint(room, rect, ExitDirection.UP, "RIGHT") shouldBe Offset(100f, 0f)

            // Test DOWN with explicit corner
            connectionManager.getConnectionPoint(room, rect, ExitDirection.DOWN, "LEFT") shouldBe Offset(0f, 60f)
            connectionManager.getConnectionPoint(room, rect, ExitDirection.DOWN, "RIGHT") shouldBe Offset(100f, 60f)
        }
    }

    context("Connection visualization") {
        test("shouldDrawConnection should determine when to draw a connection based on room positions") {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room",
                position = Position(0f, 100f) // room2 is below room1
            )

            // For NORTH/SOUTH, should draw if source is below or at same level as destination
            connectionManager.shouldDrawConnection(room1, ExitDirection.SOUTH, room2) shouldBe false
            connectionManager.shouldDrawConnection(room2, ExitDirection.NORTH, room1) shouldBe true

            // For EAST/WEST/UP/DOWN, should draw if source ID is <= destination ID
            connectionManager.shouldDrawConnection(room1, ExitDirection.EAST, room2) shouldBe true
            connectionManager.shouldDrawConnection(room2, ExitDirection.WEST, room1) shouldBe false
        }

        test("getConnectionPoints should return correct points for a connection") {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room",
                position = Position(200f, 0f) // room2 is to the right of room1
            )

            val sourceRect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)
            val destRect = Rect(left = 200f, top = 0f, right = 300f, bottom = 60f)

            // Test EAST connection points
            val (sourcePoint, destPoint) = connectionManager.getConnectionPoints(
                ExitDirection.EAST, sourceRect, destRect, room1, room2
            )

            sourcePoint shouldBe Offset(100f, 30f) // Right middle of room1
            destPoint shouldBe Offset(200f, 30f) // Left middle of room2
        }

        test("getConnectionPoints should handle UP/DOWN connections with corners") {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room",
                position = Position(0f, 0f),
                exitCorners = mapOf(ExitDirection.DOWN to "LEFT")
            )
            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room",
                position = Position(0f, 100f), // room2 is below room1
                exitCorners = mapOf(ExitDirection.UP to "RIGHT")
            )

            val sourceRect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)
            val destRect = Rect(left = 0f, top = 100f, right = 100f, bottom = 160f)

            // Test DOWN connection points with corners
            val (sourcePoint, destPoint) = connectionManager.getConnectionPoints(
                ExitDirection.DOWN, sourceRect, destRect, room1, room2
            )

            sourcePoint shouldBe Offset(0f, 60f) // Bottom left of room1 (LEFT corner)
            destPoint shouldBe Offset(100f, 100f) // Top right of room2 (RIGHT corner)
        }

        test("getConnectionColor should return the correct color for a direction") {
            // Cardinal directions should be gray
            connectionManager.getConnectionColor(ExitDirection.NORTH) shouldBe Color.Gray
            connectionManager.getConnectionColor(ExitDirection.EAST) shouldBe Color.Gray
            connectionManager.getConnectionColor(ExitDirection.SOUTH) shouldBe Color.Gray
            connectionManager.getConnectionColor(ExitDirection.WEST) shouldBe Color.Gray

            // UP/DOWN should be green
            connectionManager.getConnectionColor(ExitDirection.UP) shouldBe Color.Green
            connectionManager.getConnectionColor(ExitDirection.DOWN) shouldBe Color.Green
        }
    }

    context("Connection creation and removal") {
        test("createConnection should create a bi-directional connection between rooms") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Source room",
                position = Position(0f, 0f)
            )
            val destRoom = Room(
                id = "test1",
                name = "Destination Room",
                description = "Destination room",
                position = Position(200f, 0f)
            )

            val (updatedSource, updatedDest) = connectionManager.createConnection(
                sourceRoom,
                ExitDirection.EAST,
                destRoom
            )

            // Verify the connections were created
            updatedSource.exits[ExitDirection.EAST] shouldBe destRoom.id
            updatedDest.exits[ExitDirection.WEST] shouldBe sourceRoom.id
        }

        test("createConnection should handle UP/DOWN connections with corners") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Source room",
                position = Position(0f, 0f)
            )
            val destRoom = Room(
                id = "test1",
                name = "Destination Room",
                description = "Destination room",
                position = Position(50f, 100f)
            )

            val (updatedSource, updatedDest) = connectionManager.createConnection(
                sourceRoom,
                ExitDirection.DOWN,
                destRoom,
                "LEFT"
            )

            // Verify the connections were created with the correct corners
            updatedSource.exits[ExitDirection.DOWN] shouldBe destRoom.id
            updatedSource.exitCorners[ExitDirection.DOWN] shouldBe "LEFT"

            updatedDest.exits[ExitDirection.UP] shouldBe sourceRoom.id
            updatedDest.exitCorners[ExitDirection.UP] shouldBe "RIGHT" // Should be opposite of source
        }

        test("removeConnection should remove a bi-directional connection") {
            // Create rooms with existing connections
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Source room",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1"),
                exitCorners = emptyMap()
            )
            val destRoom = Room(
                id = "test1",
                name = "Destination Room",
                description = "Destination room",
                position = Position(200f, 0f),
                exits = mapOf(ExitDirection.WEST to "test0"),
                exitCorners = emptyMap()
            )
            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(sourceRoom, destRoom)
            )

            val result = connectionManager.removeConnection(sourceRoom, ExitDirection.EAST, zone)

            // Verify the result is not null
            result shouldNotBe null

            // Verify the connections were removed
            val (updatedSource, updatedDest) = result!!
            updatedSource.exits[ExitDirection.EAST] shouldBe null
            updatedDest.exits[ExitDirection.WEST] shouldBe null
        }
    }

    context("Proximity detection") {
        test("isNearPoint should detect when a point is near another point") {
            val point = Offset(10f, 10f)
            val target = Offset(12f, 12f)

            connectionManager.isNearPoint(point, target, 5f) shouldBe true
            connectionManager.isNearPoint(point, target, 2f) shouldBe false
        }

        test("isNearLine should detect when a point is near a line") {
            val point = Offset(50f, 12f)
            val start = Offset(0f, 10f)
            val end = Offset(100f, 10f)

            connectionManager.isNearLine(point, start, end, 5f) shouldBe true
            connectionManager.isNearLine(point, start, end, 1f) shouldBe false
        }
    }

    context("Connection finding") {
        test("findClosestConnectionPoint should find the closest connection point") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)
            val point = Offset(52f, 2f) // Close to the NORTH connection point

            val result = connectionManager.findClosestConnectionPoint(room, rect, point, 5f)

            result shouldNotBe null
            result?.direction shouldBe ExitDirection.NORTH
        }

        test("findClosestConnectionPoint should return null when no connection point is near") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)
            val point = Offset(150f, 150f) // Far from any connection point

            val result = connectionManager.findClosestConnectionPoint(room, rect, point, 5f)

            result shouldBe null
        }

        test("findConnectionNearPoint should find a connection near a point") {
            val (zone, room1, room2) = createTestZoneWithConnectedRooms()
            val density = 1.0f
            val zoomLevel = 1.0f

            // Point near the connection line between room1 and room2
            val point = Offset(150f, 30f)

            val result = connectionManager.findConnectionNearPoint(zone, point, 10f, density, zoomLevel)

            result shouldNotBe null
            val (sourceRoom, direction, destRoom) = result!!
            sourceRoom.id shouldBe room1.id
            direction shouldBe ExitDirection.EAST
            destRoom.id shouldBe room2.id
        }

        test("findConnectionNearPoint should return null when no connection is near") {
            val (zone, _, _) = createTestZoneWithConnectedRooms()
            val density = 1.0f
            val zoomLevel = 1.0f

            // Point far from any connection
            val point = Offset(150f, 150f)

            val result = connectionManager.findConnectionNearPoint(zone, point, 10f, density, zoomLevel)

            result shouldBe null
        }

        test("findRoomAtPoint should find a room at a given point") {
            val (zone, room1, _) = createTestZoneWithConnectedRooms()
            val density = 1.0f
            val zoomLevel = 1.0f

            // Point inside room1
            val point = Offset(50f, 30f)

            val result = connectionManager.findRoomAtPoint(zone, point, density, zoomLevel)

            result shouldNotBe null
            result?.id shouldBe room1.id
        }

        test("findRoomAtPoint should return null when no room is at the point") {
            val (zone, _, _) = createTestZoneWithConnectedRooms()
            val density = 1.0f
            val zoomLevel = 1.0f

            // Point not inside any room
            val point = Offset(150f, 150f)

            val result = connectionManager.findRoomAtPoint(zone, point, density, zoomLevel)

            result shouldBe null
        }
    }

    context("Connection retrieval") {
        test("getConnection should return a Connection object for valid connections") {
            val (_, room1, room2) = createTestZoneWithConnectedRooms()

            val connection = connectionManager.getConnection(room1, ExitDirection.EAST, room2)

            connection shouldNotBe null
            connection?.source?.room?.id shouldBe room1.id
            connection?.source?.direction shouldBe ExitDirection.EAST
            connection?.destination?.room?.id shouldBe room2.id
            connection?.destination?.direction shouldBe ExitDirection.WEST
        }

        test("getConnection should return null for non-existent connections") {
            val room1 = Room(
                id = "test0",
                name = "Room 1",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val room2 = Room(
                id = "test1",
                name = "Room 2",
                description = "Test room",
                position = Position(200f, 0f)
            )

            val connection = connectionManager.getConnection(room1, ExitDirection.EAST, room2)

            connection shouldBe null
        }

        test("getConnection should return null for non-bidirectional connections") {
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
                position = Position(200f, 0f)
                // No return connection to room1
            )

            val connection = connectionManager.getConnection(room1, ExitDirection.EAST, room2)

            connection shouldBe null
        }
    }

    context("Arrow calculation") {
        test("calculateAngle should return the correct angle between two points") {
            val start = Offset(0f, 0f)
            val end = Offset(10f, 0f) // Horizontal line to the right

            val angle = connectionManager.calculateAngle(start, end)

            angle shouldBe 0f
        }

        test("calculateArrowPoints should return the correct points for an arrow") {
            val point = Offset(100f, 100f)
            val angle = 0f // Pointing to the right
            val length = 10f
            val arrowAngle = kotlin.math.PI.toFloat() / 4f // 45 degrees

            val (point1, point2) = connectionManager.calculateArrowPoints(point, angle, length, arrowAngle)

            // Point1 should be above and to the left of the main point
            point1.x shouldBe (100f - 10f * kotlin.math.cos(-kotlin.math.PI.toFloat() / 4f))
            point1.y shouldBe (100f - 10f * kotlin.math.sin(-kotlin.math.PI.toFloat() / 4f))

            // Point2 should be below and to the left of the main point
            point2.x shouldBe (100f - 10f * kotlin.math.cos(kotlin.math.PI.toFloat() / 4f))
            point2.y shouldBe (100f - 10f * kotlin.math.sin(kotlin.math.PI.toFloat() / 4f))
        }
    }
})
