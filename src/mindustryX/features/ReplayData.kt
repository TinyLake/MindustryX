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
import mindustryX.features.replay.ReplayKeyframeMeta
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.Date
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.jvm.JvmOverloads

data class ReplayData(
    val version: Int,
    val time: Date,
    val serverIp: String,
    val recordPlayer: String,
) {
    private object FakeServer : Net(null) {
        override fun server(): Boolean = true
    }

    enum class StorageFormat {
        Legacy,
        Container,
    }

    enum class RecordType(val id: Int) {
        WorldData(1),
        Packet(2),
        KeyframeMeta(3),
        ;

        companion object {
            fun fromId(id: Int): RecordType? = entries.firstOrNull { it.id == id }
        }
    }

    sealed class RecordInfo(
        open val offset: Float,
        open val type: RecordType,
        open val length: Int,
        open val packetId: Byte = 0,
        open val unreadLength: Int = length,
    ) {
        data class WorldData(
            override val length: Int,
        ) : RecordInfo(0f, RecordType.WorldData, length)

        data class Packet(
            val packetInfo: PacketInfo,
        ) : RecordInfo(
            packetInfo.offset,
            RecordType.Packet,
            packetInfo.length,
            packetInfo.id,
            packetInfo.unreadLength,
        )

        data class KeyframeMeta(
            val keyframeMeta: ReplayKeyframeMeta,
            override val offset: Float,
            override val length: Int,
        ) : RecordInfo(offset, RecordType.KeyframeMeta, length, 0, 0)
    }

    sealed class PlaybackRecord(
        open val offset: Float,
    ) {
        data class WorldData(
            val data: ByteArray,
        ) : PlaybackRecord(0f)

        data class Packet(
            val packet: mindustry.net.Packet,
            override val offset: Float,
        ) : PlaybackRecord(offset)

        data class KeyframeMeta(
            val keyframeMeta: ReplayKeyframeMeta,
            override val offset: Float,
        ) : PlaybackRecord(offset)
    }

    class Writer @JvmOverloads constructor(
        outputStream: OutputStream,
        private val containerFormat: Boolean = true,
    ) : Closeable {
        private val writes = DataOutputStream(DeflaterOutputStream(outputStream))
        private val startTime = Time.time
        private val tmpBuf: ByteBuffer = ByteBuffer.allocate(32768)
        private val tmpWr: Writes = Writes(ByteBufferOutput(tmpBuf))

        fun writeHeader(meta: ReplayData) {
            if (containerFormat) {
                writes.writeInt(FILE_MAGIC)
                writes.writeInt(CONTAINER_FORMAT_VERSION)
            }
            writes.writeInt(meta.version)
            writes.writeLong(meta.time.time)
            writes.writeUTF(meta.serverIp)
            writes.writeUTF(meta.recordPlayer)
        }

        fun writePacket(packet: Packet) {
            if (containerFormat) {
                writePacketRecord(packet, Time.time - startTime)
            } else {
                writeLegacyPacket(packet, Time.time - startTime)
            }
        }

        fun writeWorldData(data: ByteArray) {
            ensureContainerFormat()
            writes.writeByte(RecordType.WorldData.id)
            writes.writeInt(data.size)
            writes.write(data)
        }

        @JvmOverloads
        fun writeKeyframeMeta(meta: ReplayKeyframeMeta, offset: Float = Time.time - startTime) {
            ensureContainerFormat()
            val body = ByteArrayOutputStream()
            val data = DataOutputStream(body)
            data.writeFloat(offset)
            data.writeLong(meta.timeline)
            data.writeUTF(meta.tag)
            data.writeInt(meta.flags)
            data.writeInt(meta.snapshotSize)
            data.flush()

            writes.writeByte(RecordType.KeyframeMeta.id)
            writes.writeInt(body.size())
            body.writeTo(writes)
        }

        private fun writePacketRecord(packet: Packet, offset: Float) {
            ensureContainerFormat()
            val payload = serializePacketPayload(packet)
            val body = ByteArrayOutputStream(payload.data.size + 12)
            val data = DataOutputStream(body)
            data.writeFloat(offset)
            data.writeByte(payload.id.toInt())
            data.writeVarShort(payload.data.size)
            data.write(payload.data)
            data.flush()

            writes.writeByte(RecordType.Packet.id)
            writes.writeInt(body.size())
            body.writeTo(writes)
        }

        private fun writeLegacyPacket(packet: Packet, offset: Float) {
            val payload = serializePacketPayload(packet)
            writes.writeFloat(offset)
            writes.writeByte(payload.id.toInt())
            writes.writeVarShort(payload.data.size)
            writes.write(payload.data)
        }

        private fun serializePacketPayload(packet: Packet): SerializedPacket {
            val id = Net.getPacketId(packet)
            if (packet is Streamable) {
                val stream = packet.stream ?: return SerializedPacket(id, ByteArray(0))
                stream.mark(stream.available())
                return try {
                    SerializedPacket(id, stream.readBytes())
                } finally {
                    stream.reset()
                }
            }

            tmpBuf.position(0)
            val oldNet = Vars.net
            try {
                Vars.net = FakeServer
                packet.write(tmpWr)
            } finally {
                Vars.net = oldNet
            }

            val length = tmpBuf.position()
            val bytes = ByteArray(length)
            System.arraycopy(tmpBuf.array(), 0, bytes, 0, length)
            return SerializedPacket(id, bytes)
        }

        override fun close() {
            writes.close()
        }

        private fun ensureContainerFormat() {
            check(containerFormat) { "This writer is configured for legacy replay output." }
        }

        private data class SerializedPacket(
            val id: Byte,
            val data: ByteArray,
        )

        private fun DataOutputStream.writeVarShort(value: Int) {
            if (value > Short.MAX_VALUE) {
                writeInt((1 shl 31) or value)
            } else {
                writeShort(value)
            }
        }
    }

    class Reader(inputStream: InputStream) : Closeable {
        private val inflated = BufferedInputStream(InflaterInputStream(inputStream))
        private val reads = DataInputStream(inflated)
        private val readsWrap = Reads(reads)

        val storageFormat: StorageFormat
        val formatVersion: Int
        val meta: ReplayData
        var source: Fi? = null

        constructor(fi: Fi) : this(fi.read(32768)) {
            source = fi
        }

        private val arcOldFormat: Boolean

        init {
            val header = readHeader()
            storageFormat = header.storageFormat
            formatVersion = header.formatVersion
            meta = header.meta
            arcOldFormat = meta.version <= 10
        }

        @Throws(EOFException::class)
        fun nextRecord(): RecordInfo {
            if (storageFormat == StorageFormat.Legacy) {
                return RecordInfo.Packet(readLegacyPacketInfo())
            }

            val typeId = reads.readUnsignedByte()
            val bodyLength = reads.readInt()
            if (bodyLength < 0) throw IOException("Negative replay record length: $bodyLength")

            return when (RecordType.fromId(typeId)) {
                RecordType.WorldData -> RecordInfo.WorldData(bodyLength)
                RecordType.Packet -> RecordInfo.Packet(readContainerPacketInfo(bodyLength))
                RecordType.KeyframeMeta -> readKeyframeMetaInfo(bodyLength)
                null -> throw IOException("Unknown replay record type: $typeId")
            }
        }

        @Throws(EOFException::class)
        fun nextPacket(): PacketInfo {
            if (storageFormat == StorageFormat.Legacy) {
                return readLegacyPacketInfo()
            }

            while (true) {
                when (val record = nextRecord()) {
                    is RecordInfo.Packet -> return record.packetInfo
                    else -> skipRemaining(record)
                }
            }
        }

        @Throws(IOException::class)
        fun nextPlaybackRecord(): PlaybackRecord {
            val info = nextRecord()
            return when (info) {
                is RecordInfo.WorldData -> PlaybackRecord.WorldData(readWorldData(info))
                is RecordInfo.Packet -> PlaybackRecord.Packet(readPacket(info.packetInfo), info.offset)
                is RecordInfo.KeyframeMeta -> PlaybackRecord.KeyframeMeta(readKeyframeMeta(info), info.offset)
            }
        }

        @Throws(IOException::class)
        fun readPacket(info: PacketInfo): Packet {
            val packet = Net.newPacket<Packet>(info.id)
            if (packet is Streamable) {
                val bytes = ByteArray(info.length)
                reads.readFully(bytes)
                packet.stream = ByteArrayInputStream(bytes)
            } else {
                packet.read(readsWrap, info.length)
            }
            if (info.trailingBytes > 0) {
                skipFully(info.trailingBytes.toLong())
            }
            return packet
        }

        @Throws(IOException::class)
        fun readPacket(info: RecordInfo): Packet {
            if (info !is RecordInfo.Packet) {
                throw IOException("Record is not a packet: ${info.type}")
            }
            return readPacket(info.packetInfo)
        }

        @Throws(IOException::class)
        fun readWorldData(info: RecordInfo): ByteArray {
            if (info !is RecordInfo.WorldData) {
                throw IOException("Record is not world data: ${info.type}")
            }
            val bytes = ByteArray(info.length)
            reads.readFully(bytes)
            return bytes
        }

        @Throws(IOException::class)
        fun readKeyframeMeta(info: RecordInfo): ReplayKeyframeMeta {
            if (info !is RecordInfo.KeyframeMeta) {
                throw IOException("Record is not keyframe metadata: ${info.type}")
            }
            return info.keyframeMeta
        }

        fun allPacket(): List<PacketInfo> = buildList {
            while (true) {
                try {
                    val info = nextPacket()
                    skipFully(info.unreadLength.toLong())
                    add(info)
                } catch (_: EOFException) {
                    break
                }
            }
        }

        fun allRecord(): List<RecordInfo> = buildList {
            while (true) {
                try {
                    val info = nextRecord()
                    skipRemaining(info)
                    add(info)
                } catch (_: EOFException) {
                    break
                }
            }
        }

        fun allRecords(): List<RecordInfo> = allRecord()

        override fun close() {
            reads.close()
        }

        private fun readHeader(): HeaderInfo {
            inflated.mark(CONTAINER_HEADER_BYTES)
            val magic = reads.readInt()
            if (magic == FILE_MAGIC) {
                val version = reads.readInt()
                return HeaderInfo(StorageFormat.Container, version, readMeta())
            }
            inflated.reset()
            return HeaderInfo(StorageFormat.Legacy, LEGACY_FORMAT_VERSION, readMeta())
        }

        private fun readMeta(): ReplayData {
            val version = reads.readInt()
            val time = Date(reads.readLong())
            val serverIp = reads.readUTF()
            val recordPlayer = reads.readUTF()
            return ReplayData(version, time, serverIp, recordPlayer)
        }

        private fun readLegacyPacketInfo(): PacketInfo {
            val offset = if (!arcOldFormat) {
                reads.readFloat()
            } else {
                reads.readLong() * Time.toSeconds / Time.nanosPerMilli / 1000
            }
            val id = reads.readByte()
            val length = reads.readVarShort()
            return PacketInfo(offset, id, length)
        }

        private fun readContainerPacketInfo(bodyLength: Int): PacketInfo {
            val offset = reads.readFloat()
            val id = reads.readByte()
            val length = reads.readVarShort()
            val trailingBytes = bodyLength - (PACKET_PREFIX_BYTES + varShortSize(length) + length)
            if (trailingBytes < 0) {
                throw IOException("Malformed replay packet record length: $bodyLength")
            }
            return PacketInfo(offset, id, length, trailingBytes)
        }

        private fun readKeyframeMetaInfo(bodyLength: Int): RecordInfo.KeyframeMeta {
            val body = ByteArray(bodyLength)
            reads.readFully(body)
            val input = DataInputStream(ByteArrayInputStream(body))
            val offset = input.readFloat()
            val timeline = input.readLong()
            val tag = input.readUTF()
            val flags = input.readInt()
            val snapshotSize = input.readInt()
            return RecordInfo.KeyframeMeta(
                ReplayKeyframeMeta(timeline, tag, flags, snapshotSize),
                offset,
                bodyLength,
            )
        }

        private fun skipRemaining(info: RecordInfo) {
            skipFully(info.unreadLength.toLong())
        }

        private fun skipFully(length: Long) {
            var remaining = length
            while (remaining > 0) {
                val skipped = reads.skip(remaining)
                if (skipped > 0) {
                    remaining -= skipped
                } else {
                    reads.readByte()
                    remaining--
                }
            }
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

        private data class HeaderInfo(
            val storageFormat: StorageFormat,
            val formatVersion: Int,
            val meta: ReplayData,
        )
    }

    data class PacketInfo(
        val offset: Float,
        val id: Byte,
        val length: Int,
        val trailingBytes: Int = 0,
    ) {
        val unreadLength: Int
            get() = length + trailingBytes
    }

    companion object {
        const val FILE_MAGIC: Int = 0x4D524550
        const val CONTAINER_FORMAT_VERSION: Int = 1
        const val LEGACY_FORMAT_VERSION: Int = 0

        private const val CONTAINER_HEADER_BYTES = 8
        private const val PACKET_PREFIX_BYTES = 5

        private fun varShortSize(value: Int): Int = if (value > Short.MAX_VALUE) 4 else 2
    }
}
