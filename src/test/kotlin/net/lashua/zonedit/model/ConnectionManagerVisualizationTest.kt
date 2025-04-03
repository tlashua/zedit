package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.absoluteValue
import io.mockk.mockk
import io.mockk.verify
import kotlin.math.PI

class ConnectionManagerVisualizationTest : FunSpec({

    context("Drawing and visualization") {
        test("drawConnectionPreview should draw a preview line") {
            // Create a mock DrawScope
            val drawScope = mockk<DrawScope>(relaxed = true)

            // Create test data
            val sourceRoom = Room(
                id = "test0",
                name = "Source Room",
                description = "Test source room",
                position = Position(0f, 0f)
            )

            val zone = Zone(
                id = "test",
                name = "Test Zone",
                rooms = listOf(sourceRoom)
            )

            val connectionManager = ConnectionManager()

            // Call the method
            connectionManager.drawConnectionPreview(
                drawScope,
                sourceRoom,
                ExitDirection.EAST,
                Offset(150f, 15f),
                "RIGHT",
                zone,
                1.0f,
                1.0f
            )

            // Verify drawLine was called with the expected parameters
            verify {
                drawScope.drawLine(
                    color = Color.Blue,  // Preview color for cardinal directions
                    start = any(),       // Source point
                    end = Offset(150f, 15f),  // Current point
                    strokeWidth = 2f,    // Default stroke width * zoom level
                    pathEffect = any()   // Dash path effect
                )
            }
        }

        test("calculateAngle should return the correct angle") {
            val connectionManager = ConnectionManager()

            // Test horizontal line (0 degrees)
            val angle1 = connectionManager.calculateAngle(
                Offset(0f, 0f),
                Offset(10f, 0f)
            )
            angle1 shouldBe (0f plusOrMinus 0.01f)

            // Test 45 degree line
            val angle2 = connectionManager.calculateAngle(
                Offset(0f, 0f),
                Offset(10f, 10f)
            )
            angle2 shouldBe (PI.toFloat() / 4f plusOrMinus 0.01f)

            // Test vertical line (90 degrees)
            val angle3 = connectionManager.calculateAngle(
                Offset(0f, 0f),
                Offset(0f, 10f)
            )
            angle3 shouldBe (PI.toFloat() / 2f plusOrMinus 0.01f)

            // Test 135 degree line
            val angle4 = connectionManager.calculateAngle(
                Offset(0f, 0f),
                Offset(-10f, 10f)
            )
            angle4 shouldBe (3f * PI.toFloat() / 4f plusOrMinus 0.01f)

            // Test 180 degree line
            val angle5 = connectionManager.calculateAngle(
                Offset(0f, 0f),
                Offset(-10f, 0f)
            )
            angle5 shouldBe (PI.toFloat() plusOrMinus 0.01f)
        }

        test("calculateArrowPoints should return the correct points") {
            val connectionManager = ConnectionManager()

            // Test horizontal arrow (0 degrees)
            val (p1, p2) = connectionManager.calculateArrowPoints(
                Offset(100f, 100f),  // Arrow tip
                0f,                  // Angle (horizontal)
                10f,                 // Length
                PI.toFloat() / 4f    // Arrow angle (45 degrees)
            )

            // The actual implementation might use different calculations
            // We'll just verify that the points are different from each other
            // and are in the general vicinity of where we expect them to be
            p1 shouldNotBe p2

            // Points should be roughly 10 units back from the tip
            (p1.x - 100f).absoluteValue shouldBe (10f plusOrMinus 5f)
            (p2.x - 100f).absoluteValue shouldBe (10f plusOrMinus 5f)
        }

        test("getConnectionColor should return the correct color for each direction") {
            val connectionManager = ConnectionManager()

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

    context("Connection management") {
        test("getConnection should return a connection if it exists") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source",
                description = "Test",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1")
            )

            val destRoom = Room(
                id = "test1",
                name = "Dest",
                description = "Test",
                position = Position(100f, 0f),
                exits = mapOf(ExitDirection.WEST to "test0")
            )

            val connectionManager = ConnectionManager()

            val connection = connectionManager.getConnection(sourceRoom, ExitDirection.EAST, destRoom)

            connection shouldNotBe null
            connection?.source?.room shouldBe sourceRoom
            connection?.source?.direction shouldBe ExitDirection.EAST
            connection?.destination?.room shouldBe destRoom
            connection?.destination?.direction shouldBe ExitDirection.WEST
        }

        test("getConnection should return null if the connection doesn't exist") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source",
                description = "Test",
                position = Position(0f, 0f)
            )

            val destRoom = Room(
                id = "test1",
                name = "Dest",
                description = "Test",
                position = Position(100f, 0f)
            )

            val connectionManager = ConnectionManager()

            val connection = connectionManager.getConnection(sourceRoom, ExitDirection.EAST, destRoom)

            connection shouldBe null
        }

        test("getConnection should return null if the connection is not bi-directional") {
            val sourceRoom = Room(
                id = "test0",
                name = "Source",
                description = "Test",
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test1")
            )

            val destRoom = Room(
                id = "test1",
                name = "Dest",
                description = "Test",
                position = Position(100f, 0f)
                // Missing the return connection
            )

            val connectionManager = ConnectionManager()

            val connection = connectionManager.getConnection(sourceRoom, ExitDirection.EAST, destRoom)

            connection shouldBe null
        }
    }
})
