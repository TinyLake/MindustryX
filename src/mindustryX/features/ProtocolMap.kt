package mindustryX.features

import arc.Core
import arc.Files
import arc.files.Fi
import arc.struct.IntIntMap
import arc.struct.ObjectIntMap
import arc.util.Log
import arc.util.io.Reads
import mindustry.gen.Player
import mindustry.gen.Syncc
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

    class PlayerFixReads(val raw: Reads) : Reads(raw.input) {
        /*
        Skip: "selectedBlock" and "selectedRotation" after "name" field in Player class
        * */
        var state = 0
        override fun str(): String? {
            state = 1
            return super.str()
        }

        override fun s(): Short {
            if (state == 1) {
                state = 2
                return 0
            }
            return super.s()
        }

        override fun i(): Int {
            if (state == 2) {
                state = 3
                return 0
            }
            return super.i()
        }
    }

    companion object {
        fun entityReadSnapReplace(entity: Syncc, read: Reads): Reads {
            if (LogicExt.mockProtocol < 155 && entity is Player) {
                return PlayerFixReads(read)
            }
            return read
        }
    }
}