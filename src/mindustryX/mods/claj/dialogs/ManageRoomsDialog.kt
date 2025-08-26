package mindustryX.mods.claj.dialogs

import arc.Core
import arc.Events
import arc.graphics.Color
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import mindustry.Vars
import mindustry.game.EventType
import mindustry.game.EventType.ClientPreConnectEvent
import mindustry.gen.Icon
import mindustry.net.Host
import mindustry.ui.dialogs.BaseDialog
import mindustryX.mods.claj.ProtocolV1

class ManageRoomsDialog : BaseDialog("管理claj房间") {
    private val default_servers = listOf("")
    private val api = "https://api.mindustry.top/servers/claj"
    private var servers: List<ClajServer> = emptyList()
    private var fetching: Boolean = false
    private val list: Table

    init {
        Events.run(EventType.HostEvent::class.java) { servers.forEach { it.close() } }
        Events.run(ClientPreConnectEvent::class.java) { servers.forEach { it.close() } }

        addCloseButton()

        cont.defaults().width(if (Vars.mobile) 550f else 750f)

        this.list = cont.table().get()
        list.defaults().growX().padBottom(8f)
        list.update { list.cells.retainAll { cell: Cell<*> -> cell.get() != null } } // remove closed rooms
        cont.row()
        cont.labelWrap("选择一个服务器，创建Claj房间，复制Claj代码给你的朋友来联机").labelAlign(2, 8).padTop(16f).width(400f).get().style.fontColor = Color.lightGray

        shown {
            if (servers.isEmpty() && !fetching) fetchServers()

            list.clearChildren()
            if (fetching) {
                list.add("获取可用服务器中，请稍后...")
            } else if (servers.isEmpty()) {
                list.add("没有可用的Claj服务器，请手动添加").color(Color.lightGray).row()
            }
            servers.forEach {
                list.add(it).row()
                if (!it.hasChildren()) it.rebuild()
            }
        }

        buttons.button("手动添加", Icon.add, Vars.iconMed) {
            Vars.ui.showTextInput("添加Claj服务器", "请输入服务器地址", "") { addr ->
                val port = addr.split(":").getOrNull(1)?.toInt() ?: Vars.port
                servers += ClajServer(addr.split(":")[0], port)
                show()
            }
        }
    }

    fun fetchServers() {
        servers += default_servers.mapNotNull { parseServer(it) }
        fetching = true
        Http.get(api) { res: Http.HttpResponse ->
            servers += Jval.read(res.resultAsString).asArray().asIterable()
                .mapNotNull { parseServer(it.asString()) }
            fetching = false
            if (isShown) Core.app.post { show() }
        }
    }

    private fun parseServer(addr: String): ClajServer? {
        try {
            val host = addr.split(":")[0]
            val port = addr.split(":").getOrNull(1)?.toInt() ?: Vars.port
            return ClajServer(host, port)
        } catch (e: Exception) {
            Log.warn("解析Claj服务器失败: ${e.message}")
            return null
        }
    }

    internal data class ClajServer(val host: String, val port: Int) : Table() {
        private var ping: Result<Host>? = null
        private var con: ProtocolV1.AsServer? = null

        fun rebuild() {
            clearChildren()

            if (con?.closed == true) con = null
            val link = con?.link

            if (link != null) {
                add(link).fontScale(.7f).ellipsis(true).growX()
                button(Icon.copy, Vars.iconMed) {
                    Core.app.clipboardText = link
                    Vars.ui.showInfoFade("@copied")
                }
                button(Icon.cancel, Vars.iconMed) { this.close() }
            } else {
                add("$host:$port").growX()
                label {
                    val info = ping ?: return@label "Ping..."
                    info.getOrNull()?.let { "${it.ping}ms" } ?: "[red]Err"
                }
                button(Icon.refresh, Vars.iconMed, this::ping).disabled { ping == null }
                button(Icon.play, Vars.iconMed) {
                    try {
                        con = ProtocolV1.AsServer(host, port, this::rebuild)
                    } catch (e: Exception) {
                        Vars.ui.showErrorMessage(e.message)
                    }
                }.disabled { ping?.isSuccess != true || con != null }

                if (ping == null) ping()
            }
        }

        fun ping() {
            ping = null
            Vars.net.pingHost(host, port, { ping = Result.success(it); }, { ping = Result.failure(it) })
        }

        fun close() {
            con?.close()
            con = null
        }
    }
}