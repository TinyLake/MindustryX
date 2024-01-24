package mindustry.arcModule.ui.auxilliary;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ui.*;

import static mindustry.arcModule.ui.RStyles.*;

public class AuxilliaryTable extends Table{
    private boolean shown = true;
    private final Seq<BaseToolsTable> toolsTables = Seq.with(
    new MapInfoTable(),
    new WaveInfoTable(),
    new AITools(),
    new ScriptButtons(),
    new MobileScriptButtons(),
    new MarkTable()
    );

    public AuxilliaryTable(){
        setup();

        rebuild();
    }

    public void setup(){
        for(BaseToolsTable table : toolsTables){
            table.setup();
        }
    }

    public void toggle(){
        shown = !shown;
        rebuild();
    }

    private void rebuild(){
        clearChildren();

        table(Styles.black3, buttons -> {
            buttons.button("[acid]辅助器", clearLineNoneTogglet, this::toggle).size(80f, 40f).tooltip((shown ? "关闭" : "开启") + "辅助器");

            if(shown){
                for(BaseToolsTable table : toolsTables){
                    table.addButton(buttons);
                }
            }
        }).fillX();

        row();

        if(shown){
            table(black1, body -> {
                body.defaults().expandX().left();
                for(BaseToolsTable table : toolsTables){
                    table.margin(4);
                    body.collapser(table, table::shown).row();
                }
            }).fillX().left();
        }
    }

}
