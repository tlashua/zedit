package net.lashua.zonedit.model

object RoomUtils {
    fun generateRoomId(zone: Zone, roomNumber: Int): String {
        return "${zone.name.lowercase()}$roomNumber"
    }

    /**
     * Renumbers all rooms in a zone sequentially from 0
     * Maintains all connections by updating the exit references
     */
    fun renumberRooms(zone: Zone): Zone {
        val oldToNewIds = zone.rooms.mapIndexed { index, room ->
            room.id to generateRoomId(zone, index)
        }.toMap()

        val updatedRooms = zone.rooms.mapIndexed { index, room ->
            room.copy(
                id = oldToNewIds[room.id]!!,
                exits = room.exits.map { exit ->
                    exit.copy(destinationId = oldToNewIds[exit.destinationId] ?: exit.destinationId)
                }
            )
        }

        return zone.copy(rooms = updatedRooms)
    }
}