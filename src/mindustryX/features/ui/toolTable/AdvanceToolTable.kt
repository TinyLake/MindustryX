package mindustryX.features.ui.toolTable

import arc.graphics.Color
import arc.math.Mathf
import arc.struct.Seq
import mindustry.Vars
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.editor.MapInfoDialog
import mindustry.game.Team
import mindustry.gen.Icon
import mindustry.gen.Iconc
import mindustry.gen.Payloadc
import mindustry.gen.Unit
import mindustry.ui.Styles
import mindustry.ui.dialogs.CustomRulesDialog
import mindustry.world.blocks.payloads.Payload
import mindustryX.features.LogicExt
import mindustryX.features.Settings
import mindustryX.features.TimeControl
import mindustryX.features.UIExt

//move from mindustry.arcModule.ui.AdvanceToolTable
class AdvanceToolTable : ToolTableBase(Iconc.wrench.toString()) {
    val factoryDialog: ArcUnitFactoryDialog = ArcUnitFactoryDialog()
    private val rulesDialog = CustomRulesDialog()
    private val mapInfoDialog: MapInfoDialog = MapInfoDialog()

    init {
        row().add("警告：该页功能主要供单机使用").color(Color.yellow).colspan(2)

        row().add("单位：")
        with(table().get()) {
            button(Items.copper.emoji() + "[acid]+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.width(40f).tooltip("[acid]填满核心的所有资源")
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.width(40f).tooltip("[acid]清空核心的所有资源")
            button(UnitTypes.gamma.emoji() + "[acid]+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val cloneUnit = cloneExactUnit(Vars.player.unit())
                cloneUnit[Vars.player.x + Mathf.range(8f)] = Vars.player.y + Mathf.range(8f)
                cloneUnit.add()
            }.width(40f).tooltip("[acid]克隆")
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { Vars.player.unit().kill() }.width(40f).tooltip("[red]自杀")
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.width(40f).tooltip("[acid]单位工厂-ARC")
        }


        row().add("队伍：")
        with(table().get()) {
            for (team in Team.baseTeams) {
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }.size(30f, 30f)
            }
            button("[violet]+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip("[acid]更多队伍选择").size(30f, 30f)
        }

        row().add("建筑：")
        with(table().get()) {
            button("创世神", Styles.flatToggleMenut) { Settings.toggle("worldCreator") }
                .checked { LogicExt.worldCreator }.size(70f, 30f)
            button("解禁", Styles.flatToggleMenut) {
                Settings.toggle("allUnlocked")
            }.checked { LogicExt.allUnlocked }
                .tooltip("[acid]显示并允许建造所有物品").size(50f, 30f)
            button("地形蓝图", Styles.flatToggleMenut) { Settings.toggle("terrainSchematic") }
                .checked { LogicExt.terrainSchematic }.size(72f, 30f)
        }

        row().add("规则：")
        with(table().get()) {
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(40f)
            button("无限火力", Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip("[acid]开关自己队的无限火力").size(90f, 30f)
            button("编辑器", Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.size(70f, 30f)
            button("沙盒", Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.size(50f, 30f)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(40f)
        }

        row().add("沙漏：")
        table(TimeControl::draw)
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
}