package net.lashua.zonedit.ui

import androidx.compose.ui.geometry.Offset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import net.lashua.zonedit.model.ExitDirection
import net.lashua.zonedit.model.Position
import net.lashua.zonedit.model.Room
import net.lashua.zonedit.model.Zone
import net.lashua.zonedit.ui.state.ZoneCanvasState

class ZoneCanvasStateEdgeCaseTest : FunSpec({
    
    context("Edge cases") {
        test("syncWithExternalState should not call callbacks when state doesn't change") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Mock callbacks
            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
            val onRoomSelected = mockk<(Room?) -> Unit>(relaxed = true)
            
            // Create the state
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = room,
                onZoneChanged = onZoneChanged,
                onRoomSelected = onRoomSelected,
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Sync with the same state
            state.syncWithExternalState(zone, room)
            
            // Verify callbacks were not called
            verify(exactly = 0) { onZoneChanged(any()) }
            verify(exactly = 0) { onRoomSelected(any()) }
        }
        
        test("updateSelectedRoom should not call onRoomSelected when the selected room is the same") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Mock callbacks
            val onRoomSelected = mockk<(Room?) -> Unit>(relaxed = true)
            
            // Create the state with an initially selected room
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = room,
                onZoneChanged = {},
                onRoomSelected = onRoomSelected,
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Update with the same room
            state.updateSelectedRoom(room)
            
            // Verify onRoomSelected was not called
            verify(exactly = 0) { onRoomSelected(any()) }
        }
        
        test("finalizeConnectionDrag should do nothing when no drag is in progress") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Mock callbacks
            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
            
            // Create the state
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = onZoneChanged,
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Finalize without starting a drag
            state.finalizeConnectionDrag(Offset(150f, 15f), 1.0f, 1.0f, 1000f, 800f)
            
            // Verify onZoneChanged was not called
            verify(exactly = 0) { onZoneChanged(any()) }
        }
        
        test("updateConnectionDragPoint should do nothing when no drag is in progress") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Create the state
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = {},
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Update without starting a drag
            state.updateConnectionDragPoint(Offset(150f, 15f))
            
            // No exception should be thrown
            state.connectionDragState shouldBe null
        }
        
        test("stopDragging should do nothing when no drag is in progress") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Mock callbacks
            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
            
            // Create the state
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = onZoneChanged,
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Stop without starting a drag
            state.stopDragging()
            
            // Verify onZoneChanged was not called
            verify(exactly = 0) { onZoneChanged(any()) }
        }
        
        test("updateZone should not call onZoneChanged when the zone is the same") {
            // Create test data
            val room = Room(
                id = "test0", 
                name = "Test Room", 
                description = "Test room", 
                position = Position(0f, 0f)
            )
            
            val zone = Zone(
                id = "test", 
                name = "Test Zone", 
                rooms = listOf(room)
            )
            
            // Mock callbacks
            val onZoneChanged = mockk<(Zone) -> Unit>(relaxed = true)
            
            // Create the state
            val state = ZoneCanvasState(
                initialZone = zone,
                initialSelectedRoom = null,
                onZoneChanged = onZoneChanged,
                onRoomSelected = {},
                onConnectionStarted = { _, _ -> },
                onPointerPositionChanged = {}
            )
            
            // Update with the same zone
            state.updateZone(zone)
            
            // Verify onZoneChanged was not called
            verify(exactly = 0) { onZoneChanged(any()) }
        }
    }
})
