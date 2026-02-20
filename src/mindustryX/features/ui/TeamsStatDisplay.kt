package mindustryX.features.ui

import arc.Events
import arc.func.Boolf
import arc.func.Floatf
import arc.graphics.g2d.TextureRegion
import arc.scene.event.Touchable
import arc.scene.ui.Button
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.Align
import arc.util.Interval
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.core.UI
import mindustry.game.EventType
import mindustry.game.Teams
import mindustry.gen.Icon
import mindustry.graphics.Pal
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustryX.features.ui.FormatDefault.format

//moved from mindustry.arcModule.ui.OtherCoreItemDisplay
class TeamsStatDisplay : Table() {
    private val timer = Interval()
    private val teams = Seq<Teams.TeamData>()
    private var showStat = true
    private var showItem = true
    private var showUnit = true


    init {
        background(Styles.black6)
        update {
            if (timer.get(120f)) rebuild()
        }

        Events.on(EventType.ResetEvent::class.java) { _ ->
            teams.clear()
            clearChildren()
        }
    }

    fun wrapped(): Table {
        val table = Table()

        table.add(this).growX().touchable(Touchable.childrenOnly).row()
        table.table { buttons ->
            buttons.defaults().size(40f)
            buttons.button(Icon.addSmall, Styles.flati) {
                openAddTeamDialog {
                    if (teams.contains(it)) return@openAddTeamDialog
                    teams.add(it)
                    rebuild()
                }
            }
            buttons.button(Blocks.worldProcessor.emoji(), Styles.flatTogglet) {
                showStat = !showStat
                rebuild()
            }.checked { _ -> showStat }
            buttons.button(Vars.content.items().get(0).emoji(), Styles.flatTogglet) {
                showItem = !showItem
                rebuild()
            }.checked { _ -> showItem }
            buttons.button(UnitTypes.mono.emoji(), Styles.flatTogglet) {
                showUnit = !showUnit
                rebuild()
            }.checked { _ -> showUnit }
        }.row()

        return table
    }

    private fun openAddTeamDialog(onSelected: (Teams.TeamData) -> Unit) {
        BaseDialog(mindustryX.bundles.UiTextBundle.i("添加队伍")).apply {
            Vars.state.teams.active.forEach { team ->
                cont.add(Button().apply {
                    add(Table().apply {
                        image().color(team.team.color).size(Vars.iconMed)
                        add().width(16f)
                        add(team.team.coloredName())
                        add("#"+team.team.id).color(team.team.color)
                        add().growX()
                        image(Blocks.coreFoundation.uiIcon).size(Vars.iconSmall).padRight(4f)
                        label { team.cores.size.toString() }
                    }).growX().row()
                    image().growX().row()
                    team.players.forEach {
                        add(Table().apply {
                            image(it.unit()?.type?.uiIcon).size(Vars.iconMed)
                            add().width(16f)
                            label { it.plainName() }.left().expandX()
                        }).fillX().row()
                    }
                    clicked { onSelected(team) }
                    setDisabled { team in teams }
                }).growX().maxWidth(800f).row()
            }
            addCloseButton()
        }.show()
    }

    private fun rebuild() {
        if (teams.isEmpty) {
            Vars.state.teams.getActive().forEach { teams.addUnique(it) }
            if (Vars.state.rules.waveTimer) teams.addUnique(Vars.state.rules.waveTeam.data())
        }
        teams.sort { it.cores.size.toFloat() }

        clearChildren()
        //name + cores + units
        i(Icon.players.region); stat { if (it.team.id < 6) it.team.localized() else it.team.id.toString() }
        i(Blocks.coreNucleus.uiIcon); stat { UI.formatAmount(it.cores.size.toLong()) }
        i(UnitTypes.mono.uiIcon); stat { UI.formatAmount(it.units.size.toLong()) }
        i(UnitTypes.gamma.uiIcon); stat { it.players.size.toString() }
        i(Icon.eyeSmall.region); teams.forEach {
            button(Icon.eyeOffSmall, Styles.clearNonei) {
                teams.remove(it)
                rebuild()
            }
        }; row()

        if (showStat) {
            image().color(Pal.accent).fillX().height(1f).colspan(999).padTop(3f).padBottom(3f).row()
            addTeamDataB(Blocks.siliconSmelter.uiIcon) { it.team.rules().cheat }
            addTeamDataF(Blocks.arc.uiIcon) { Vars.state.rules.blockDamage(it.team) }
            addTeamDataF(Blocks.titaniumWall.uiIcon) { Vars.state.rules.blockHealth(it.team) }
            addTeamDataF(Blocks.buildTower.uiIcon) { Vars.state.rules.buildSpeed(it.team) }
            addTeamDataF(UnitTypes.corvus.uiIcon) { Vars.state.rules.unitDamage(it.team) }
            addTeamDataF(UnitTypes.oct.uiIcon) { Vars.state.rules.unitHealth(it.team) }
            addTeamDataF(UnitTypes.zenith.uiIcon) { Vars.state.rules.unitCrashDamage(it.team) }
            addTeamDataF(Blocks.tetrativeReconstructor.uiIcon) { Vars.state.rules.unitBuildSpeed(it.team) }
            addTeamDataF(Blocks.basicAssemblerModule.uiIcon) { Vars.state.rules.unitCost(it.team) }
        }

        if (showItem) {
            image().color(Pal.accent).fillX().height(1f).colspan(999).padTop(3f).padBottom(3f).row()
            for (item in Vars.content.items()) {
                if (teams.all { t -> t.core().let { it == null || it.items[item] == 0 } }) continue
                i(item.uiIcon); stat {
                    if (it.hasCore() && it.core().items.get(item) > 0) UI.formatAmount(it.core().items.get(item).toLong()) else "-"
                }
            }
        }

        if (showUnit) {
            image().color(Pal.accent).fillX().height(1f).colspan(999).padTop(3f).padBottom(3f).row()
            for (unit in Vars.content.units()) {
                if (teams.all { it.countType(unit) == 0 }) continue
                i(unit.uiIcon); stat { if (it.countType(unit) > 0) it.countType(unit).toString() else "-" }
            }
        }
    }

    private fun addTeamDataF(icon: TextureRegion, f: Floatf<Teams.TeamData>) {
        if (teams.isEmpty || teams.allMatch { f.get(it) == 1f }) return
        i(icon)
        //check allSame
        val value = f.get(teams.get(0))
        if (teams.allMatch { f.get(it) == value }) {
            stat(format(value))
            return
        }
        stat { format(f.get(it)) }
    }

    private fun addTeamDataB(icon: TextureRegion, checked: Boolf<Teams.TeamData>) {
        if (teams.isEmpty || teams.allMatch { !checked.get(it) }) return
        i(icon)
        //check allSame
        val value = checked.get(teams.get(0))
        if (teams.allMatch { checked.get(it) == value }) {
            stat(if (value) "+" else "x")
            return
        }
        stat { if (checked.get(it)) "+" else "×" }
    }

    fun i(icon: TextureRegion) {
        image(icon).size(15f, 15f).left()
    }

    private fun stat(value: String) {
        add(value).color(Pal.accent).align(Align.center).fontScale(fontScl).colspan(columns - 1)
        row()
    }

    private inline fun stat(crossinline f: (Teams.TeamData) -> CharSequence) {
        for (teamData in teams) {
            label { f(teamData) }
                .color(teamData.team.color)
                .padLeft(2f).expandX().uniformX().fontScale(fontScl)
        }
        row()
    }

    companion object {
        private const val fontScl = 0.8f
    }
}
