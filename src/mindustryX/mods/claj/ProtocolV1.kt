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
import java.io.Closeable
import java.nio.ByteBuffer

/** CLajV1 协议
 *
 * 1. 房主连接R，发送"new"
 * 2. R返回$token, 供其他玩家加入
 * 3. 其他玩家连接R，发送"join$token"
 * 4. R通知房主"new", 房主新建另一条连接，发送"host$token"
 * 5. R通过新连接转发房主和玩家的消息
 * */
object ProtocolV1 {
    object Serializer : ArcNetProvider.PacketSerializer() {
        private const val HEADER = (-3).toByte()
        override fun write(buffer: ByteBuffer, o: Any) {
            if (o is String) {
                buffer.put(HEADER)
                TypeIO.writeString(buffer, o)
                return
            }
            super.write(buffer, o)
        }

        override fun read(buffer: ByteBuffer): Any {
            if (buffer.get() == HEADER) return TypeIO.readString(buffer)
            buffer.position(buffer.position() - 1)
            return super.read(buffer)
        }
    }

    class AsServer(val relay: String, val port: Int, val changed: Runnable) : Closeable, NetListener {
        var token: String? = null
        val clients = mutableListOf<Client>()
        val server = Client(8192, 16384, Serializer).apply {
            Threads.daemon("Claj-Server", this)
            addListener(this@AsServer)
            connect(5000, relay, port)
        }

        var closed = false
        val link: String? get() = if (token != null) "$token#$relay:$port" else null

        override fun connected(connection: Connection) {
            server.sendTCP("new")
        }

        override fun received(connection: Connection, msg: Any) {
            if (msg !is String) error("Invalid message: $msg")
            when {
                msg.startsWith("CLaj") -> {
                    token = msg
                    Core.app.post(changed)
                }

                msg == "new" -> newClient()
                else -> Call.sendMessage(msg)
            }
        }

        override fun disconnected(connection: Connection, reason: DcReason) {
            close()
        }

        override fun close() {
            closed = true
            token = null
            clients.forEach { it.close() }
            server.close()
            Core.app.post(changed)
        }

        private fun newClient() {
            val client = Client(8192, 16384, Serializer);
            Threads.daemon("Claj-Client", client)

            client.addListener(Claj.serverListener)
            client.addListener(object : NetListener {
                override fun connected(connection: Connection) {
                    client.sendTCP("host$token")
                }
            })

            client.connect(5000, relay, port)
            clients.add(client)
        }
    }

    fun join(relay: String, port: Int, token: String, success: Runnable) {
        Vars.logic.reset()
        Vars.net.reset()
        Vars.netClient.beginConnecting()
        Vars.net.connect(relay, port) {
            if (!Vars.net.client()) return@connect

            val buffer = ByteBuffer.allocate(8192)
            Serializer.write(buffer, "join$token")
            buffer.limit(buffer.position()).position(0)
            Vars.net.send(buffer, true)

            success.run()
        }
    }
}