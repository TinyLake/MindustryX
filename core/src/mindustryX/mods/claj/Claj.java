package mindustryX.mods.claj;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustryX.mods.claj.dialogs.JoinViaClajDialog;
import mindustryX.mods.claj.dialogs.ManageRoomsDialog;

public class Claj extends Plugin{
    public JoinViaClajDialog joinViaClaj;
    public ManageRoomsDialog manageRooms;

    @Override
    public void init(){
        ClajIntegration.load();
        joinViaClaj = new JoinViaClajDialog();
        manageRooms = new ManageRoomsDialog();

        Stack stack = (Stack)Vars.ui.join.getChildren().get(1);
        Table root = (Table)stack.getChildren().get(1);

        root.button("通过claj代码加入游戏", Icon.play, joinViaClaj::show);
        if(!Vars.steam && !Vars.mobile) root.getCells().insert(4, root.getCells().remove(6));
        else root.getCells().insert(3, root.getCells().remove(4));

        var pausedDialog = Vars.ui.paused;
        pausedDialog.shown(() -> {
            if(Vars.mobile){
                pausedDialog.cont.buttonRow("管理claj房间", Icon.planet, () -> manageRooms.show()).colspan(2).width(450f).disabled(b -> !Vars.net.server());
            }else{
                pausedDialog.cont.row().button("管理claj房间", Icon.planet, () -> manageRooms.show()).colspan(2).width(450f).disabled(b -> !Vars.net.server());
            }
        });
    }
}