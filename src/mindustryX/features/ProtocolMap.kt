package mindustryX.features

import arc.Core
import arc.Files
import arc.files.Fi
import arc.struct.IntIntMap
import arc.struct.ObjectIntMap
import arc.util.Log
import mindustry.net.Net
import mindustry.net.Packet

class ProtocolMap(val version: String) {
    var loaded = false
    var mapping = emptyList<String>()
    val packetToId = ObjectIntMap<String>()
    val idMapping = IntIntMap() // oldId -> newId

    fun load() {
        if (loaded) return
        loaded = true
        Log.info("Initializing $version packets mapping...")
        val fi = if (Core.files != null) Core.files.internal("packets_$version.txt") else Fi("packets_$version.txt", Files.FileType.internal)
        mapping = fi.readString().lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (name in mapping) {
            packetToId.put(name, packetToId.size)
        }
        Net.allPacketClasses().forEachIndexed { curId, packetClass ->
            val oldId = packetToId.get(packetClass.getSimpleName(), -1)
            if (oldId != -1) {
                idMapping.put(oldId, curId)
            } else {
                Log.warn("New packet type: " + packetClass.getSimpleName())
            }
        }
        for (i in mapping.indices) {
            if (!idMapping.containsKey(i)) Log.warn("Deleted packet: " + mapping[i])
        }
        Log.info("== End load $version packets mapping ==")
    }

    fun getId(packet: Packet): Int {
        load()
        return packetToId.get(packet.javaClass.getSimpleName(), -1)
    }

    fun mapId(oldId: Int): Int {
        load()
        return idMapping.get(oldId, -1)
    }
}