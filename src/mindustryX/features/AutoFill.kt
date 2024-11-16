package mindustryX.features

import arc.Core
import arc.Events
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Building
import mindustry.gen.Call
import mindustry.world.blocks.storage.StorageBlock

/** 一键装弹
 * 尝试从附近核心/仓库取背包同种资源
 * 尝试给附近新建炮台装弹
 * 尝试给鼠标所在炮台/工厂装弹
 */
object AutoFill {
    @JvmField
    var enable = false
    private val justTransferred = mutableSetOf<Building>()
    private val justTransferredNext = mutableSetOf<Building>()

    private fun justTransferred(build: Building): Boolean {
        if (justTransferred.add(build)) return false
        justTransferredNext.add(build)
        return true
    }

    private fun tryFill(build: Building) {
        val player = Vars.player ?: return
        if (build.block is StorageBlock || build.team != player.team()) return
        val item = player.unit()?.item() ?: return
        if (build.within(player, Vars.itemTransferRange) && build.acceptItem(build, item)) {
            if (justTransferred(build)) return
            Call.transferInventory(player, build)
        }
    }

    init {
        Events.on(EventType.BlockBuildEndEvent::class.java) { event ->
            if (!enable || event.breaking) return@on
            val build = event.tile.build ?: return@on
            tryFill(build)
        }
        Events.run(EventType.Trigger.update, ::update)
    }

    fun update() {
        justTransferred.clear()
        justTransferred.addAll(justTransferredNext)
        justTransferredNext.clear()
        if (!enable || Vars.player.dead()) return
        val player = Vars.player
        val item = player.unit()?.item() ?: return

        Core.input.mouseWorld().run { Vars.world.buildWorld(x, y) }?.let { tryFill(it) }

        if (player.unit().stack.amount < player.unit().itemCapacity() * 0.5) {
            Vars.indexer.findTile(player.team(), player.x, player.y, Vars.buildingRange, {
                it.block is StorageBlock && it.items.has(item)
            })?.let { find ->
                if (justTransferred(find)) return@let
                Call.requestItem(player, find, item, 9999)
            }
        }
    }
}