package mindustryX.features

import arc.Core
import arc.func.Cons
import arc.net.dns.ArcDns
import arc.util.Log
import arc.util.Threads
import arc.util.Time
import arc.util.Timer
import mindustry.Vars
import mindustry.net.Host
import mindustry.net.NetworkIO
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

object PingService {
    private class Req(val addr: InetAddress, val port: Int, val handled: AtomicBoolean, val callback: Cons<Host>) {
        val time = Time.millis()
        fun handle(packet: DatagramPacket) {
            val buffer = ByteBuffer.wrap(packet.data)
            val host = NetworkIO.readServerData(Time.timeSinceMillis(time).toInt(), addr.hostAddress, buffer)
            host.port = port
            if (handled.compareAndSet(false, true))
                Core.app.post { callback.get(host) }
        }

        fun send() {
            synchronized(requests) {
                requests.add(this)
            }
            socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, addr, port))
        }
    }

    private val requests = LinkedList<Req>()
    private val socket by lazy {
        DatagramSocket().also { socket ->
            Threads.daemon("PingService") {
                while (true) {
                    val packet = DatagramPacket(ByteArray(512), 512)
                    socket.receive(packet)
                    val req = synchronized(requests) {
                        requests.removeFirst { it.addr == packet.address && it.port == packet.port }
                    } ?: continue
                    kotlin.runCatching { req.handle(packet) }.onFailure {
                        Log.warn("Failed to handle ping request", it)
                    }
                }
            }
        }
    }

    fun pingHost(address: String, port: Int, valid: Cons<Host>, invalid: Cons<Exception>) {
        val handled = AtomicBoolean(false)
        Req(InetAddress.getByName(address), port, handled, valid).send()
        if (port == Vars.port) {
            for (record in ArcDns.getSrvRecords("_mindustry._tcp.$address")) {
                Req(InetAddress.getByName(record.target), record.port, handled, valid).send()
            }
        }
        Timer.schedule({
            synchronized(requests) {
                requests.removeAll { it.handled == handled }
            }
            if (handled.compareAndSet(false, true))
                Core.app.post { invalid.get(TimeoutException()) }
        }, 2f)
    }

    private inline fun <T> LinkedList<T>.removeFirst(f: (T) -> Boolean): T? {
        val iter = iterator()
        while (iter.hasNext()) {
            val it = iter.next()
            if (f(it)) {
                iter.remove()
                return it
            }
        }
        return null
    }
}