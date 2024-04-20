package mindustryX.features

import arc.Core
import arc.func.Func2
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.scene.event.Touchable
import arc.scene.ui.layout.Table
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Strings
import arc.util.Tmp
import mindustry.Vars
import mindustry.Vars.state
import mindustry.gen.Building
import mindustry.graphics.Drawf
import mindustry.type.Item
import mindustry.ui.Styles
import mindustry.world.Block
import mindustry.world.blocks.distribution.*
import mindustry.world.blocks.distribution.ArmoredConveyor.ArmoredConveyorBuild
import mindustry.world.blocks.distribution.Conveyor.ConveyorBuild
import mindustry.world.blocks.distribution.DirectionBridge.DirectionBridgeBuild
import mindustry.world.blocks.distribution.Duct.DuctBuild
import mindustry.world.blocks.distribution.DuctRouter.DuctRouterBuild
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild
import mindustry.world.blocks.distribution.Junction.JunctionBuild
import mindustry.world.blocks.distribution.MassDriver.MassDriverBuild
import mindustry.world.blocks.distribution.OverflowDuct.OverflowDuctBuild
import mindustry.world.blocks.distribution.OverflowGate.OverflowGateBuild
import mindustry.world.blocks.distribution.Router.RouterBuild
import mindustry.world.blocks.distribution.Sorter.SorterBuild
import mindustry.world.blocks.distribution.StackConveyor.StackConveyorBuild
import mindustry.world.blocks.liquid.LiquidBridge.LiquidBridgeBuild
import mindustry.world.blocks.production.Drill
import mindustry.world.blocks.production.Drill.DrillBuild
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.blocks.storage.Unloader
import mindustry.world.meta.BlockGroup

class MapAnalyzer {

    fun mapAnalyzer() {
        detailTransporter()
    }

    private fun detailTransporter() {
        ct.visible = ct.visible && state.isPlaying
        ctTable.clear()
        if (!Vars.control.input.arcScanMode) {
            ct.visible = false
            return
        }

        //check tile being hovered over
        val hoverTile = Vars.world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y)
        if (hoverTile?.build == null || !hoverTile.build.isDiscovered(Vars.player.team())) {
            return
        }

        val segment = Segment()

        segment.path.clear()
        travelPath(segment, Point(hoverTile.build, null), this::getPrevious)
        drawPath(segment.path, false)

        /*
        path.clear();
        travelPath(new Point(hoverTile.build, null), arcScanMode::getNext);
        drawPath(true);

        //暂时放弃下游 等有空先
        */
        ct.setPosition(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())
        ct.visible = true
        ctTable.table { ctt: Table ->
            for (itemstack in segment.totalProduction()) {
                ctt.add(itemstack.key.localizedName + ":" + Strings.fixed(itemstack.value, 2) + "/s")
                ctt.row()
            }
            /*
            ctt.row()
            ctt.add("debug below").row()
            ctt.add("=================").row()
            ctt.add("total point:" + segment.path.size).row()
            for (building in segment.inputs) {
                ctt.add(building.block.localizedName + ":(" + building.x + "," + building.y + ")")
                ctt.row()
            }
            */
        }
    }

    fun travelPath(segment: Segment, point: Point, getNext: Func2<Segment, Point, Seq<Point>>) {
        if (point.build == null) return
        if (!point.trans) return

        val same =
            segment.path.find { other: Point -> point.build === other.build && (other.from == null || point.from!!.build === other.from!!.build) }
        if (same != null) {
            if (point.conduit >= same.conduit) return
            else segment.path.replace(same, point)
        } else segment.path.add(point)

        getNext[segment, point].each { p: Point -> travelPath(segment, p, getNext) }
    }

    fun getPrevious(segment: Segment, point: Point): Seq<Point> {
        val build = point.build ?: return Seq()
        val previous = Seq<Point>()

        if (build is MassDriverBuild) {
            //质驱
            //暂时搞不定
        } else if (build is ItemBridgeBuild && build !is LiquidBridgeBuild) {
            //桥
            build.incoming.each { pos: Int ->
                previous.add(
                    Point(
                        Vars.world.tile(pos).build,
                        point
                    )
                )
            }
        } else if (build is DirectionBridgeBuild) {
            //导管桥
            for (b in build.occupied) {
                if (b != null) {
                    previous.add(Point(b, point))
                }
            }
        }
        for (b in build.proximity) {
            val from = Point(b, b.relativeTo(build), if (b.block.instantTransfer) point.conduit + 1 else 0, point)
            if (canInput(point, b, true) && canOutput(from, build, false)) {
                previous.add(from)
            } else if (canOutput(from, build, false)) {
                from.trans = false
                previous.add(from)
            }
            if (!segment.inputs.contains{ it.tile == b.tile } && canInput(point, b, true)) {
                if (b.block is GenericCrafter) {
                    segment.inputs.add(b)
                }
                if (b.block is Drill) {
                    segment.inputs.add(b)
                }
            }
        }
        return previous
    }


    /*
    public static Seq<Point> getNext(Point point){
        Building build = point.build;
        if(build == null) return new Seq<>();
        Seq<Point> next = new Seq<>();
        //质驱
        if(build instanceof MassDriver.MassDriverBuild massDriverBuild){
            if(massDriverBuild.arcLinkValid()){
                next.add(new Point(world.build(massDriverBuild.link), point));
            }
        }//桥
        else if(build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild && !(build instanceof LiquidBridge.LiquidBridgeBuild)){
            if(itemBridgeBuild.arcLinkValid()){
                next.add(new Point(world.build(itemBridgeBuild.link), point));
            }
        }//导管桥
        else if(build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild){
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            if(link != null){
                next.add(new Point(link, point));
            }
        }

        for(Building b : build.proximity){
            Point to = new Point(b, build.relativeTo(b), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if(canInput(to, build, false) && canOutput(point, b, true)){
                next.add(to);
            }else if(canInput(to, build, false)){
                to.trans = false;
                next.add(to);
            }
        }
        return next;
    }
    */
    fun canInput(point: Point, from: Building?, active: Boolean): Boolean {
        val build = point.build
        if (build == null || from == null) return false
        if (from.block.instantTransfer && point.conduit > 2) return false
        //装甲传送带
        if (build is ArmoredConveyorBuild) {
            return from !== build.front() && (from is ConveyorBuild || from === build.back())
        } //装甲导管
        else if (build is DuctBuild && (build.block as Duct).armored) {
            return from !== build.front() && (from.block.isDuct || from === build.back())
        } //传送带和导管
        else if (build is ConveyorBuild || build is DuctBuild) {
            return from !== build.front()
        } //塑钢带
        else if (build is StackConveyorBuild) {
            return when (build.state) {
                2 -> from === build.back() && from is StackConveyorBuild
                1 -> from !== build.front()
                else -> from is StackConveyorBuild
            }
        } //交叉器
        else if (build is JunctionBuild) {
            return point.facing.toInt() == -1 || from.relativeTo(build) == point.facing
        } //分类
        else if (build is SorterBuild) {
            return !active || build.relativeTo(from) != point.facing && (build.sortItem != null || (from.relativeTo(
                build
            ) == point.facing) == (build.block as Sorter).invert)
        } //溢流
        else if (build is OverflowGateBuild) {
            return !active || build.relativeTo(from) != point.facing
        } //导管路由器与导管溢流
        else if (build is DuctRouterBuild || build is OverflowDuctBuild) {
            return from === build.back()
        } //桥
        else if (build is ItemBridgeBuild) {
            return build.arcCheckAccept(from)
        } //导管桥
        else if (build is DirectionBridgeBuild) {
            return build.arcCheckAccept(from)
        } else if (build is RouterBuild) {
            return true
        } else if (canAccept(build.block)) {
            point.trans = false
            return true
        }
        return false
    }

    private fun canAccept(block: Block): Boolean {
        if (block.group == BlockGroup.transportation) return true
        for (item in Vars.content.items()) {
            if (block.consumesItem(item) || block.itemCapacity > 0) {
                return true
            }
        }
        return false
    }

    fun canOutput(point: Point, to: Building?, active: Boolean): Boolean {
        val build = point.build
        if (build == null || to == null) return false
        if (to.block.instantTransfer && point.conduit > 2) return false
        //传送带和导管
        if (build is ConveyorBuild || build is DuctBuild) {
            return to === build.front()
        } //塑钢带
        else if (build is StackConveyorBuild) {
            if (build.state == 2 && (build.block as StackConveyor).outputRouter) {
                return to !== build.back()
            }
            return to === build.front()
        } //交叉器
        else if (build is JunctionBuild) {
            return point.facing.toInt() == -1 || build.relativeTo(to) == point.facing
        } //分类
        else if (build is SorterBuild) {
            return !active || to.relativeTo(build) != point.facing && (build.sortItem != null || (build.relativeTo(to) == point.facing) == (build.block as Sorter).invert)
        } //溢流
        else if (build is OverflowGateBuild) {
            return !active || to.relativeTo(build) != point.facing
        } //导管路由器与导管溢流
        else if (build is DuctRouterBuild || build is OverflowDuctBuild) {
            return to !== build.back()
        } //桥
        else if (build is ItemBridgeBuild) {
            return build.arcCheckDump(to)
        } //导管桥
        else if(build is DirectionBridge.DirectionBridgeBuild) {
            val link: DirectionBridge.DirectionBridgeBuild? = build.findLink()
            return link == null && build.relativeTo(to).toInt() == build.rotation
        }else if(build is Router.RouterBuild || build is Unloader.UnloaderBuild){
            return true;
        }else if(build is GenericCrafter.GenericCrafterBuild){
            point.trans = false;
            return true;
        }
        return false
    }

    fun drawPath(path: Seq<Point>, forward: Boolean) {
        val mainColor = if (forward) Color.valueOf("80ff00") else Color.valueOf("ff8000")
        val highlightColor = if (forward) Color.valueOf("00cc00") else Color.red
        path.each { p: Point ->
            if (p.from != null && p.trans) {
                val x1 = p.build!!.tile.drawx()
                val y1 = p.build!!.tile.drawy()
                val x2 = p.from!!.build!!.tile.drawx()
                val y2 = p.from!!.build!!.tile.drawy()

                Draw.color(mainColor)
                Draw.color(Tmp.c1.set(mainColor).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f))
                Lines.stroke(1.5f)
                Lines.line(x1, y1, x2, y2)
            } else {
                Drawf.selected(p.build, Tmp.c1.set(highlightColor).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f))
            }
            Draw.reset()
        }
        path.each { p: Point ->
            if (p.from != null && p.trans) {
                val x1 = p.build!!.tile.drawx()
                val y1 = p.build!!.tile.drawy()
                val x2 = p.from!!.build!!.tile.drawx()
                val y2 = p.from!!.build!!.tile.drawy()
                val dst = Mathf.dst(x1, y1, x2, y2)

                Draw.color(highlightColor)
                Fill.circle(x1, y1, 1.8f)

                if (dst > Vars.tilesize) {
                    Draw.color(highlightColor)
                    if (forward) {
                        Drawf.simpleArrow(x2, y2, x1, y1, dst / 2, 3f)
                    } else {
                        Drawf.simpleArrow(x1, y1, x2, y2, dst / 2, 3f)
                    }
                }
            }
            Draw.reset()
        }
    }


    companion object {
        val ct: Table = Table(Styles.none)
        val ctTable: Table = Table()

        init {
            ct.touchable = Touchable.disabled;
            ct.visible = false
            ct.add(ctTable).margin(8f)
            ct.pack()
            ct.update { ct.visible = ct.visible && state.isPlaying }
            Core.scene.add(ct);
        }

        class Point {
            var build: Building?
            var facing: Byte = -1
            var conduit: Int = 0

            //用于记录端点方块
            var trans: Boolean = true

            var from: Point?

            constructor(build: Building?, from: Point?) {
                this.build = build
                this.from = from
            }

            constructor(build: Building?, facing: Byte, conduit: Int, from: Point?) {
                this.build = build
                this.facing = facing
                this.conduit = conduit
                this.from = from
            }
        }

        class Segment {
            var path: Seq<Point> = Seq()

            var inputs: Seq<Building> = Seq()

            fun totalProduction(): ObjectMap<Item, Float> {
                val totalProduction = ObjectMap<Item, Float>()
                inputs.forEach { building ->
                    if (building.block is GenericCrafter) {
                        val genericCrafter: GenericCrafter = building.block as GenericCrafter
                        if (genericCrafter.outputItem != null) {
                            for (outputItem in genericCrafter.outputItems) {
                                val item = outputItem.item
                                val production = outputItem.amount / (genericCrafter.craftTime / 60)
                                if (totalProduction.containsKey(item)) {
                                    totalProduction.put(item, totalProduction[item] + production)
                                } else {
                                    totalProduction.put(item, production)
                                }
                            }
                        }
                    }
                    if (building is DrillBuild) {
                        val drillBuild: DrillBuild = building
                        val drill: Drill = building.block as Drill
                        if (drillBuild.dominantItem != null) {
                            val item = drillBuild.dominantItem
                            val speed = 60f / drill.getDrillTime(item) * drill.returnCount
                            val production = if (true) { //TODO
                                speed * drill.liquidBoostIntensity * drill.liquidBoostIntensity
                            } else {
                                speed
                            }

                            if (totalProduction.containsKey(item)) {
                                totalProduction.put(item, totalProduction[item] + production)
                            } else {
                                totalProduction.put(item, production)
                            }
                        }
                    }
                }
                return totalProduction
            }

            /*
            for (outputLiquid in genericCrafter.outputLiquids) {
                            if (totalProduction.containsKey(outputLiquid.liquid)) {
                                totalProduction.put(
                                    outputLiquid.liquid,
                                    totalProduction[outputLiquid.liquid] + outputLiquid.amount / (genericCrafter.craftTime / 60)
                                )
                            } else {
                                totalProduction.put(
                                    outputLiquid.liquid,
                                    outputLiquid.amount / (genericCrafter.craftTime / 60)
                                )
                            }
                        }
            * */
        }

    }
}
