package net.lashua.zonedit.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("net.lashua.zonedit.model.ConnectionManager")

data class ConnectionPoint(
    val position: Offset,
    val room: Room,
    val direction: ExitDirection,
    val corner: String? = null  // Only used for UP/DOWN connections
)

data class Connection(
    val source: ConnectionPoint,
    val destination: ConnectionPoint
)

class ConnectionManager {
    fun getConnectionPoints(room: Room, rect: Rect): List<ConnectionPoint> {
        return buildList {
            // Cardinal directions (N,S,E,W)
            add(ConnectionPoint(
                Offset(rect.center.x, rect.top),
                room,
                ExitDirection.NORTH
            ))
            add(ConnectionPoint(
                Offset(rect.center.x, rect.bottom),
                room,
                ExitDirection.SOUTH
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.center.y),
                room,
                ExitDirection.EAST
            ))
            add(ConnectionPoint(
                Offset(rect.left, rect.center.y),
                room,
                ExitDirection.WEST
            ))

            // UP points (regardless of whether UP exit exists)
            add(ConnectionPoint(
                Offset(rect.left, rect.top),
                room,
                ExitDirection.UP,
                "LEFT"
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.top),
                room,
                ExitDirection.UP,
                "RIGHT"
            ))

            // DOWN points (regardless of whether DOWN exit exists)
            add(ConnectionPoint(
                Offset(rect.left, rect.bottom),
                room,
                ExitDirection.DOWN,
                "LEFT"
            ))
            add(ConnectionPoint(
                Offset(rect.right, rect.bottom),
                room,
                ExitDirection.DOWN,
                "RIGHT"
            ))
        }
    }

    fun getConnection(source: Room, direction: ExitDirection, destination: Room): Connection? {
        val destId = source.exits[direction] ?: return null
        if (destination.id != destId) return null

        val oppositeDir = getOppositeDirection(direction)
        
        // Verify bi-directional connection
        if (destination.exits[oppositeDir] != source.id) {
            log.warn("Invalid connection state: ${source.id} -> ${destination.id} is not bi-directional")
            return null
        }

        return Connection(
            source = ConnectionPoint(
                position = Offset.Zero, // Will be set by drawing code
                room = source,
                direction = direction,
                corner = source.exitCorners[direction]
            ),
            destination = ConnectionPoint(
                position = Offset.Zero, // Will be set by drawing code
                room = destination,
                direction = oppositeDir,
                corner = destination.exitCorners[oppositeDir]
            )
        )
    }

    fun getOppositeDirection(direction: ExitDirection): ExitDirection {
        return when (direction) {
            ExitDirection.NORTH -> ExitDirection.SOUTH
            ExitDirection.SOUTH -> ExitDirection.NORTH
            ExitDirection.EAST -> ExitDirection.WEST
            ExitDirection.WEST -> ExitDirection.EAST
            ExitDirection.UP -> ExitDirection.DOWN
            ExitDirection.DOWN -> ExitDirection.UP
        }
    }

    fun createConnection(source: Room, direction: ExitDirection, destination: Room, sourceCorner: String? = null): Pair<Room, Room> {
        val oppositeDir = getOppositeDirection(direction)
        
        val updatedSource = source.copy(
            exits = source.exits + (direction to destination.id),
            exitCorners = when (direction) {
                ExitDirection.UP, ExitDirection.DOWN -> source.exitCorners + (direction to (sourceCorner ?: "RIGHT"))
                else -> source.exitCorners
            }
        )
        
        val destCorner = if (sourceCorner == "LEFT") "RIGHT" else "LEFT"
        val updatedDest = destination.copy(
            exits = destination.exits + (oppositeDir to source.id),
            exitCorners = when (oppositeDir) {
                ExitDirection.UP, ExitDirection.DOWN -> destination.exitCorners + (oppositeDir to destCorner)
                else -> destination.exitCorners
            }
        )
        
        return Pair(updatedSource, updatedDest)
    }

    fun removeConnection(source: Room, direction: ExitDirection, zone: Zone): Pair<Room, Room>? {
        log.debug("removeConnection called - source: {}, direction: {}", source.id, direction)
        
        val destId = source.exits[direction]
        if (destId == null) {
            log.warn("No destination found for direction {} in room {}", direction, source.id)
            return null
        }
        
        val destination = zone.rooms.find { it.id == destId }
        if (destination == null) {
            log.warn("Destination room {} not found in zone", destId)
            return null
        }

        val oppositeDir = getOppositeDirection(direction)
        log.debug("Removing bi-directional connection {} <-[{}/{}]-> {}", 
            source.id, direction, oppositeDir, destination.id)
        
        val updatedSource = source.copy(
            exits = source.exits - direction,
            exitCorners = source.exitCorners - direction
        )
        
        val updatedDest = destination.copy(
            exits = destination.exits - oppositeDir,
            exitCorners = destination.exitCorners - oppositeDir
        )
        
        log.debug("Connection removal complete")
        return Pair(updatedSource, updatedDest)
    }
}
