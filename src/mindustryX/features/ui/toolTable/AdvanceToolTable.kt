package mindustryX.features.ui.toolTable

import arc.graphics.Color
import arc.math.Mathf
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.editor.MapInfoDialog
import mindustry.game.Team
import mindustry.gen.Icon
import mindustry.gen.Iconc
import mindustry.ui.Styles
import mindustry.ui.dialogs.CustomRulesDialog
import mindustry.world.blocks.payloads.Payload
import mindustry.world.blocks.payloads.UnitPayload
import mindustryX.VarsX
import mindustryX.features.*
import mindustryX.features.ui.UnitFactoryDialog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

//move from mindustry.arcModule.ui.AdvanceToolTable
class AdvanceToolTable : Table() {
    val factoryDialog: UnitFactoryDialog = UnitFactoryDialog()
    private val rulesDialog = CustomRulesDialog()
    private val mapInfoDialog: MapInfoDialog = MapInfoDialog()

    init {
        background = Styles.black6
        row().add(mindustryX.bundles.UiTexts.ui("single_player_map_tools_only")).color(Color.yellow).colspan(2) // 原文本:警告：该页功能主要供单机作图使用

        row().add(mindustryX.bundles.UiTexts.ui("unit")) // 原文本:单位：
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            button(Items.copper.emoji() + "+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.tooltip(mindustryX.bundles.UiTexts.ui("fill_core_resources")) // 原文本:填满核心的所有资源
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.tooltip(mindustryX.bundles.UiTexts.ui("clear_all_core_resources")) // 原文本:清空核心的所有资源
            button(UnitTypes.gamma.emoji() + "+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val data = copyIO { Payload.write(UnitPayload(Vars.player.unit()), it) }
                val cloneUnit = Payload.read<UnitPayload>(data).unit
                cloneUnit.resetController()
                cloneUnit.set(Vars.player.x + Mathf.range(8f), Vars.player.y + Mathf.range(8f))
                cloneUnit.add()
            }.tooltip(mindustryX.bundles.UiTexts.ui("clone")) // 原文本:克隆
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { if (!Vars.player.dead()) Vars.player.unit().kill() }.tooltip(mindustryX.bundles.UiTexts.ui("self_destruct")) // 原文本:自杀
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.tooltip(mindustryX.bundles.UiTexts.ui("unit_factory_x")) // 原文本:单位工厂-X
        }

        row().add(mindustryX.bundles.UiTexts.ui("team")) // 原文本:队伍：
        with(table().growX().get()) {
            defaults().pad(4f)
            var count = 0
            for (team in Team.baseTeams) {
                if (count > 0 && count % 4 == 0) row()
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .minWidth(88f).height(Vars.iconMed)
                    .checked { Vars.player.team() === team }
                count++
            }
            if (count > 0 && count % 4 == 0) row()
            button("+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .minWidth(88f).height(Vars.iconMed)
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip(mindustryX.bundles.UiTexts.ui("more_teams")) // 原文本:更多队伍选择
        }

        row().add(mindustryX.bundles.UiTexts.ui("buildings")) // 原文本:建筑：
        with(table().growX().get()) {
            defaults().pad(4f)
            button(mindustryX.bundles.UiTexts.ui("god_mode"), Styles.flatToggleMenut) { LogicExt.worldCreator0.toggle() } // 原文本:创世神
                .minWidth(120f).height(Vars.iconMed)
                .checked { LogicExt.worldCreator }.wrapLabel(true)
            button(mindustryX.bundles.UiTexts.ui("unlock"), Styles.flatToggleMenut) { // 原文本:解禁
                VarsX.allUnlocked.toggle()
            }.minWidth(120f).height(Vars.iconMed).checked { VarsX.allUnlocked.value }.tooltip(mindustryX.bundles.UiTexts.ui("unlock_and_allow_all_blocks")).wrapLabel(true).row() // 原文本:显示并允许建造所有物品
            button(mindustryX.bundles.UiTexts.ui("terrain_blueprint"), Styles.flatToggleMenut) { LogicExt.terrainSchematic0.toggle() } // 原文本:地形蓝图
                .minWidth(120f).height(Vars.iconMed)
                .checked { LogicExt.terrainSchematic }.wrapLabel(true)
            button(mindustryX.bundles.UiTexts.ui("instant"), Styles.cleart) { // 原文本:瞬间完成
                Vars.player.unit()?.apply {
                    if (!canBuild()) {
                        UIExt.announce(mindustryX.bundles.UiTexts.ui("current_unit_cannot_build")) // 原文本:[red]当前单位不可建筑
                        return@apply
                    }
                    val bak = updateBuilding
                    updateBuilding = true
                    repeat(10000) { updateBuildLogic() }
                    updateBuilding = bak
                }
            }.minWidth(120f).height(Vars.iconMed).disabled { Vars.net.client() }.wrapLabel(true)
        }

        row().add(mindustryX.bundles.UiTexts.ui("rules")) // 原文本:规则：
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(40f)
            button(mindustryX.bundles.UiTexts.ui("cheat"), Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat } // 原文本:无限火力
                .checked { Vars.player.team().rules().cheat }.tooltip(mindustryX.bundles.UiTexts.ui("toggle_your_team_s_cheat")).wrapLabel(false) // 原文本:开关自己队的无限火力
            button(mindustryX.bundles.UiTexts.ui("editor"), Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor } // 原文本:编辑器
                .checked { Vars.state.rules.editor }.wrapLabel(false)
            button(mindustryX.bundles.UiTexts.ui("sandbox"), Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources } // 原文本:沙盒
                .checked { Vars.state.rules.infiniteResources }.wrapLabel(false)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(Vars.iconMed)
        }

        row().add(mindustryX.bundles.UiTexts.ui("hourglass")) // 原文本:沙漏：
        table(TimeControl::draw)
    }

    private inline fun copyIO(write: (Writes) -> Unit): Reads {
        val data = ByteArrayOutputStream().use {
            write(Writes(DataOutputStream(it)))
            it.toByteArray()
        }
        return Reads(DataInputStream(ByteArrayInputStream(data)))
    }
}
