package net.lashua.zonedit.model

import net.lashua.zonedit.graph.model.Edge
import net.lashua.zonedit.graph.model.Graph
import net.lashua.zonedit.graph.model.Node

/**
 * Represents a MUD zone.
 * Extends the generic Graph class with MUD-specific functionality.
 *
 * @property name The name of the zone
 * @property nodeWidthDp The width of nodes in dp
 * @property nodeHeightDp The height of nodes in dp
 */
class MudZone(
    val name: String,
    val nodeWidthDp: Float = 100f,
    val nodeHeightDp: Float = 60f,
    rooms: List<MudRoom> = emptyList(),
    exits: List<MudExit> = emptyList()
) : Graph<RoomData, ExitData>(
    nodes = rooms.map { it as Node<RoomData> },
    edges = exits.map { it as Edge<ExitData> }
) {

    /**
     * Gets all rooms in the zone.
     */
    val rooms: List<MudRoom>
        get() = nodes.map {
            when (it) {
                is MudRoom -> it
                else -> MudRoom(it.id, it.data)
            }
        }

    /**
     * Gets all exits in the zone.
     */
    val exits: List<MudExit>
        get() = edges.map {
            when (it) {
                is MudExit -> it
                else -> MudExit(it.sourceId, it.targetId, it.data)
            }
        }

    /**
     * Gets a room by its ID.
     *
     * @param id The ID of the room to get
     * @return The room with the specified ID, or null if not found
     */
    fun getRoom(id: String): MudRoom? {
        return getNode(id) as? MudRoom
    }

    /**
     * Gets all exits originating from a room.
     *
     * @param roomId The ID of the room
     * @return A list of exits originating from the room
     */
    fun getRoomExits(roomId: String): List<MudExit> {
        return getOutgoingEdges(roomId).map { it as MudExit }
    }

    /**
     * Gets all exits targeting a room.
     *
     * @param roomId The ID of the room
     * @return A list of exits targeting the room
     */
    fun getRoomEntrances(roomId: String): List<MudExit> {
        return getIncomingEdges(roomId).map { it as MudExit }
    }

    /**
     * Gets all exits in a specific direction from a room.
     *
     * @param roomId The ID of the room
     * @param direction The direction to filter by
     * @return A list of exits in the specified direction
     */
    fun getRoomExitsInDirection(roomId: String, direction: ExitDirection): List<MudExit> {
        return getRoomExits(roomId).filter { it.direction == direction }
    }

    /**
     * Gets all exits in a specific direction targeting a room.
     *
     * @param roomId The ID of the room
     * @param direction The direction to filter by
     * @return A list of exits in the specified direction targeting the room
     */
    fun getRoomEntrancesInDirection(roomId: String, direction: ExitDirection): List<MudExit> {
        return getRoomEntrances(roomId).filter { it.direction == direction }
    }

    /**
     * Adds a room to the zone.
     *
     * @param room The room to add
     * @return A new MudZone with the room added
     */
    fun addRoom(room: MudRoom): MudZone {
        val newGraph = addNode(room)
        return MudZone(
            name = name,
            nodeWidthDp = nodeWidthDp,
            nodeHeightDp = nodeHeightDp,
            rooms = newGraph.nodes.map {
                when (it) {
                    is MudRoom -> it
                    else -> MudRoom(it.id, it.data)
                }
            },
            exits = newGraph.edges.map {
                when (it) {
                    is MudExit -> it
                    else -> MudExit(it.sourceId, it.targetId, it.data)
                }
            }
        )
    }

    /**
     * Removes a room from the zone.
     * Also removes all exits connected to the room.
     *
     * @param roomId The ID of the room to remove
     * @return A new MudZone with the room and its exits removed
     */
    fun removeRoom(roomId: String): MudZone {
        val newGraph = removeNode(roomId)
        return MudZone(
            name = name,
            nodeWidthDp = nodeWidthDp,
            nodeHeightDp = nodeHeightDp,
            rooms = newGraph.nodes.map {
                when (it) {
                    is MudRoom -> it
                    else -> MudRoom(it.id, it.data)
                }
            },
            exits = newGraph.edges.map {
                when (it) {
                    is MudExit -> it
                    else -> MudExit(it.sourceId, it.targetId, it.data)
                }
            }
        )
    }

    /**
     * Updates a room in the zone.
     *
     * @param room The updated room
     * @return A new MudZone with the room updated
     */
    fun updateRoom(room: MudRoom): MudZone {
        val newGraph = updateNode(room)
        return MudZone(
            name = name,
            nodeWidthDp = nodeWidthDp,
            nodeHeightDp = nodeHeightDp,
            rooms = newGraph.nodes.map {
                when (it) {
                    is MudRoom -> it
                    else -> MudRoom(it.id, it.data)
                }
            },
            exits = newGraph.edges.map {
                when (it) {
                    is MudExit -> it
                    else -> MudExit(it.sourceId, it.targetId, it.data)
                }
            }
        )
    }

    /**
     * Updates a room's position in the zone.
     *
     * @param roomId The ID of the room to update
     * @param position The new position of the room
     * @return A new MudZone with the room's position updated
     */
    fun updateRoomPosition(roomId: String, position: Position): MudZone {
        val room = getRoom(roomId) ?: return this
        val updatedRoom = room.updatePosition(position)
        return updateRoom(updatedRoom)
    }

    /**
     * Adds an exit to the zone.
     *
     * @param exit The exit to add
     * @return A new MudZone with the exit added
     */
    fun addExit(exit: MudExit): MudZone {
        val newGraph = addEdge(exit)
        return MudZone(
            name = name,
            nodeWidthDp = nodeWidthDp,
            nodeHeightDp = nodeHeightDp,
            rooms = newGraph.nodes.map {
                when (it) {
                    is MudRoom -> it
                    else -> MudRoom(it.id, it.data)
                }
            },
            exits = newGraph.edges.map {
                when (it) {
                    is MudExit -> it
                    else -> MudExit(it.sourceId, it.targetId, it.data)
                }
            }
        )
    }

    /**
     * Removes an exit from the zone.
     *
     * @param sourceId The ID of the source room
     * @param targetId The ID of the target room
     * @param direction The direction of the exit
     * @return A new MudZone with the exit removed
     */
    fun removeExit(sourceId: String, targetId: String, direction: ExitDirection): MudZone {
        // Find the specific exit to remove
        val exitToRemove = exits.find {
            it.sourceId == sourceId && it.targetId == targetId && it.direction == direction
        } ?: return this

        // Remove the exit
        val updatedExits = exits.filter { it != exitToRemove }
        return MudZone(name, nodeWidthDp, nodeHeightDp, rooms, updatedExits)
    }

    /**
     * Creates a bi-directional connection between two rooms.
     *
     * @param sourceRoom The source room
     * @param targetRoom The target room
     * @param direction The direction from source to target
     * @param sourceCorner The corner for UP/DOWN exits from source to target
     * @return A new MudZone with the connection added
     */
    fun connectRooms(
        sourceRoom: MudRoom,
        targetRoom: MudRoom,
        direction: ExitDirection,
        sourceCorner: String? = null
    ): MudZone {
        val oppositeDir = getOppositeDirection(direction)

        // Create the exits
        val sourceExit = MudExit(
            sourceId = sourceRoom.id,
            targetId = targetRoom.id,
            data = ExitData(
                direction = direction,
                corner = if (direction in listOf(ExitDirection.UP, ExitDirection.DOWN)) sourceCorner else null
            )
        )

        val targetCorner = if (sourceCorner == "LEFT") "RIGHT" else "LEFT"
        val targetExit = MudExit(
            sourceId = targetRoom.id,
            targetId = sourceRoom.id,
            data = ExitData(
                direction = oppositeDir,
                corner = if (oppositeDir in listOf(ExitDirection.UP, ExitDirection.DOWN)) targetCorner else null
            )
        )

        // Add the exits to the zone
        var updatedZone = this
        updatedZone = updatedZone.addExit(sourceExit)
        updatedZone = updatedZone.addExit(targetExit)

        return updatedZone
    }

    /**
     * Creates a new room with a connection to an existing room.
     *
     * @param sourceRoom The existing room
     * @param direction The direction from source to the new room
     * @param position The position of the new room
     * @param sourceCorner The corner for UP/DOWN exits from source to the new room
     * @return A new MudZone with the new room and connection added
     */
    fun createRoomWithConnection(
        sourceRoom: MudRoom,
        direction: ExitDirection,
        position: Position,
        sourceCorner: String? = null
    ): MudZone {
        // Create a new room
        val newRoomId = "scratchpad${rooms.size}"
        val newRoom = MudRoom(
            id = newRoomId,
            data = RoomData(
                name = "New Room",
                description = "A new room",
                position = position
            )
        )

        // Add the new room to the zone
        var updatedZone = addRoom(newRoom)

        // Connect the rooms
        updatedZone = updatedZone.connectRooms(sourceRoom, newRoom, direction, sourceCorner)

        return updatedZone
    }

    /**
     * Gets the opposite direction of a given direction.
     *
     * @param direction The direction to get the opposite of
     * @return The opposite direction
     */
    private fun getOppositeDirection(direction: ExitDirection): ExitDirection {
        return when (direction) {
            ExitDirection.NORTH -> ExitDirection.SOUTH
            ExitDirection.SOUTH -> ExitDirection.NORTH
            ExitDirection.EAST -> ExitDirection.WEST
            ExitDirection.WEST -> ExitDirection.EAST
            ExitDirection.UP -> ExitDirection.DOWN
            ExitDirection.DOWN -> ExitDirection.UP
        }
    }
}
