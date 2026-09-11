package mindustryX.features

import arc.Core
import arc.Files
import arc.files.Fi
import arc.struct.IntIntMap
import arc.struct.ObjectIntMap
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.net.Net
import mindustry.net.Packet

/** Data container for assets/packets.jsonl; the mapping for the current [LogicExt.mockProtocol] is cached. */
object ProtocolMap {
    class Entry(val name: String, var since: Int, var until: Int) {
        fun active(version: Int): Boolean = version in since..<until
    }

    fun parse(lines: List<String>): MutableList<Entry> = lines.map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
        val json = Jval.read(line)
        Entry(json.getString("name", ""), json.getInt("since", Int.MIN_VALUE), json.getInt("until", Int.MAX_VALUE))
    }.toMutableList()

    fun serialize(entries: List<Entry>): String = buildString {
        for (e in entries) {
            append("{\"name\":\"").append(e.name).append('"')
            if (e.since != Int.MIN_VALUE) append(",\"since\":").append(e.since)
            if (e.until != Int.MAX_VALUE) append(",\"until\":").append(e.until)
            append("}\n")
        }
    }

    private val entries: List<Entry> by lazy {
        try {
            val fi = if (Core.files != null) Core.files.internal("packets.jsonl") else Fi("packets.jsonl", Files.FileType.internal)
            parse(fi.readString().lines())
        } catch (t: Throwable) {
            Log.err("Failed to load packets.jsonl; only the current protocol version is available", t)
            Net.allPacketClasses().map { Entry(it.getSimpleName(), Int.MIN_VALUE, Int.MAX_VALUE) }.toList()
        }
    }

    class Version(
        val version: Int,
        val mapping: List<String>,
        val packetToId: ObjectIntMap<Class<*>>,
        val idMapping: IntIntMap,
    )

    private var cached: Version? = null

    @JvmStatic
    fun getId(packet: Packet): Int {
        //current (or newer) version: no remap, skip building a Version entirely
        if (LogicExt.mockProtocol >= mindustry.core.Version.build) return Net.getPacketClassId(packet.javaClass).toInt()
        return version().packetToId.get(packet.javaClass, -1)
    }

    @JvmStatic
    fun mapId(oldId: Int): Int {
        if (LogicExt.mockProtocol >= mindustry.core.Version.build) return oldId
        return version().idMapping.get(oldId, -1)
    }

    fun version(): Version {
        val version = LogicExt.mockProtocol
        val current = cached
        if (current != null && current.version == version) return current

        Log.info("Initializing v$version packets mapping...")
        val mapping = entries.filter { it.active(version) }.map { it.name }
        val nameToId = ObjectIntMap<String>()
        for (name in mapping) {
            nameToId.put(name, nameToId.size)
        }
        val idMapping = IntIntMap()
        val packetToId = ObjectIntMap<Class<*>>()
        Net.allPacketClasses().forEachIndexed { curId, packetClass ->
            val oldId = nameToId.get(packetClass.getSimpleName(), -1)
            if (oldId != -1) {
                idMapping.put(oldId, curId)
                packetToId.put(packetClass, oldId)
            } else {
                Log.warn("New packet type: " + packetClass.getSimpleName())
            }
        }
        for (i in mapping.indices) {
            if (!idMapping.containsKey(i)) Log.warn("Deleted packet: " + mapping[i])
        }
        Log.info("== End load v$version packets mapping ==")
        return Version(version, mapping, packetToId, idMapping).also { cached = it }
    }
}
