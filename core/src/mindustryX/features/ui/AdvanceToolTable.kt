package mindustryX.features.ui

import arc.Events
import arc.math.Mathf
import arc.math.geom.Vec2
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.Reflect
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Icon
import mindustry.gen.Payloadc
import mindustry.gen.Tex
import mindustry.gen.Unit
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustry.world.Tile
import mindustry.world.blocks.payloads.Payload
import mindustryX.features.TimeControl
import mindustryX.features.UIExt
import kotlin.math.abs
import kotlin.math.min

//move from mindustry.arcModule.ui.AdvanceToolTable
class AdvanceToolTable : Table() {
    companion object {
        @JvmField
        var worldCreator: Boolean = false

        @JvmField
        var forcePlacement: Boolean = false

        @JvmField
        var allBlocksReveal: Boolean = false
    }

    val factoryDialog: ArcUnitFactoryDialog = ArcUnitFactoryDialog()
    val tileCopyDialog: TileCopyDialog = TileCopyDialog()

    init {
        Events.on(EventType.ResetEvent::class.java) { _ ->
            if (!Vars.state.rules.editor) {
                worldCreator = false
                forcePlacement = false
                allBlocksReveal = false
            }
        }

        val showEntities = SimpleCollapser().table {
            background = Tex.buttonEdge3
            button(Items.copper.emoji() + "[acid]+", Styles.cleart) {
                for (item in Vars.content.items()) Vars.player.core().items[item] = Vars.player.core().storageCapacity
            }.width(40f).tooltip("[acid]填满核心的所有资源")
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                for (item in Vars.content.items()) Vars.player.core().items[item] = 0
            }.width(40f).tooltip("[acid]清空核心的所有资源")
            button(UnitTypes.gamma.emoji() + "[acid]+", Styles.cleart) {
                val cloneUnit = cloneExactUnit(Vars.player.unit())
                cloneUnit[Vars.player.x + Mathf.range(8f)] = Vars.player.y + Mathf.range(8f)
                cloneUnit.add()
            }.width(40f).tooltip("[acid]克隆")
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { Vars.player.unit().kill() }.width(40f).tooltip("[red]自杀")
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.width(40f).tooltip("[acid]单位工厂-ARC")
        }.also { add(it).left().row() }
        val showTimeControl = SimpleCollapser().table {
            background = Tex.buttonEdge3
            TimeControl.draw(this)
        }.also { add(it).left().row() }
        val showCreator = SimpleCollapser().table {
            background = Tex.buttonEdge3
            button("创世神", Styles.flatToggleMenut) { worldCreator = !worldCreator }
                .checked { worldCreator }.size(70f, 30f)
            button("强制放置", Styles.flatToggleMenut) { forcePlacement = !forcePlacement }
                .checked { forcePlacement }.size(70f, 30f)
            button("解禁", Styles.flatToggleMenut) {
                allBlocksReveal = !allBlocksReveal
                Reflect.invoke<Any>(Vars.ui.hudfrag.blockfrag, "rebuild")
            }.checked { allBlocksReveal }
                .tooltip("[acid]显示并允许建造所有物品").size(50f, 30f)
            button("[orange]复制地形", Styles.flatToggleMenut) { tileCopyDialog.show() }
                .tooltip("[acid]复制特定地形").size(70f, 30f).checked { false }
        }.also { add(it).left().row() }
        val showTeamChange = SimpleCollapser().table {
            background = Tex.buttonEdge3
            add("队伍：").left()
            for (team in Team.baseTeams) {
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }.size(30f, 30f)
            }
            button("[violet]+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip("[acid]更多队伍选择").size(30f, 30f)
        }.also { add(it).left().row() }
        val showGameMode = SimpleCollapser().table {
            background = Tex.buttonEdge3
            add("规则：").left()
            button("无限火力", Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip("[acid]开关自己队的无限火力").size(90f, 30f)
            button("编辑器", Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.size(70f, 30f)
            button("沙盒", Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.size(50f, 30f)
        }.also { add(it).left().row() }
        table(Tex.buttonEdge3) { row: Table ->
            val showButtons = SimpleCollapser().table {
                button(Items.copper.emoji() + UnitTypes.gamma.emoji(), Styles.cleart, showEntities::toggle).width(50f)
                button(Blocks.worldProcessor.emoji(), Styles.cleart, showCreator::toggle).width(50f)
                button(Blocks.overdriveProjector.emoji(), Styles.cleart, showTimeControl::toggle).width(50f)
                button("规则", Styles.cleart, showGameMode::toggle).width(50f)
                button("队伍", Styles.cleart, showTeamChange::toggle).width(50f)
            }
            row.button("", Styles.cleart, showButtons::toggle).update { it.setText(if (showButtons.collapsed) "[cyan]工具箱" else "[red]工具箱") }.width(70f)
            row.add(showButtons)
        }.left()
    }

    private fun cloneExactUnit(unit: Unit): Unit {
        val reUnit = unit.type.create(unit.team)
        reUnit.health = unit.health
        reUnit.shield = unit.shield
        reUnit.stack = unit.stack

        for (effects in Vars.content.statusEffects()) {
            if (unit.getDuration(effects) > 0f) reUnit.apply(effects, unit.getDuration(effects))
        }

        if (unit is Payloadc && reUnit is Payloadc) {
            unit.payloads().each { load: Payload? -> reUnit.addPayload(load) }
        }
        return reUnit
    }

    class TileCopyDialog : BaseDialog("地块复制器") {
        private val fromA = Vec2(0f, 0f)
        private val fromB = Vec2(0f, 0f)
        private val toA = Vec2(0f, 0f)

        init {
            cont.table { tt: Table ->
                tt.add("复制区角A：")
                UIExt.buildPositionRow(tt, fromA)
            }.row()
            cont.table { tt: Table ->
                tt.add("复制区角B：")
                UIExt.buildPositionRow(tt, fromB)
            }.row()
            cont.table { tt: Table ->
                tt.add("粘贴区左下坐标：")
                UIExt.buildPositionRow(tt, toA)
            }.row()
            cont.button("复制！") {
                Vars.ui.showInfoFade("复制蓝图中...\n[orange]测试中功能，请等待后续完善")
                val left2 = Vec2(min(fromA.x, fromB.x), min(fromA.y, fromB.y))
                var x = 0
                while (x <= abs(fromA.x - fromB.x)) {
                    var y = 0
                    while (y <= abs(fromA.y - fromB.y)) {
                        val copyTile = Vars.world.tile((left2.x + x).toInt(), (left2.y + y).toInt())
                        val thisTile = Vars.world.tile((toA.x + x).toInt(), (toA.y + y).toInt())
                        Tile.setFloor(thisTile, copyTile.floor(), copyTile.overlay())
                        if (copyTile.build == null) thisTile.setBlock(copyTile.block())
                        else thisTile.setBlock(copyTile.block(), copyTile.build.team, copyTile.build.rotation)

                        y++
                    }
                    x++
                }
            }.height(50f).fillX()
            addCloseButton()
        }
    }
}