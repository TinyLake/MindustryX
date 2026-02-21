package mindustryX.features.ui;

import arc.func.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.VarsX;

import static mindustryX.features.UIExt.i;

//move from mindustry.arcModule.ui.dialogs.TeamSelectDialog
public class TeamSelectDialog extends BaseDialog{
    private Boolf<Team> checked;
    private Team lastTeam;
    private Cons<Team> cons;

    public TeamSelectDialog(){
        super(i("队伍选择器"));
        cont.pane(td -> {
            for(Team team : Team.all){
                if(team.id % 10 == 6){
                    td.row();
                    td.add(VarsX.bundle.teamRange(team.id));
                }
                td.button(Tex.whiteui, Styles.clearTogglei, 36f, () -> {
                    lastTeam = team;
                    cons.get(team);
                })
                .checked(b -> checked.get(team)).pad(3f).size(50f).with(b -> b.getStyle().imageUpColor = team.color);
            }
        });
        closeOnBack();
        addCloseButton();
    }

    public void pickOne(Cons<Team> cons, Team selectedTeam){
        if(selectedTeam != null) lastTeam = selectedTeam;
        pickOne(cons);
    }

    public void pickOne(Cons<Team> cons){
        select((t) -> t == lastTeam, (t) -> {
            hide();
            cons.get(t);
        });
    }

    public void select(Boolf<Team> checked, Cons<Team> cons){
        this.checked = checked;
        this.cons = cons;
        show();
    }
}
