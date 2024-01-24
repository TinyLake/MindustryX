package mindustry.arcModule.ui.quickTool;

import arc.scene.ui.layout.*;

public class QuickToolTable extends Table{
    public HudSettingsTable hudSettingsTable = new HudSettingsTable();
    public AdvanceBuildTool advanceBuildTool = new AdvanceBuildTool();

    public QuickToolTable(){
        add(hudSettingsTable).growX().row();
        add(advanceBuildTool).growX();
    }
}
