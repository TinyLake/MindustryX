package mindustryX.features

import arc.files.Fi
import arc.util.Time
import arc.util.io.ByteBufferOutput
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.net.Net
import mindustry.net.Packet
import mindustry.net.Streamable
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

data class ReplayData(
    val version: Int,
    val time: Date,
    val serverIp: String,
    val recordPlayer: String,
) {
    private object FakeServer : Net(null) {
        override fun server(): Boolean = true
    }

    class Writer(outputStream: OutputStream) : Closeable {
        val writes = DataOutputStream(DeflaterOutputStream(outputStream))
        private val startTime = Time.time
        private val tmpBuf: ByteBuffer = ByteBuffer.allocate(32768)
        private val tmpWr: Writes = Writes(ByteBufferOutput(tmpBuf))

        fun writeHeader(meta: ReplayData) {
            writes.writeInt(meta.version)
            writes.writeLong(meta.time.time)
            writes.writeUTF(meta.serverIp)
            writes.writeUTF(meta.recordPlayer)
        }

        fun writePacket(packet: Packet) {
            val id = Net.getPacketId(packet).toUInt()
            writes.writeFloat(Time.time - startTime)
            writes.writeByte(id.toInt())

            if (packet is Streamable) packet.stream.apply {
                mark(available())
                writes.writeVarShort(available())
                copyTo(writes)
                reset()
            } else {
                tmpBuf.position(0)
                val oldNet = Vars.net
                try {
                    Vars.net = FakeServer
                    packet.write(tmpWr)
                } finally {
                    Vars.net = oldNet
                }
                writes.writeVarShort(tmpBuf.position())
                writes.write(tmpBuf.array(), 0, tmpBuf.position())
            }
        }

        override fun close() {
            writes.close()
        }

        private fun DataOutputStream.writeVarShort(value: Int) {
            if (value > Short.MAX_VALUE) {
                writeInt((1 shl 31) or value)
            } else {
                writeShort(value)
            }
        }
    }

    class Reader(inputStream: InputStream) : Closeable {
        val reads = DataInputStream(InflaterInputStream(inputStream))
        val meta = readHeader()
        var source: Fi? = null

        constructor(fi: Fi) : this(fi.read(32768)) {
            source = fi
        }

        private val arcOldFormat = meta.version <= 10
        private val readsWrap = Reads(reads)

        private fun readHeader(): ReplayData {
            val version = reads.readInt()
            val time = Date(reads.readLong())
            val serverIp = reads.readUTF()
            val recordPlayer = reads.readUTF()
            return ReplayData(version, time, serverIp, recordPlayer)
        }

        @Throws(EOFException::class)
        fun nextPacket(): PacketInfo {
            val offset = if (!arcOldFormat) {
                reads.readFloat()
            } else {
                reads.readLong() * Time.toSeconds / Time.nanosPerMilli / 1000
            }
            val id = reads.readByte()
            val length = reads.readVarShort()
            return PacketInfo(offset, id, length)
        }

        @Throws(IOException::class)
        fun readPacket(info: PacketInfo): Packet {
            val p = Net.newPacket<Packet>(info.id)
            if (p is Streamable) {
                val bs = ByteArray(info.length)
                reads.readFully(bs)
                p.stream = ByteArrayInputStream(bs)
            } else {
                p.read(readsWrap, info.length)
            }
            return p
        }

        fun allPacket(): List<PacketInfo> = buildList {
            while (true) {
                try {
                    val info = nextPacket()
                    reads.skip(info.length.toLong())
                    add(info)
                } catch (e: EOFException) {
                    break
                }
            }
        }

        override fun close() {
            reads.close()
        }

        private fun DataInputStream.readVarShort(): Int {
            val high = readUnsignedShort()
            return if (high and 0x8000 != 0) {
                val low = readUnsignedShort()
                ((high and 0x7FFF) shl 16) + low
            } else {
                high
            }
        }
    }

    data class PacketInfo(
        val offset: Float,
        val id: Byte,
        val length: Int,
    )
}