package mindustryX.features

import arc.Core
import arc.Events
import arc.util.Interval
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Building
import mindustry.gen.Call
import mindustry.type.Category
import mindustry.world.blocks.storage.StorageBlock

/** 一键装弹
 * 尝试从附近核心/仓库取背包同种资源
 * 尝试给附近新建炮台装弹
 * 尝试给鼠标所在炮台/工厂装弹
 */
object AutoFill {
    @JvmField
    var enable = false
    private val transferredThisTick = mutableSetOf<Building>()
    private val cooldown = SettingsV2.SliderPref("autoFill.cooldown", 300, 0, 3000, 100)
    private val minFill = SettingsV2.SliderPref("autoFill.minFill", 5, 1, 20, 1)
    private val fillStorageBlock = SettingsV2.CheckPref("autoFill.fillStorageBlock")
    private val fillDistribution = SettingsV2.CheckPref("autoFill.fillDistribution")
    val settings = listOf(cooldown, minFill, fillStorageBlock, fillDistribution)

    private var timer = Interval()

    private fun justTransferred(build: Building): Boolean {
        transferredThisTick.add(build)
        return !timer[cooldown.value * 60f / 1000]
    }

    private fun tryFill(build: Building) {
        val player = Vars.player ?: return
        if (player.dead() || player.unit().stack.amount == 0) return
        if (build.team != player.team()
            || (!fillStorageBlock.value && build.block is StorageBlock)
            || (!fillDistribution.value && build.block.category == Category.distribution) || build.team != player.team()
        ) return
        val item = player.unit().item() ?: return
        if (build.within(player, Vars.itemTransferRange) && build.acceptStack(item, 9999, player.unit()) >= minFill.value) {
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
        transferredThisTick.clear()
        if (!enable || Vars.player.dead()) return
        val player = Vars.player
        val item = player.unit()?.item() ?: return

        Core.input.mouseWorld().run { Vars.world.buildWorld(x, y) }?.let { tryFill(it) }

        if (player.unit().stack.amount < player.unit().itemCapacity() * 0.5) {
            Vars.indexer.findTile(player.team(), player.x, player.y, Vars.buildingRange) {
                it.block is StorageBlock && it.items.has(item) && it !in transferredThisTick
            }?.let { find ->
                if (justTransferred(find)) return@let
                Call.requestItem(player, find, item, 9999)
            }
        }
    }
}