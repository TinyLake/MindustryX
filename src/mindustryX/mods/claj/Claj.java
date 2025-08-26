package mindustryX.mods.claj;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustryX.mods.claj.dialogs.*;

public class Claj extends Plugin{
    public JoinViaClajDialog joinViaClaj;
    public ManageRoomsDialog manageRooms;

    @Override
    public void init(){
        if(Vars.headless) return;
        ClajIntegration.load();
        joinViaClaj = new JoinViaClajDialog();
        manageRooms = new ManageRoomsDialog();

        Table buttons = Vars.ui.join.buttons;
        buttons.button("通过claj代码加入游戏", Icon.play, joinViaClaj::show);

        var pausedDialog = Vars.ui.paused;
        pausedDialog.shown(() -> pausedDialog.cont.row()
        .collapser((t) -> t.button("管理claj房间", Icon.planet, () -> manageRooms.show()).growX().fillY(), () -> Vars.net.server())
        .name("ClajInfo").size(0, 60).colspan(pausedDialog.cont.getColumns()).fill().row());
    }
}