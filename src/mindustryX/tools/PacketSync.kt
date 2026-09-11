package mindustryX.tools

import mindustry.net.Net
import mindustryX.features.ProtocolMap
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Dev tool: keep assets/packets.jsonl in sync with the current packet registration order.
 *
 * Usage: `PacketSync <packets.jsonl> <currentVersion>`
 */
object PacketSync {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            System.err.println("usage: PacketSync <packets.jsonl> <currentVersion>")
            exitProcess(2)
        }
        val file = Paths.get(args[0])
        val version = args[1].toInt()

        val current = Net.allPacketClasses().map { it.simpleName }.toList()
        val entries = ProtocolMap.parse(Files.readAllLines(file, StandardCharsets.UTF_8))
        val projection = entries.filter { it.active(version) }.map { it.name }

        val currentSet = current.toHashSet()
        val projectionSet = projection.toHashSet()

        val added = current.filter { it !in projectionSet }
        val removed = projection.filter { it !in currentSet }

        //the relative order of packets that exist in both must be identical (no swaps)
        val commonProjection = projection.filter { it in currentSet }
        val commonCurrent = current.filter { it in projectionSet }
        val ordered = commonProjection == commonCurrent

        println("packets.jsonl entries: ${entries.size}")
        println("version $version: projection=${projection.size} dump=${current.size}")
        added.forEach { println("  + $it") }
        removed.forEach { println("  - $it") }
        if (!ordered) println("  ! common packets are not consistently ordered (possible swap/rename)")

        if (added.isEmpty() && removed.isEmpty() && ordered) {
            println("OK: assets/packets.jsonl matches current build")
            return
        }

        merge(entries, current, version)
        val after = entries.filter { it.active(version) }.map { it.name }
        if (after != current) {
            System.err.println("ERROR: cannot derive a consistent packets.jsonl (possible packet reorder); update it manually.")
            exitProcess(1)
        }
        Files.write(file, ProtocolMap.serialize(entries).toByteArray(StandardCharsets.UTF_8))
        println("synced $file")
    }

    private fun merge(entries: MutableList<ProtocolMap.Entry>, current: List<String>, version: Int) {
        val byName = HashMap<String, ProtocolMap.Entry>()
        for (e in entries) byName[e.name] = e
        val currentSet = current.toHashSet()

        for (e in entries) {
            if (e.active(version) && e.name !in currentSet) e.until = version
        }
        for (i in current.indices) {
            val name = current[i]
            val e = byName[name]
            if (e == null) {
                var anchor: String? = null
                for (j in i + 1 until current.size) {
                    if (byName.containsKey(current[j])) {
                        anchor = current[j]
                        break
                    }
                }
                val ne = ProtocolMap.Entry(name, version, Int.MAX_VALUE)
                if (anchor == null) entries.add(ne) else entries.add(indexOf(entries, anchor), ne)
                byName[name] = ne
            } else if (!e.active(version)) {
                e.since = version
                e.until = Int.MAX_VALUE
            }
        }
    }

    private fun indexOf(entries: List<ProtocolMap.Entry>, name: String) = entries.indexOfFirst { it.name == name }
}
