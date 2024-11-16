package mindustryX.features.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.game.EventType.*;
import mindustry.ui.*;
import mindustryX.features.ui.auxiliary.*;

public class AuxiliaryTools extends Table{
    static{
        RStyles.load();
    }

    private boolean shown = true;
    private final Seq<Table> toolsTables = Seq.with(
    new AITools(),
    new ScriptButtons(),
    new MobileScriptButtons()
    );

    public AuxiliaryTools(){
        name = "AuxiliaryTable";
        Events.on(WorldLoadEvent.class, e -> rebuild());
        Events.on(ResetEvent.class, e -> clearChildren());
        rebuild();
    }

    public void toggle(){
        shown = !shown;
        rebuild();
    }

    private void rebuild(){
        clearChildren();

        table(Styles.black5, buttons -> {
            buttons.button("[acid]辅助器", RStyles.clearLineNoneTogglet, this::toggle).size(80f, 40f).tooltip((shown ? "关闭" : "开启") + "辅助器");

            if(!shown) return;
            for(Table table : toolsTables){
                buttons.button(table.icon, RStyles.clearAccentNoneTogglei, 30, () -> table.shown ^= true)
                .size(40).checked(b -> table.shown);
            }
        }).fillX().row();
        if(!shown) return;
        table(Styles.black3, body -> {
            body.defaults().expandX().left();
            for(Table table : toolsTables){
                table.margin(4);
                body.collapser(table, () -> table.shown).growX().row();
            }
        }).fillX().left();
    }

    public abstract static class Table extends arc.scene.ui.layout.Table{
        public boolean shown;
        protected Drawable icon;

        public Table(TextureRegion region){
            this(new TextureRegionDrawable(region));
        }

        public Table(Drawable icon){
            this.icon = icon;
        }
    }
}
