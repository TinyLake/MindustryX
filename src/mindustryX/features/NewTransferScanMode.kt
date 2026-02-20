package mindustryX.features

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.struct.IntSet
import arc.util.Tmp
import mindustry.Vars.*
import mindustry.content.Items
import mindustry.gen.Building
import mindustry.gen.Icon
import mindustry.graphics.Drawf
import mindustry.graphics.Layer
import mindustry.graphics.Pal
import mindustry.world.blocks.campaign.LandingPad
import mindustry.world.blocks.distribution.*
import mindustry.world.blocks.distribution.ArmoredConveyor.ArmoredConveyorBuild
import mindustry.world.blocks.liquid.ArmoredConduit.ArmoredConduitBuild
import mindustry.world.blocks.liquid.Conduit
import mindustry.world.blocks.liquid.LiquidBridge
import mindustry.world.blocks.liquid.LiquidJunction
import mindustry.world.blocks.liquid.LiquidRouter
import mindustry.world.blocks.production.*
import mindustry.world.blocks.sandbox.ItemSource
import mindustry.world.blocks.sandbox.ItemVoid
import mindustry.world.blocks.sandbox.LiquidSource
import mindustry.world.blocks.sandbox.LiquidVoid
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.blocks.storage.StorageBlock
import mindustry.world.blocks.storage.Unloader
import mindustry.world.blocks.units.UnitCargoUnloadPoint
import mindustryX.VarsX
import mindustryX.features.func.drawText

/**
 * 新的物流扫描模式 - 支持物品和液体传输的可视化
 * 分离收集、逻辑判断和渲染三个部分
 * New transport scanning mode - supports both item and liquid transport visualization
 * Separated into collection, logic, and rendering components
 */
object NewTransferScanMode {
    private val itemInputColor = Color.valueOf("ff8000")
    private val itemOutputColor = Color.valueOf("80ff00")
    private val liquidInputColor = Color.valueOf("4080ff")
    private val liquidOutputColor = Color.valueOf("00ffff")

    enum class TransportType {
        ITEM, LIQUID
    }

    /**
     * 主入口 - 渲染函数
     * Main entry point - rendering function
     */
    fun draw() {
        Draw.z(Layer.overlayUI + 0.01f)

        val pos = Core.input.mouseWorld()
        val text = VarsX.uiTextBundle.coordinateDistance(
            (pos.x / tilesize).toInt(),
            (pos.y / tilesize).toInt(),
            (player.dst(pos) / tilesize).toInt()
        )
        drawText(pos, text)

        Draw.z(Layer.overlayUI)
        // 获取鼠标悬停的建筑
        val build = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y)?.build ?: return
        if (!build.isDiscovered(player.team())) {
            return
        }

        Drawf.selected(build, Pal.accent)
        startBuild = build
        type = TransportType.ITEM; drawColor = itemOutputColor; drawOutput(build); visited.clear()
        type = TransportType.LIQUID; drawColor = liquidOutputColor; drawOutput(build); visited.clear()
        type = TransportType.ITEM; drawColor = itemInputColor; drawInput(build); visited.clear()
        type = TransportType.LIQUID; drawColor = liquidInputColor; drawInput(build); visited.clear()
    }

    private const val maxStack = 128

    private var startBuild: Building? = null
    private var type: TransportType = TransportType.ITEM
    private var drawColor: Color = itemOutputColor

    private val visited = IntSet()
    private var stack = ArrayDeque<Building>(maxStack)

    private fun drawOutput(build: Building, previous: Building? = null) {
        if (!visited.add(build.id * 10007 + (previous?.id ?: 0))) return
        if (stack.size == maxStack) return drawStackOverflow(build)
        val wrapper = getWrapper(build)
        if (wrapper.isEndPoint && build != startBuild) {
            Drawf.selected(build, drawColor)
            return
        }
        val validReceiver = buildList {
            //主动输出
            val outputs = previous?.let { wrapper.getOutputs(it) } ?: wrapper.getOutputs()
            for (output in outputs) {
                if (!getWrapper(output).canInput(build)) continue
                add(output)
            }
            //被动拉取
            build.proximity.forEach {
                if (getWrapper(it).activeInput(build)) {
                    add(it)
                }
            }
        }
        stack.addLast(build)
        for (output in validReceiver) {
            drawConnection(build, output, drawColor)
            drawOutput(output, previous = build)
        }
        stack.removeLast()
    }

    private fun drawInput(build: Building, previous: Building? = null) {
        if (!visited.add(build.id * 10007 + (previous?.id ?: 0))) return
        if (stack.size == maxStack) return drawStackOverflow(build)
        val wrapper = getWrapper(build)
        if (wrapper.isEndPoint && build != startBuild) {
            Drawf.selected(build, drawColor)
            return
        }
        val validSource = buildList {
            //已知输入/主动抽取
            addAll(wrapper.activeInputs())
            //检查邻居
            build.proximity.forEach { receiver ->
                val canInput = previous?.let { wrapper.canInput(receiver, it) } ?: wrapper.canInput(receiver)
                if (canInput && getWrapper(receiver).canOutput(build)) {
                    add(receiver)
                }
            }
        }
        stack.addLast(build)
        for (source in validSource) {
            drawConnection(source, build, drawColor)
            drawInput(source, previous = build)
        }
        stack.removeLast()
    }

    /**
     * 绘制单个连接
     * Draw single connection
     */
    private fun drawConnection(from: Building, to: Building, color: Color, alpha: Float = 1f) {
        val x1 = from.tile.drawx()
        val y1 = from.tile.drawy()
        val x2 = to.tile.drawx()
        val y2 = to.tile.drawy()

        Draw.color(color, alpha * (Mathf.absin(4f, 1f) * 0.4f + 0.6f))
        Lines.stroke(1.5f)
        Lines.line(x1, y1, x2, y2)
        Draw.reset()

        val dst = Mathf.dst(x1, y1, x2, y2)

        if (dst > tilesize) {
            Draw.color(color, alpha)

            // 起点圆点
            Fill.circle(x1, y1, 1.8f)

            // 方向箭头
            val fromPos = Tmp.v1.set(x1, y1)
            val toPos = Tmp.v2.set(x2, y2)

            val midPoint = Tmp.v3.set(fromPos).lerp(toPos, 0.5f)
            val angle = fromPos.angleTo(toPos)

            Fill.poly(midPoint.x, midPoint.y, 3, 3f, angle)
            Draw.reset()
        }
    }

    private fun drawStackOverflow(build: Building) {
        Draw.color(Color.orange)
        Draw.rect(Icon.none.region, build.x, build.y)
        Draw.color()
    }

    abstract class BuildingAdaptor(
        val isEndPoint: Boolean = false,
    ) {

        //active output, haven't checked input
        open fun canOutput(to: Building): Boolean = to in getOutputs()
        open fun getOutputs(): List<Building> = emptyList()
        open fun getOutputs(from: Building): List<Building> = getOutputs()

        //passive input
        open fun canInput(from: Building): Boolean = false
        open fun canInput(from: Building, to: Building): Boolean = canInput(from)

        //active input(pull) from other building, like unloader. no need check canOutput
        open fun activeInput(from: Building): Boolean = from in activeInputs()
        open fun activeInputs(): List<Building> = emptyList()
    }

    private inline fun itemOnly(body: () -> BuildingAdaptor) = if (type == TransportType.ITEM) body() else NoopAdaptor
    private inline fun liquidOnly(body: () -> BuildingAdaptor) = if (type == TransportType.LIQUID) body() else NoopAdaptor

    /**
     * 获取建筑的包装器实例
     * Get wrapper instance for build
     */

    fun getWrapper(build: Building): BuildingAdaptor = when (build) {
        is Conduit.ConduitBuild -> liquidOnly { ConveyorAdaptor(build) }
        is LiquidRouter.LiquidRouterBuild -> liquidOnly { RouterAdaptor(build) }
        is LiquidBridge.LiquidBridgeBuild -> liquidOnly { BridgeAdaptor(build) }
        is LiquidJunction.LiquidJunctionBuild -> liquidOnly { JunctionAdaptor(build) }
        is DirectionLiquidBridge.DuctBridgeBuild -> liquidOnly { DirectionBridgeAdaptor(build) }
        is Pump.PumpBuild, is LiquidSource.LiquidSourceBuild -> liquidOnly { SourceAdaptor(build) }
        is LiquidVoid.LiquidVoidBuild -> liquidOnly { VoidAdaptor() }

        is Conveyor.ConveyorBuild, is Duct.DuctBuild -> itemOnly { ConveyorAdaptor(build) }
        is Router.RouterBuild -> itemOnly{ RouterAdaptor(build) }
        is Sorter.SorterBuild, is OverflowGate.OverflowGateBuild -> itemOnly { InstantAdaptor(build) }
        is ItemBridge.ItemBridgeBuild -> itemOnly { BridgeAdaptor(build) }
        is StackConveyor.StackConveyorBuild -> itemOnly { StackConveyorAdaptor(build) }
        is Junction.JunctionBuild, is DuctJunction.DuctJunctionBuild -> itemOnly { JunctionAdaptor(build) }
        is DuctBridge.DuctBridgeBuild -> itemOnly { DirectionBridgeAdaptor(build) }
        is Unloader.UnloaderBuild -> itemOnly { UnloaderAdaptor(build) }
        is DirectionalUnloader.DirectionalUnloaderBuild -> itemOnly { DirectionalUnloaderAdaptor(build) }
        is MassDriver.MassDriverBuild -> itemOnly { MassDriverAdaptor(build) }
        is OverflowDuct.OverflowDuctBuild, is DuctRouter.DuctRouterBuild -> itemOnly { RouterAdaptor(build) }
        is ItemSource.ItemSourceBuild -> itemOnly { SourceAdaptor(build) }
        is UnitCargoUnloadPoint.UnitCargoUnloadPointBuild -> itemOnly { SourceAdaptor(build) }
        is LandingPad.LandingPadBuild -> itemOnly { SourceAdaptor(build) }
        is Drill.DrillBuild, is BeamDrill.BeamDrillBuild, is WallCrafter.WallCrafterBuild -> itemOnly { SourceAdaptor(build) }
        is ItemVoid.ItemVoidBuild, is CoreBlock.CoreBuild -> itemOnly { VoidAdaptor() }

        is GenericCrafter.GenericCrafterBuild -> GenericCrafterAdaptor(build)
        is Incinerator.IncineratorBuild -> VoidAdaptor()
        else -> DefaultAdaptor(build)
    }

    // ===== 具体包装器实现 =====
    private class ConveyorAdaptor(val build: Building) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> = listOfNotNull(build.front())
        override fun canInput(from: Building): Boolean {
            if (from == build.front()) return false
            return when (type) {
                TransportType.ITEM -> from is Conveyor.ConveyorBuild || build !is ArmoredConveyorBuild
                TransportType.LIQUID -> from is Conduit.ConduitBuild || build !is ArmoredConduitBuild
            }
        }
    }

    private class StackConveyorAdaptor(val build: StackConveyor.StackConveyorBuild) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> {
            val front = build.front()
            val back = build.back()

            return when (build.state) {
                // 输出模式
                2 -> if ((build.block as StackConveyor).outputRouter) {
                    build.proximity.filter { it != back }
                } else {
                    listOfNotNull(front)
                }

                1 -> listOfNotNull(front)// 输入模式
                else -> { // 待机模式
                    build.proximity.filterIsInstance<StackConveyor.StackConveyorBuild>()
                }
            }
        }

        override fun canInput(from: Building): Boolean {
            return when (build.state) {
                2 -> from is StackConveyor.StackConveyorBuild && from == build.back()
                1 -> from != build.front()
                else -> from is StackConveyor.StackConveyorBuild && from.front() == build
            }
        }
    }

    private class RouterAdaptor(val build: Building) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> = build.proximity.toList()
        override fun canInput(from: Building): Boolean = true
    }

    private class InstantAdaptor(val build: Building): BuildingAdaptor() {
        override fun getOutputs(): List<Building> = build.proximity.toList()
        override fun getOutputs(from: Building): List<Building> = if (from.block.instantTransfer) build.proximity.filter { !it.block.instantTransfer } else getOutputs()
        override fun canInput(from: Building): Boolean = true
        override fun canInput(from: Building, to: Building): Boolean = !(from.block.instantTransfer && to.block.instantTransfer)
    }

    private class UnloaderAdaptor(val build: Building) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> = build.proximity.filter { it.block !is StorageBlock }
        override fun activeInput(from: Building): Boolean = from.items != null && from.canUnload()
        override fun activeInputs(): List<Building> = build.proximity.filter { activeInput(it) }
    }

    private class DirectionalUnloaderAdaptor(val build: Building) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> = listOfNotNull(build.front())
        override fun activeInput(from: Building): Boolean = from.items != null && from.canUnload() && from == build.back()
        override fun activeInputs(): List<Building> = listOfNotNull(build.back()?.takeIf { activeInput(it) })
    }

    private class BridgeAdaptor(val build: ItemBridge.ItemBridgeBuild) : BuildingAdaptor() {
        val block = build.block as ItemBridge
        private val linkValid get() = block.linkValid(build.tile, world.tile(build.link))
        override fun getOutputs(): List<Building> {
            if (linkValid) return listOf(world.build(build.link))
            return build.proximity.filter { build.canDump(it, Items.copper) }
        }

        override fun canInput(from: Building) = build.arcCheckAccept(from)
        override fun activeInputs(): List<Building> {
            return buildList {
                // 从链接源接收
                build.incoming.each { pos ->
                    val source = world.tile(pos).build
                    if (source != null) add(source)
                }
            }
        }
    }

    private class JunctionAdaptor(val build: Building) : BuildingAdaptor() {
        private fun otherSide(a: Building): Building? = a.relativeTo(build).toInt().let { build.nearby(it) }

        override fun getOutputs(): List<Building> = build.proximity.toList()
        override fun getOutputs(from: Building): List<Building> = listOfNotNull(otherSide(from))
        override fun canOutput(to: Building): Boolean {
            val from = otherSide(to) ?: return false
            return getWrapper(from).canOutput(build)
        }

        override fun canInput(from: Building, to: Building): Boolean = from == otherSide(to) && getWrapper(to).canInput(from)
        override fun canInput(from: Building): Boolean {
            val to = otherSide(from) ?: return false
            return getWrapper(to).canInput(build)
        }
    }

    private class MassDriverAdaptor(val build: MassDriver.MassDriverBuild) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> {
            return if (build.arcLinkValid()) {
                val target = world.build(build.link)
                if (target != null) listOf(target) else emptyList()
            } else {
                build.proximity.toList()
            }
        }

        override fun canInput(from: Building): Boolean = build.arcLinkValid() || (from as? MassDriver.MassDriverBuild)?.link == build.pos()
    }

    private class DirectionBridgeAdaptor(val build: DirectionBridge.DirectionBridgeBuild) : BuildingAdaptor() {
        override fun getOutputs(): List<Building> {
            val link = build.findLink()
            return if (link != null) listOf(link) else listOfNotNull(build.front())
        }

        override fun activeInputs(): List<Building> {
            return build.occupied.filterNotNull()
        }

        override fun canInput(from: Building): Boolean {
            if (from in build.occupied) return true
            val dir = from.relativeTo(build).toInt()
            return build.findLink() != null && build.occupied[dir] == null
        }
    }

    private class GenericCrafterAdaptor(val build: GenericCrafter.GenericCrafterBuild) : BuildingAdaptor(isEndPoint = true) {
        val block = build.block as GenericCrafter
        override fun getOutputs(): List<Building> {
            if (type == TransportType.ITEM && block.outputItems == null) return emptyList()
            if (type == TransportType.LIQUID && block.outputLiquids == null) return emptyList()
            return build.proximity.toList()
        }

        override fun canInput(from: Building): Boolean {
            when (type) {
                TransportType.ITEM -> {
                    if (!block.hasItems) return false
                    (from.block as? GenericCrafter)?.let { b ->
                        return b.outputItems != null && b.outputItems.any { block.itemFilter[it.item.id.toInt()] }
                    }
                    return build.block.itemFilter.any { it }
                }

                TransportType.LIQUID -> {
                    if (!block.hasLiquids) return false
                    (from.block as? GenericCrafter)?.let { b ->
                        return b.outputLiquids != null && b.outputLiquids.any { block.liquidFilter[it.liquid.id.toInt()] }
                    }
                    return build.block.liquidFilter.any { it }
                }

                else -> return false
            }
        }
    }

    private class SourceAdaptor(val build: Building) : BuildingAdaptor(isEndPoint = true) {
        override fun getOutputs(): List<Building> = build.proximity.toList()
    }

    private class VoidAdaptor: BuildingAdaptor() {
        override fun canInput(from: Building): Boolean = true
    }

    private class DefaultAdaptor(val build: Building) : BuildingAdaptor() {
        override fun canInput(from: Building): Boolean = when (type) {
            TransportType.ITEM -> build.block.hasItems && build.block.itemFilter.any { it }
            TransportType.LIQUID -> build.block.hasLiquids && build.block.liquidFilter.any { it }
        }
    }

    private object NoopAdaptor : BuildingAdaptor()
}
