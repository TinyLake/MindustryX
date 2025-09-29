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
        row().add("警告：该页功能主要供单机作图使用").color(Color.yellow).colspan(2)

        row().add("单位：")
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            button(Items.copper.emoji() + "+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.tooltip("填满核心的所有资源")
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.tooltip("清空核心的所有资源")
            button(UnitTypes.gamma.emoji() + "+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val data = copyIO { Payload.write(UnitPayload(Vars.player.unit()), it) }
                val cloneUnit = Payload.read<UnitPayload>(data).unit
                cloneUnit.resetController()
                cloneUnit.set(Vars.player.x + Mathf.range(8f), Vars.player.y + Mathf.range(8f))
                cloneUnit.add()
            }.tooltip("克隆")
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { if (!Vars.player.dead()) Vars.player.unit().kill() }.tooltip("自杀")
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.tooltip("单位工厂-X")
        }

        row().add("队伍：")
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            for (team in Team.baseTeams) {
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }
            }
            button("+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip("更多队伍选择")
        }

        row().add("建筑：")
        with(table().growX().get()) {
            defaults().pad(4f)
            button("创世神", Styles.flatToggleMenut) { LogicExt.worldCreator0.toggle() }
                .checked { LogicExt.worldCreator }.wrapLabel(false)
            button("解禁", Styles.flatToggleMenut) {
                VarsX.allUnlocked.toggle()
            }.checked { VarsX.allUnlocked.value }.tooltip("显示并允许建造所有物品").wrapLabel(false)
            button("地形蓝图", Styles.flatToggleMenut) { LogicExt.terrainSchematic0.toggle() }
                .checked { LogicExt.terrainSchematic }.wrapLabel(false)
            button("瞬间完成", Styles.cleart) {
                Vars.player.unit()?.apply {
                    if (!canBuild()) {
                        UIExt.announce("[red]当前单位不可建筑")
                        return@apply
                    }
                    val bak = updateBuilding
                    updateBuilding = true
                    repeat(10000) { updateBuildLogic() }
                    updateBuilding = bak
                }
            }.wrapLabel(false).disabled { Vars.net.client() }
        }

        row().add("规则：")
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(40f)
            button("无限火力", Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip("开关自己队的无限火力").wrapLabel(false)
            button("编辑器", Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.wrapLabel(false)
            button("沙盒", Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.wrapLabel(false)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(Vars.iconMed)
        }

        row().add("沙漏：")
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