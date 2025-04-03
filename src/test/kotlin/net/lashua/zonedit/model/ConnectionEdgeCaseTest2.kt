package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Additional tests for edge cases in ConnectionManager
 */
class ConnectionEdgeCaseTest2 : FunSpec({

    val connectionManager = ConnectionManager()

    context("Self-connections") {
        test("ConnectionManager should handle a room connected to itself") {
            val room = Room(
                id = "test0",
                name = "Room",
                description = "Test room",
                position = Position(0f, 0f)
            )

            val (updatedRoom, _) = connectionManager.createConnection(
                room,
                ExitDirection.EAST,
                room
            )

            // Verify the room has a connection to itself
            updatedRoom.exits[ExitDirection.EAST] shouldBe room.id
        }

        test("ConnectionManager should correctly remove a self-connection") {
            val room = Room(
                id = "test0",
                name = "Room",
                description = "Test room",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test0")
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(room)
            )

            val result = connectionManager.removeConnection(room, ExitDirection.EAST, zone)

            // Verify the connection was removed
            result shouldNotBe null
            val (updatedRoom, _) = result!!
            updatedRoom.exits[ExitDirection.EAST] shouldBe null
        }
    }

    context("Multiple connections") {
        test("ConnectionManager should handle multiple connections from different directions") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Source room",
                position = Position(0f, 0f)
            )

            val destRoom1 = Room(
                id = "test1",
                name = "Destination Room 1",
                description = "Destination room 1",
                position = Position(100f, 0f)
            )

            val destRoom2 = Room(
                id = "test2",
                name = "Destination Room 2",
                description = "Destination room 2",
                position = Position(0f, 100f)
            )

            // Create first connection (EAST)
            val (updatedSource1, _) = connectionManager.createConnection(
                sourceRoom,
                ExitDirection.EAST,
                destRoom1
            )

            // Create second connection (SOUTH)
            val (updatedSource2, _) = connectionManager.createConnection(
                updatedSource1,
                ExitDirection.SOUTH,
                destRoom2
            )

            // Verify both connections exist
            updatedSource2.exits[ExitDirection.EAST] shouldBe destRoom1.id
            updatedSource2.exits[ExitDirection.SOUTH] shouldBe destRoom2.id
        }

        test("ConnectionManager should handle connections with the same destination from different directions") {
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
                position = Position(100f, 100f)
            )

            // Create first connection (EAST)
            val (updatedSource1, updatedDest1) = connectionManager.createConnection(
                sourceRoom,
                ExitDirection.EAST,
                destRoom
            )

            // Create second connection (SOUTH)
            val (updatedSource2, updatedDest2) = connectionManager.createConnection(
                updatedSource1,
                ExitDirection.SOUTH,
                updatedDest1
            )

            // Verify both connections exist
            updatedSource2.exits[ExitDirection.EAST] shouldBe destRoom.id
            updatedSource2.exits[ExitDirection.SOUTH] shouldBe destRoom.id

            // Verify destination has connections back to source
            updatedDest2.exits[ExitDirection.WEST] shouldBe sourceRoom.id
            updatedDest2.exits[ExitDirection.NORTH] shouldBe sourceRoom.id
        }
    }

    context("Connection point finding") {
        test("ConnectionManager should find the closest connection point even with small differences") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)

            // Point very close to NORTH connection point
            val point1 = Offset(50.1f, 0.1f)
            val result1 = connectionManager.findClosestConnectionPoint(room, rect, point1, 5f)
            result1?.direction shouldBe ExitDirection.NORTH

            // Point very close to EAST connection point
            val point2 = Offset(99.9f, 30.1f)
            val result2 = connectionManager.findClosestConnectionPoint(room, rect, point2, 5f)
            result2?.direction shouldBe ExitDirection.EAST

            // Point very close to UP-LEFT connection point
            val point3 = Offset(0.1f, 0.1f)
            val result3 = connectionManager.findClosestConnectionPoint(room, rect, point3, 5f)
            result3?.direction shouldBe ExitDirection.UP
            result3?.corner shouldBe "LEFT"
        }

        test("ConnectionManager should handle finding connection points with different thresholds") {
            val room = Room(
                id = "test0",
                name = "Test Room",
                description = "Test room",
                position = Position(0f, 0f)
            )
            val rect = Rect(left = 0f, top = 0f, right = 100f, bottom = 60f)
            val point = Offset(50f, 10f) // 10 units away from NORTH connection point

            // With small threshold, should not find a connection point
            val result1 = connectionManager.findClosestConnectionPoint(room, rect, point, 5f)
            result1 shouldBe null

            // With larger threshold, should find NORTH connection point
            val result2 = connectionManager.findClosestConnectionPoint(room, rect, point, 15f)
            result2?.direction shouldBe ExitDirection.NORTH
        }
    }

    context("Line proximity detection edge cases") {
        test("isNearLine should handle points that project outside the line segment") {
            val start = Offset(0f, 0f)
            val end = Offset(100f, 0f) // Horizontal line

            // Point that projects before the start (t < 0)
            val pointBeforeStart = Offset(-10f, 5f)
            // Distance to start point is sqrt((-10-0)^2 + (5-0)^2) = sqrt(125) ≈ 11.18
            connectionManager.isNearLine(pointBeforeStart, start, end, 12f) shouldBe true
            connectionManager.isNearLine(pointBeforeStart, start, end, 11f) shouldBe false

            // Point that projects beyond the end (t > 1)
            val pointBeyondEnd = Offset(110f, 5f)
            // Distance to end point is sqrt((110-100)^2 + (5-0)^2) = sqrt(125) ≈ 11.18
            connectionManager.isNearLine(pointBeyondEnd, start, end, 12f) shouldBe true
            connectionManager.isNearLine(pointBeyondEnd, start, end, 11f) shouldBe false
        }

        test("isNearLine should handle points that project inside the line segment") {
            val start = Offset(0f, 0f)
            val end = Offset(100f, 0f) // Horizontal line

            // Point that projects at the start (t = 0)
            val pointAtStart = Offset(0f, 5f)
            connectionManager.isNearLine(pointAtStart, start, end, 10f) shouldBe true
            connectionManager.isNearLine(pointAtStart, start, end, 4f) shouldBe false

            // Point that projects at the end (t = 1)
            val pointAtEnd = Offset(100f, 5f)
            connectionManager.isNearLine(pointAtEnd, start, end, 10f) shouldBe true
            connectionManager.isNearLine(pointAtEnd, start, end, 4f) shouldBe false

            // Point that projects in the middle (0 < t < 1)
            val pointInMiddle = Offset(50f, 5f)
            connectionManager.isNearLine(pointInMiddle, start, end, 10f) shouldBe true
            connectionManager.isNearLine(pointInMiddle, start, end, 4f) shouldBe false
        }

        test("isNearLine should handle threshold edge cases") {
            val start = Offset(0f, 0f)
            val end = Offset(100f, 0f) // Horizontal line
            val threshold = 5f

            // Point exactly at the threshold distance
            val pointAtThreshold = Offset(50f, 5f)
            connectionManager.isNearLine(pointAtThreshold, start, end, threshold) shouldBe true

            // Point just inside the threshold
            val pointJustInside = Offset(50f, 4.9f)
            connectionManager.isNearLine(pointJustInside, start, end, threshold) shouldBe true

            // Point just outside the threshold
            val pointJustOutside = Offset(50f, 5.1f)
            connectionManager.isNearLine(pointJustOutside, start, end, threshold) shouldBe false
        }

        test("isNearLine should handle diagonal lines") {
            val start = Offset(0f, 0f)
            val end = Offset(100f, 100f) // Diagonal line

            // Point that projects before the start (t < 0)
            val pointBeforeStart = Offset(-10f, -10f)
            // Distance to start point is sqrt((-10-0)^2 + (-10-0)^2) = sqrt(200) ≈ 14.14
            connectionManager.isNearLine(pointBeforeStart, start, end, 15f) shouldBe true
            connectionManager.isNearLine(pointBeforeStart, start, end, 14f) shouldBe false

            // Point that projects beyond the end (t > 1)
            val pointBeyondEnd = Offset(110f, 110f)
            // Distance to end point is sqrt((110-100)^2 + (110-100)^2) = sqrt(200) ≈ 14.14
            connectionManager.isNearLine(pointBeyondEnd, start, end, 15f) shouldBe true
            connectionManager.isNearLine(pointBeyondEnd, start, end, 14f) shouldBe false

            // Point that projects in the middle (0 < t < 1)
            // For a diagonal line, a point 5 units perpendicular would be at (45, 55) or (55, 45)
            val pointInMiddle = Offset(45f, 55f)
            connectionManager.isNearLine(pointInMiddle, start, end, 10f) shouldBe true
            connectionManager.isNearLine(pointInMiddle, start, end, 4f) shouldBe false
        }

        test("isNearLine should handle vertical lines") {
            val start = Offset(0f, 0f)
            val end = Offset(0f, 100f) // Vertical line

            // Point that projects before the start (t < 0)
            val pointBeforeStart = Offset(5f, -10f)
            // Distance to start point is sqrt((5-0)^2 + (-10-0)^2) = sqrt(125) ≈ 11.18
            connectionManager.isNearLine(pointBeforeStart, start, end, 12f) shouldBe true
            connectionManager.isNearLine(pointBeforeStart, start, end, 11f) shouldBe false

            // Point that projects beyond the end (t > 1)
            val pointBeyondEnd = Offset(5f, 110f)
            // Distance to end point is sqrt((5-0)^2 + (110-100)^2) = sqrt(125) ≈ 11.18
            connectionManager.isNearLine(pointBeyondEnd, start, end, 12f) shouldBe true
            connectionManager.isNearLine(pointBeyondEnd, start, end, 11f) shouldBe false

            // Point that projects in the middle (0 < t < 1)
            val pointInMiddle = Offset(5f, 50f)
            connectionManager.isNearLine(pointInMiddle, start, end, 10f) shouldBe true
            connectionManager.isNearLine(pointInMiddle, start, end, 4f) shouldBe false
        }
    }
})
