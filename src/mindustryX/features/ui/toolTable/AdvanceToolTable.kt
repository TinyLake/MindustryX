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
        row().add("@advanceToolTable.warning").color(Color.yellow).colspan(2)

        row().add("@advanceToolTable.unitSection")
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            button(Items.copper.emoji() + "[acid]+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.tooltip("@advanceToolTable.fillCore")
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.tooltip("@advanceToolTable.clearCore")
            button(UnitTypes.gamma.emoji() + "[acid]+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val data = copyIO { Payload.write(UnitPayload(Vars.player.unit()), it) }
                val cloneUnit = Payload.read<UnitPayload>(data).unit
                cloneUnit.resetController()
                cloneUnit.set(Vars.player.x + Mathf.range(8f), Vars.player.y + Mathf.range(8f))
                cloneUnit.add()
            }.tooltip("@advanceToolTable.clone")
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { if (!Vars.player.dead()) Vars.player.unit().kill() }.tooltip("@advanceToolTable.suicide")
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.tooltip("@advanceToolTable.unitFactory")
        }

        row().add("@advanceToolTable.teamSection")
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            for (team in Team.baseTeams) {
                button(String.format("[#%s]%s", team.color, team.localized()), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }
            }
            button("[violet]+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip("@advanceToolTable.moreTeams")
        }

        row().add("@advanceToolTable.buildingSection")
        with(table().growX().get()) {
            defaults().pad(4f)
            button("@advanceToolTable.worldCreator", Styles.flatToggleMenut) { Settings.toggle("worldCreator") }
                .checked { LogicExt.worldCreator }.wrapLabel(false)
            button("@advanceToolTable.unlock", Styles.flatToggleMenut) {
                VarsX.allUnlocked.toggle()
            }.checked { VarsX.allUnlocked.value }.tooltip("@advanceToolTable.unlockTooltip").wrapLabel(false)
            button("@advanceToolTable.terrainSchematic", Styles.flatToggleMenut) { Settings.toggle("terrainSchematic") }
                .checked { LogicExt.terrainSchematic }.wrapLabel(false)
            button("@advanceToolTable.instantComplete", Styles.cleart) {
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

        row().add("@advanceToolTable.rulesSection")
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(40f)
            button("@advanceToolTable.infiniteFire", Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip("@advanceToolTable.infiniteFireTooltip").wrapLabel(false)
            button("@advanceToolTable.editor", Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.wrapLabel(false)
            button("@advanceToolTable.sandbox", Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.wrapLabel(false)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(Vars.iconMed)
        }

        row().add("@advanceToolTable.timeSection")
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