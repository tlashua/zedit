package net.lashua.zonedit.io

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import net.lashua.zonedit.model.*
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class ZoneFile(
    val name: String,
    val id: String,
    val node_width: Float = 100f,
    val node_height: Float = 60f,
    val rooms: List<SerializableRoom> = emptyList(),
    val zone_attributes: Map<String, String> = emptyMap(),
    val scripting: Map<String, String> = emptyMap()
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
            ignoreUnknownNames = true, // For forward compatibility
            allowEmptyValues = true,
            allowNullValues = true
        ),
        outputConfig = TomlOutputConfig(
            indentation = "    "
        )
    )

    fun saveZone(zone: Zone, file: File) {
        val zoneFile = ZoneFile(
            name = zone.name,
            id = zone.id,
            node_width = zone.nodeWidth,
            node_height = zone.nodeHeight,
            rooms = zone.rooms.map { room ->
                SerializableRoom(
                    id = room.id,
                    name = room.name,
                    description = room.description,
                    position = SerializablePosition(room.position.x, room.position.y),
                    exits = room.exits.mapKeys { it.key.name.lowercase() }
                )
            }
        )
        
        file.writeText(toml.encodeToString(ZoneFile.serializer(), zoneFile))
    }

    fun loadZone(file: File): Zone {
        val zoneFile = toml.decodeFromString(ZoneFile.serializer(), file.readText())
        
        return Zone(
            id = zoneFile.id,
            name = zoneFile.name,
            nodeWidth = zoneFile.node_width,
            nodeHeight = zoneFile.node_height,
            rooms = zoneFile.rooms.map { room ->
                Room(
                    id = room.id,
                    name = room.name,
                    description = room.description,
                    position = Position(room.position.x, room.position.y),
                    exits = room.exits.mapKeys { 
                        ExitDirection.valueOf(it.key.uppercase())
                    }
                )
            }
        )
    }
}