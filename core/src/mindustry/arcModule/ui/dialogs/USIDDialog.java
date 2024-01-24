package mindustry.arcModule.ui.dialogs;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.util.*;

public class USIDDialog extends BaseDialog{
    public static boolean chooseUSID = false;

    public USIDDialog(){
        super("usid管理器");
        shown(this::build);
        addCloseButton();
    }

    public void build(){
        cont.clear();
        cont.add("在这里可以更改你在各个服务器内的usid").row();
        cont.add("但小心，误改可能会导致账号需重新绑定/丢失管理").row();
        cont.check("在加入新服务器时提示指定usid(不清楚用途不要开启)", b -> {
            chooseUSID = b;
            Core.settings.put("arc-chooseUSID", b);
        }).checked(Core.settings.getBool("arc-chooseUSID", false)).row();
        cont.pane(pane -> pane.table(t -> {
            t.table(t2 -> {
                t2.table(t3 -> t3.add("ip").color(Pal.accent).width(100).padLeft(5).left().get().setAlignment(Align.left)).growX();
                t2.table(t3 -> t3.add("usid").color(Pal.accent).width(100).left().get().setAlignment(Align.left)).growX();
                t2.add().size(48);
            }).growX();
            t.row();
            HashMap<String, Object> map = Reflect.get(Core.settings, "values");
            for(String k : map.keySet()){
                if(k.startsWith("usid-")){
                    t.table(t2 -> {
                        t2.table(t3 -> t3.add(k.substring(5)).color(Pal.accent).growX().padLeft(5).get().setAlignment(Align.left)).growX();
                        t2.table(t3 -> t3.field(Core.settings.getString(k), v -> Core.settings.put(k, v))).growX();
                        t2.button(Icon.trash, () -> Vars.ui.showConfirm("警告:此操作不可逆且可能会导致账号需重新绑定/丢失管理", () -> {
                            Core.settings.remove(k);
                            build();
                        })).size(48);
                    }).growX();
                    t.row();
                }
            }
            t.button("清除所有usid", Icon.trash, () -> Vars.ui.showConfirm("警告:此操作不可逆且可能会导致账号需重新绑定/丢失管理", () -> {
                Seq<String> keys = new Seq<>();
                for(String k : map.keySet()){
                    if(k.startsWith("usid-")){
                        keys.add(k);
                    }
                }
                for(String k : keys){
                    Core.settings.remove(k);
                }
                build();
            })).growX();
        }).get().setBackground(Styles.grayPanel)).grow();
    }

    public static void showSet(String ip){
        Vars.ui.showTextInput("设置usid", "选择对于ip " + ip + " 的usid\n填入\"rand\"可自动生成", 32, "", false, s -> {
            if(Objects.equals(s, "rand")){
                byte[] bytes = new byte[8];
                new Rand().nextBytes(bytes);
                String result = new String(Base64Coder.encode(bytes));
                Core.settings.put("usid-" + ip, result);
            }else{
                Core.settings.put("usid-" + ip, s);
            }
            Vars.ui.join.reconnect();
        }, () -> Vars.ui.loadfrag.hide());
    }
}
