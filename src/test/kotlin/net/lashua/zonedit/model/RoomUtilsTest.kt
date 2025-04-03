package net.lashua.zonedit.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RoomUtilsTest : FunSpec({
    
    context("Room ID generation") {
        test("generateRoomId should create a room ID with the zone name and room number") {
            val zone = Zone(id = "test", name = "Test Zone")
            val roomNumber = 42
            
            val roomId = RoomUtils.generateRoomId(zone, roomNumber)
            
            roomId shouldBe "test zone42"
        }
        
        test("generateRoomId should handle zone names with spaces") {
            val zone = Zone(id = "test", name = "My Test Zone")
            val roomNumber = 7
            
            val roomId = RoomUtils.generateRoomId(zone, roomNumber)
            
            roomId shouldBe "my test zone7"
        }
        
        test("generateRoomId should handle zone names with special characters") {
            val zone = Zone(id = "test", name = "Test-Zone!")
            val roomNumber = 0
            
            val roomId = RoomUtils.generateRoomId(zone, roomNumber)
            
            roomId shouldBe "test-zone!0"
        }
    }
    
    context("Room renumbering") {
        test("renumberRooms should renumber all rooms sequentially from 0") {
            // Create a zone with rooms that have non-sequential IDs
            val room1 = Room(
                id = "test5", 
                name = "Room 1", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            val room2 = Room(
                id = "test10", 
                name = "Room 2", 
                description = "Test room", 
                position = Position(100f, 0f)
            )
            val room3 = Room(
                id = "test20", 
                name = "Room 3", 
                description = "Test room", 
                position = Position(200f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "test", 
                rooms = listOf(room1, room2, room3)
            )
            
            val renumberedZone = RoomUtils.renumberRooms(zone)
            
            // Check that rooms were renumbered sequentially
            renumberedZone.rooms[0].id shouldBe "test0"
            renumberedZone.rooms[1].id shouldBe "test1"
            renumberedZone.rooms[2].id shouldBe "test2"
        }
        
        test("renumberRooms should update exit references") {
            // Create a zone with connected rooms
            val room1 = Room(
                id = "test5", 
                name = "Room 1", 
                description = "Test room", 
                position = Position(0f, 0f),
                exits = mapOf(ExitDirection.EAST to "test10")
            )
            val room2 = Room(
                id = "test10", 
                name = "Room 2", 
                description = "Test room", 
                position = Position(100f, 0f),
                exits = mapOf(ExitDirection.WEST to "test5", ExitDirection.EAST to "test20")
            )
            val room3 = Room(
                id = "test20", 
                name = "Room 3", 
                description = "Test room", 
                position = Position(200f, 0f),
                exits = mapOf(ExitDirection.WEST to "test10")
            )
            
            val zone = Zone(
                id = "test", 
                name = "test", 
                rooms = listOf(room1, room2, room3)
            )
            
            val renumberedZone = RoomUtils.renumberRooms(zone)
            
            // Check that exit references were updated
            renumberedZone.rooms[0].exits[ExitDirection.EAST] shouldBe "test1" // Updated from test10
            renumberedZone.rooms[1].exits[ExitDirection.WEST] shouldBe "test0" // Updated from test5
            renumberedZone.rooms[1].exits[ExitDirection.EAST] shouldBe "test2" // Updated from test20
            renumberedZone.rooms[2].exits[ExitDirection.WEST] shouldBe "test1" // Updated from test10
        }
        
        test("renumberRooms should preserve room properties other than ID") {
            val originalRoom = Room(
                id = "test5", 
                name = "Original Room", 
                description = "Test description", 
                position = Position(42f, 24f),
                exits = mapOf(ExitDirection.NORTH to "test10"),
                exitCorners = mapOf(ExitDirection.UP to "LEFT"),
                flags = listOf("flag1", "flag2"),
                metadata = mapOf("key1" to "value1", "key2" to "value2")
            )
            
            val zone = Zone(
                id = "test", 
                name = "test", 
                rooms = listOf(originalRoom)
            )
            
            val renumberedZone = RoomUtils.renumberRooms(zone)
            val renumberedRoom = renumberedZone.rooms[0]
            
            // ID should be updated
            renumberedRoom.id shouldBe "test0"
            
            // All other properties should be preserved
            renumberedRoom.name shouldBe originalRoom.name
            renumberedRoom.description shouldBe originalRoom.description
            renumberedRoom.position shouldBe originalRoom.position
            renumberedRoom.exitCorners shouldBe originalRoom.exitCorners
            renumberedRoom.flags shouldBe originalRoom.flags
            renumberedRoom.metadata shouldBe originalRoom.metadata
            
            // Exits should be updated if they point to renumbered rooms
            // In this case, we don't have the target room in our test zone
            renumberedRoom.exits[ExitDirection.NORTH] shouldBe "test10" // Not changed because target not in zone
        }
    }
})
