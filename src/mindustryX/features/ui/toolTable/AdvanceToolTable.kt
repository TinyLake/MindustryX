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
import mindustryX.features.LogicExt
import mindustryX.features.TimeControl
import mindustryX.features.UIExt
import mindustryX.features.UIExt.i
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
        row().add(i("ui.simple.single-player-map-tools-only")).color(Color.yellow).colspan(2)

        row().add(i("ui.simple.unit"))
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).pad(4f)
            button(Items.copper.emoji() + "+", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                for (item in Vars.content.items()) core.items[item] = core.storageCapacity
            }.tooltip(i("ui.simple.fill-core-resources"))
            button(Items.copper.emoji() + "[red]-", Styles.cleart) {
                val core = Vars.player.core() ?: return@button
                core.items.clear()
            }.tooltip(i("ui.simple.clear-all-core-resources"))
            button(UnitTypes.gamma.emoji() + "+", Styles.cleart) {
                if (Vars.player.dead()) return@button
                val data = copyIO { Payload.write(UnitPayload(Vars.player.unit()), it) }
                val cloneUnit = Payload.read<UnitPayload>(data).unit
                cloneUnit.resetController()
                cloneUnit.set(Vars.player.x + Mathf.range(8f), Vars.player.y + Mathf.range(8f))
                cloneUnit.add()
            }.tooltip(i("ui.simple.clone"))
            button(UnitTypes.gamma.emoji() + "[red]×", Styles.cleart) { if (!Vars.player.dead()) Vars.player.unit().kill() }.tooltip(i("ui.simple.self-destruct"))
            button(Icon.waves, Styles.clearNonei) { factoryDialog.show() }.tooltip(i("ui.simple.unit-factory-x"))
        }

        row().add(i("ui.simple.team"))
        with(table().growX().get()) {
            defaults().size(Vars.iconMed).maxWidth(120f).pad(4f).padRight(8f)
            for (team in Team.baseTeams) {
                button(team.localized(), Styles.flatToggleMenut) { Vars.player.team(team) }
                    .checked { Vars.player.team() === team }.get().apply {
                        label.apply { setWrap(false); setColor(team.color) }
                    }
            }
            button("+", Styles.flatToggleMenut) { UIExt.teamSelect.pickOne({ team: Team? -> Vars.player.team(team) }, Vars.player.team()) }
                .checked { !Seq.with(*Team.baseTeams).contains(Vars.player.team()) }
                .tooltip(i("ui.simple.more-teams"))
        }

        row().add(i("ui.simple.buildings"))
        with(table().growX().get()) {
            defaults().pad(4f).padRight(8f)
            button(i("settingV2.worldCreator.name"), Styles.flatToggleMenut) { LogicExt.worldCreator0.toggle() }
                .checked { LogicExt.worldCreator }.wrapLabel(false)
            button(i("ui.simple.unlock"), Styles.flatToggleMenut) {
                VarsX.allUnlocked.toggle()
            }.checked { VarsX.allUnlocked.value }.tooltip(i("ui.simple.unlock-and-allow-all-blocks")).wrapLabel(false)
            button(i("settingV2.terrainSchematic.name"), Styles.flatToggleMenut) { LogicExt.terrainSchematic0.toggle() }
                .checked { LogicExt.terrainSchematic }.wrapLabel(false)
            button(i("ui.simple.instant"), Styles.cleart) {
                Vars.player.unit()?.apply {
                    if (!canBuild()) {
                        UIExt.announce(i("ui.simple.red-current-unit-cannot-build"))
                        return@apply
                    }
                    val bak = updateBuilding
                    updateBuilding = true
                    repeat(10000) { updateBuildLogic() }
                    updateBuilding = bak
                }
            }.wrapLabel(false).disabled { Vars.net.client() }
        }

        row().add(i("ui.simple.rules"))
        with(table().growX().get()) {
            defaults().pad(4f)
            button(Iconc.map.toString(), Styles.cleart) { mapInfoDialog.show() }.width(Vars.iconMed)
            button(i("ui.simple.cheat"), Styles.flatToggleMenut) { Vars.player.team().rules().cheat = !Vars.player.team().rules().cheat }
                .checked { Vars.player.team().rules().cheat }.tooltip(i("ui.simple.toggle-your-team-s-cheat")).wrapLabel(false)
            button(i("ui.simple.editor"), Styles.flatToggleMenut) { Vars.state.rules.editor = !Vars.state.rules.editor }
                .checked { Vars.state.rules.editor }.wrapLabel(false)
            button(i("ui.simple.sandbox"), Styles.flatToggleMenut) { Vars.state.rules.infiniteResources = !Vars.state.rules.infiniteResources }
                .checked { Vars.state.rules.infiniteResources }.wrapLabel(false)
            button(Iconc.edit.toString(), Styles.cleart) {
                rulesDialog.show(Vars.state.rules) { Vars.state.rules }
            }.width(Vars.iconMed)
        }

        row().add(i("ui.simple.hourglass"))
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
