package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustryX.features.*;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.*;

//moved from mindustry.arcModule.ui.OtherCoreItemDisplay
public class TeamsStatDisplay extends Table{
    private final float fontScl = 0.8f;
    private final Interval timer = new Interval();

    public final Seq<Teams.TeamData> teams = new Seq<>();
    public final Seq<Team> manualDeleted = new Seq<>();
    private boolean showStat = true, showItem = true, showUnit = true;


    public TeamsStatDisplay(){
        background(black6);
        update(() -> {
            if(timer.get(120f)) rebuild();
        });

        Events.on(EventType.ResetEvent.class, e -> {
            manualDeleted.clear();
            teams.clear();
            clearChildren();
        });
    }

    public Table wrapped(){
        Table table = new Table();

        table.button("编辑队伍", flatBordert, () -> UIExt.teamSelect.select(team -> teams.contains(team.data()), team -> {
            manualDeleted.remove(team);
            if(teams.contains(team.data())){
                teams.remove(team.data());
                manualDeleted.add(team);
            }else teams.add(team.data());
            rebuild();
        })).fillX().row();
        table.add(this).growX().touchable(Touchable.disabled).row();
        table.table(buttons -> {
            buttons.defaults().size(40);
            buttons.button(Blocks.worldProcessor.emoji(), flatTogglet, () -> {
                showStat = !showStat;
                rebuild();
            }).checked(a -> showStat);
            buttons.button(content.items().get(0).emoji(), flatTogglet, () -> {
                showItem = !showItem;
                rebuild();
            }).checked(a -> showItem);
            buttons.button(UnitTypes.mono.emoji(), flatTogglet, () -> {
                showUnit = !showUnit;
                rebuild();
            }).checked(a -> showUnit);
        }).row();

        return table;
    }

    private void rebuild(){
        Vars.state.teams.getActive().each(teams::addUnique);
        if(state.rules.waveTimer) teams.addUnique(state.rules.waveTeam.data());
        teams.removeAll(it -> manualDeleted.contains(it.team));
        teams.sort(teamData -> -teamData.cores.size);

        clearChildren();
        //name + cores + units
        addTeamData(Icon.players.getRegion(), team -> team.team.id < 6 ? team.team.localized() : String.valueOf(team.team.id));
        addTeamData(Blocks.coreNucleus.uiIcon, team -> UI.formatAmount(team.cores.size));
        addTeamData(UnitTypes.mono.uiIcon, team -> UI.formatAmount(team.units.size));
        addTeamData(UnitTypes.gamma.uiIcon, team -> String.valueOf(team.players.size));

        if(showStat){
            image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            addTeamDataCheckB(Blocks.siliconSmelter.uiIcon, team -> team.team.rules().cheat);
            addTeamDataCheck(Blocks.arc.uiIcon, team -> state.rules.blockDamage(team.team));
            addTeamDataCheck(Blocks.titaniumWall.uiIcon, team -> state.rules.blockHealth(team.team));
            addTeamDataCheck(Blocks.buildTower.uiIcon, team -> state.rules.buildSpeed(team.team));
            addTeamDataCheck(UnitTypes.corvus.uiIcon, team -> state.rules.unitDamage(team.team));
            addTeamDataCheck(UnitTypes.oct.uiIcon, team -> state.rules.unitHealth(team.team));
            addTeamDataCheck(UnitTypes.zenith.uiIcon, team -> state.rules.unitCrashDamage(team.team));
            addTeamDataCheck(Blocks.tetrativeReconstructor.uiIcon, team -> state.rules.unitBuildSpeed(team.team));
            addTeamDataCheck(Blocks.basicAssemblerModule.uiIcon, team -> state.rules.unitCost(team.team));
        }

        if(showItem){
            image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            for(Item item : content.items()){
                boolean show = false;
                for(Teams.TeamData team : teams){
                    if(team.hasCore() && team.core().items.get(item) > 0)
                        show = true;
                }
                if(show){
                    addTeamData(item.uiIcon, team -> (team.hasCore() && team.core().items.get(item) > 0) ? UI.formatAmount(team.core().items.get(item)) : "-");
                }
            }
        }

        if(showUnit){
            image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            for(UnitType unit : content.units()){
                boolean show = false;
                for(Teams.TeamData team : teams){
                    if(team.countType(unit) > 0)
                        show = true;
                }
                if(show){
                    addTeamData(unit.uiIcon, team -> team.countType(unit) > 0 ? String.valueOf(team.countType(unit)) : "-");
                }
            }
        }
    }

    private void addTeamDataCheck(TextureRegion icon, Floatf<TeamData> checked){
        if(teams.isEmpty() || teams.allMatch(it -> checked.get(it) == 1f)) return;
        //check allSame
        float value = checked.get(teams.get(0));
        if(teams.allMatch(it -> checked.get(it) == value)){
            addTeamData(icon, FormatDefault.format(value));
            return;
        }
        addTeamData(icon, team -> FormatDefault.format(checked.get(team)));
    }

    private void addTeamDataCheckB(TextureRegion icon, Boolf<TeamData> checked){
        if(teams.isEmpty() || teams.allMatch(it -> !checked.get(it))) return;
        //check allSame
        boolean value = checked.get(teams.get(0));
        if(teams.allMatch(it -> checked.get(it) == value)){
            addTeamData(icon, value ? "+" : "x");
            return;
        }
        addTeamData(icon, team -> checked.get(team) ? "+" : "×");
    }

    private void addTeamData(TextureRegion icon, String value){
        // 只显示一个数值
        image(icon).size(15, 15).left();
        label(() -> "[#" + Pal.accent + "]" + value).align(Align.center).fontScale(fontScl).colspan(getColumns() - 1);
        row();
    }

    private void addTeamData(TextureRegion icon, Func<TeamData, String> teamFunc){
        // 通用情况
        image(icon).size(15, 15).left();
        for(Teams.TeamData teamData : teams){
            label(() -> "[#" + teamData.team.color + "]" + teamFunc.get(teamData))
            .padLeft(2).expandX().uniformX().fontScale(fontScl);
        }
        row();
    }
}