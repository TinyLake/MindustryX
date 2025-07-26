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
        row().add(Core.bundle.get("advanceToolTable.warning")).color(Color.yellow).colspan(2)

        row().add(Core.bundle.get("advanceToolTable.unitSection"))
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            button(Items.copper.emoji() + "[acid]+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.tooltip(Core.bundle.get("advanceToolTable.fillCore"))
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.tooltip(Core.bundle.get("advanceToolTable.clearCore"))
            button(UnitTypes.gamma.emoji() + "[acid]+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val data = copyIO { Payload.write(UnitPayload(Vars.player.unit()), it) }
                val cloneUnit = Payload.read<UnitPayload>(data).unit
                cloneUnit.resetController()
                cloneUnit.set(Vars.player.x + Mathf.range(8f), Vars.player.y + Mathf.range(8f))
                cloneUnit.add()
            }.tooltip(Core.bundle.get("advanceToolTable.clone"))
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { if (!Vars.player.dead()) Vars.player.unit().kill() }.tooltip(Core.bundle.get("advanceToolTable.suicide"))
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.tooltip(Core.bundle.get("advanceToolTable.unitFactory"))
        }

        row().add(Core.bundle.get("advanceToolTable.teamSection"))
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            for (team in Team.baseTeams) {
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }
            }
            button("[violet]+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip(Core.bundle.get("advanceToolTable.moreTeams"))
        }

        row().add(Core.bundle.get("advanceToolTable.buildingSection"))
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Core.bundle.get("advanceToolTable.worldCreator"), Styles.flatToggleMenut) { Settings.toggle("worldCreator") }
                .checked { LogicExt.worldCreator }.wrapLabel(false)
            button(Core.bundle.get("advanceToolTable.unlock"), Styles.flatToggleMenut) {
                VarsX.allUnlocked.toggle()
            }.checked { VarsX.allUnlocked.value }.tooltip(Core.bundle.get("advanceToolTable.unlockTooltip")).wrapLabel(false)
            button(Core.bundle.get("advanceToolTable.terrainSchematic"), Styles.flatToggleMenut) { Settings.toggle("terrainSchematic") }
                .checked { LogicExt.terrainSchematic }.wrapLabel(false)
            button(Core.bundle.get("advanceToolTable.instantComplete"), Styles.cleart) {
                Vars.player.unit()?.apply {
                    if (!canBuild()) {
                        UIExt.announce(Core.bundle.get("advanceToolTable.cannotBuild"))
                        return@apply
                    }
                    val bak = updateBuilding
                    updateBuilding = true
                    repeat(10000) { updateBuildLogic() }
                    updateBuilding = bak
                }
            }.wrapLabel(false).disabled { Vars.net.client() }
        }

        row().add(Core.bundle.get("advanceToolTable.rulesSection"))
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(40f)
            button(Core.bundle.get("advanceToolTable.infiniteFire"), Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip(Core.bundle.get("advanceToolTable.infiniteFireTooltip")).wrapLabel(false)
            button(Core.bundle.get("advanceToolTable.editor"), Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.wrapLabel(false)
            button(Core.bundle.get("advanceToolTable.sandbox"), Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.wrapLabel(false)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(Vars.iconMed)
        }

        row().add(Core.bundle.get("advanceToolTable.timeSection"))
        table(TimeControl::draw)
    }

    private inline fun copyIO(write: (Writes) -> Unit): Reads {
        val data = ByteArrayOutputStream().use {
            write(Writes.get(DataOutputStream(it)))
            it.toByteArray()
        }
        return Reads.get(DataInputStream(ByteArrayInputStream(data)))
    }
}