package mindustryX.mods.claj

import arc.Core
import arc.net.Client
import arc.net.Connection
import arc.net.DcReason
import arc.net.NetListener
import arc.util.Threads
import mindustry.Vars
import mindustry.gen.Call
import mindustry.io.TypeIO
import mindustry.net.ArcNetProvider
import mindustryX.VarsX
import mindustryX.mods.claj.ProtocolV2.RoomCreated
import java.io.Closeable
import java.nio.ByteBuffer

/** CLajV2 协议
 *
 * 1. 房主连接R，发送[RoomCreationRequest]
 * 2. R返回[RoomCreated],含id, 供其他玩家加入
 * 3. 其他玩家连接R，发送[RoomJoin]
 * 4. R通知房主[ClientJoin]
 * 5. R通过[CLajPacket]转发房主和玩家的消息
 * 6. 房主断开连接可通过[RoomCloseRequest] [RoomCreated]，其他玩家断开连接触发[ClientClosed]
 * */
object ProtocolV2 {
    enum class RoomCloseReason {
        closed,
        obsoleteClient,
        outdatedVersion,
        serverClosed,
    }

    sealed class CLajPacket(val id: Int)
    data class ClientPacket(val conId: Int, val isTcp: Boolean, val packet: Any) : CLajPacket(0)
    data class ClientClosed(val conId: Int, val reason: DcReason) : CLajPacket(1)
    data class ClientJoin(val conId: Int, val room: Long) : CLajPacket(2)
    data class ClientIdling(val conId: Int) : CLajPacket(3)
    data class RoomCreationRequest(val version: String) : CLajPacket(4)
    data object RoomCloseRequest : CLajPacket(5)
    data class RoomClosed(val reason: RoomCloseReason) : CLajPacket(6)
    data class RoomCreated(val room: Long) : CLajPacket(7)
    data class RoomJoin(val room: Long) : CLajPacket(8)
    sealed interface MsgPacket
    data class CLajMessage(val message: String) : CLajPacket(9), MsgPacket
    data class CLajMessage2(val message: Int) : CLajPacket(10), MsgPacket
    data class CLajPopupMessage(val message: String) : CLajPacket(11), MsgPacket

    object Serializer : ArcNetProvider.PacketSerializer() {
        private const val HEADER = (-4).toByte()
        override fun read(buffer: ByteBuffer): Any {
            if (buffer.get() == HEADER) {
                return when (val id = buffer.get().toInt()) {
                    0 -> ClientPacket(buffer.getInt(), buffer.get() > 0, super.read(buffer))
                    1 -> ClientClosed(buffer.getInt(), DcReason.entries[buffer.get().toInt()])
                    2 -> ClientJoin(buffer.getInt(), buffer.long)
                    3 -> ClientIdling(buffer.getInt())
                    4 -> RoomCreationRequest(TypeIO.readString(buffer))
                    5 -> RoomCloseRequest
                    6 -> RoomClosed(RoomCloseReason.entries[buffer.get().toInt()])
                    7 -> RoomCreated(buffer.long)
                    8 -> RoomJoin(buffer.long)
                    9 -> CLajMessage(TypeIO.readString(buffer))
                    10 -> CLajMessage2(buffer.int)
                    11 -> CLajPopupMessage(TypeIO.readString(buffer))
                    else -> error("Unknown CLajPacket id: $id" + buffer.position() + "/" + buffer.limit())
                }
            }
            buffer.position(buffer.position() - 1)
            return super.read(buffer)
        }

        override fun write(buffer: ByteBuffer, o: Any) {
            if (o is CLajPacket) {
                buffer.put(HEADER)
                buffer.put(o.id.toByte())
                when (o) {
                    is ClientPacket -> {
                        buffer.put(if (o.isTcp) 1 else 0)
                        super.write(buffer, o.packet)
                    }

                    is ClientClosed -> {
                        buffer.putInt(o.conId)
                        buffer.put(o.reason.ordinal.toByte())
                    }

                    is ClientJoin -> {
                        buffer.putInt(o.conId)
                        buffer.putLong(o.room)
                    }

                    is ClientIdling -> buffer.putInt(o.conId)
                    is RoomCreationRequest -> TypeIO.writeString(buffer, o.version)
                    is RoomCloseRequest -> Unit// no data
                    is RoomClosed -> buffer.put(o.reason.ordinal.toByte())
                    is RoomCreated -> buffer.putLong(o.room)
                    is RoomJoin -> buffer.putLong(o.room)
                    is CLajMessage -> TypeIO.writeString(buffer, o.message)
                    is CLajMessage2 -> buffer.putInt(o.message)
                    is CLajPopupMessage -> TypeIO.writeString(buffer, o.message)
                }
                return
            }
            super.write(buffer, o)
        }
    }

    fun handleMsgPacket(msg: MsgPacket) {
        when (msg) {
            is CLajMessage -> {
                Call.sendMessage("[CLaj Server]: ${msg.message}")
            }

            is CLajMessage2 -> {
                Call.sendMessage("[CLaj Server]: Code ${msg.message}")
            }

            is CLajPopupMessage -> {
                Vars.ui.showText("[CLaj Server]", msg.message)
            }
        }
    }

    class AsServer(val redirect: String, val port: Int, val changed: Runnable) : Closeable, NetListener {
        var token: Long? = null
        val clients = mutableListOf<Client>()
        val server = Client(8192, 16384, Serializer).apply {
            Threads.daemon("Claj-Server", this)
            addListener(this@AsServer)
            connect(5000, redirect, port)
        }

        var closed = false
        val link: String? get() = if (token != null) "$token#$redirect:$port" else null

        override fun connected(connection: Connection) {
            server.sendTCP(RoomCreationRequest("MDTX ${VarsX.version}"))
        }

        override fun received(connection: Connection, msg: Any) {
            if (msg !is CLajPacket) error("Invalid packet: $msg")
            when (msg) {
                is RoomCreated -> {
                    token = msg.room
                    Core.app.post(changed)
                }

                is MsgPacket -> Core.app.post { handleMsgPacket(msg) }
                is RoomClosed -> close(msg.reason)
                //handle client
                is ClientJoin -> {
                    if (msg.room != token) {
                        server.sendTCP(ClientClosed(msg.conId, DcReason.error))
                        return
                    }
                    newClient(msg.conId)
                }

                is ClientIdling -> {}
                is ClientPacket -> {}
                is ClientClosed -> {}
                else -> error("Unexpected packet: $msg")
            }
        }

        override fun disconnected(connection: Connection, reason: DcReason) {
            close()
        }

        override fun close() = close(RoomCloseReason.closed)
        fun close(reason: RoomCloseReason = RoomCloseReason.closed) {
            if (closed) return
            closed = true
            token = null
            clients.forEach { it.close(DcReason.closed) }
            server.close()
            Core.app.post(changed)
        }

        private fun newClient(conId: Int) {
            val client = Client(conId)
            clients.add(client)
        }

        inner class Client(val conId: Int) : Connection() {
            init {
                addListener(Claj.serverListener)
            }

            override fun sendTCP(o: Any): Int {
                return server.sendTCP(ClientPacket(conId, true, o))
            }

            override fun sendUDP(o: Any): Int {
                return server.sendTCP(ClientPacket(conId, false, o))
            }
        }
    }

    fun join(redirect: String, port: Int, token: Long, success: Runnable) {
        Vars.logic.reset()
        Vars.net.reset()
        Vars.netClient.beginConnecting()
        Vars.net.connect(redirect, port) {
            if (!Vars.net.client()) return@connect

            val buffer = ByteBuffer.allocate(8192)
            Serializer.write(buffer, RoomJoin(token))
            buffer.limit(buffer.position()).position(0)
            Vars.net.send(buffer, true)

            success.run()
        }
    }
}