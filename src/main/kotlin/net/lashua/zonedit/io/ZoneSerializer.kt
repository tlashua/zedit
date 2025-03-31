package net.lashua.zonedit.io

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import net.lashua.zonedit.model.*
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ZoneFile(
    val name: String,
    val id: String,
    @SerialName("node_width.f")
    val nodeWidth: Float = 100f,
    @SerialName("node_height.f")
    val nodeHeight: Float = 60f,
    val rooms: List<SerializableRoom> = emptyList()
)

@Serializable
data class SerializableRoom(
    val id: String,
    val name: String,
    val description: String,
    val position: SerializablePosition,
    val exits: Map<String, String> = emptyMap(),
    val flags: List<String> = emptyList()
)

@Serializable
data class SerializablePosition(
    val x: Float,
    val y: Float
)

object ZoneSerializer {
    private val toml = Toml(
        inputConfig = TomlInputConfig(
            ignoreUnknownNames = true,
            allowEmptyValues = true,
            allowNullValues = true
        ),
        outputConfig = TomlOutputConfig(
            indentation = TomlIndentation.TWO_SPACES
        )
    )

    fun saveZone(zone: Zone, file: File) {
        val zoneFile = ZoneFile(
            name = zone.name,
            id = zone.id,
            nodeWidth = zone.nodeWidth,
            nodeHeight = zone.nodeHeight,
            rooms = zone.rooms.map { room ->
                SerializableRoom(
                    id = room.id,
                    name = room.name,
                    description = room.description,
                    position = SerializablePosition(room.position.x, room.position.y),
                    exits = room.exits.entries.associate { (direction, destId) -> 
                        direction.name.lowercase() to destId 
                    },
                    flags = room.flags
                )
            }
        )
        
        file.writeText(toml.encodeToString(ZoneFile.serializer(), zoneFile))
    }

    fun loadZone(file: File): Zone {
        val content = file.readText()
        val zoneFile = toml.decodeFromString(ZoneFile.serializer(), content)
        
        return Zone(
            id = zoneFile.id,
            name = zoneFile.name,
            nodeWidth = zoneFile.nodeWidth,
            nodeHeight = zoneFile.nodeHeight,
            rooms = zoneFile.rooms.map { room ->
                Room(
                    id = room.id,
                    name = room.name,
                    description = room.description,
                    position = Position(room.position.x, room.position.y),
                    exits = room.exits.entries.associate { (direction, destId) -> 
                        ExitDirection.valueOf(direction.uppercase()) to destId 
                    },
                    flags = room.flags
                )
            }
        )
    }
}
