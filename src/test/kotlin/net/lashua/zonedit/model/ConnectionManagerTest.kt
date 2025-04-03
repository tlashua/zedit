package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf

class ConnectionManagerTest : FunSpec({
    
    val connectionManager = ConnectionManager()
    
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
    }
})
